package app.carlosribeiro.homemarket.data.repository

import app.carlosribeiro.homemarket.data.local.PurchaseDao
import app.carlosribeiro.homemarket.data.mapper.PurchaseFields
import app.carlosribeiro.homemarket.data.mapper.PurchaseItemFields
import app.carlosribeiro.homemarket.data.mapper.PurchaseMapper
import app.carlosribeiro.homemarket.domain.model.AdminError
import app.carlosribeiro.homemarket.domain.model.AdminResult
import app.carlosribeiro.homemarket.domain.model.Purchase
import app.carlosribeiro.homemarket.domain.model.PurchaseItem
import app.carlosribeiro.homemarket.domain.receipt.ReceiptLine
import app.carlosribeiro.homemarket.domain.repository.PurchaseRepository
import app.carlosribeiro.homemarket.domain.util.Money
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/** Same queries and writes as iOS `HistoryService`, offline-first like [FirebaseListRepository]. */
@Singleton
class FirebasePurchaseRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val purchaseDao: PurchaseDao
) : PurchaseRepository {

    private val purchases get() = firestore.collection(PURCHASES)
    private val purchaseItems get() = firestore.collection(PURCHASE_ITEMS)

    override fun observePurchases(householdId: String): Flow<List<Purchase>> = channelFlow {
        val registration = purchases.whereEqualTo(PurchaseFields.HOUSEHOLD_ID, householdId)
            .addSnapshotListener { snapshot, _ ->
                snapshot ?: return@addSnapshotListener
                val entities = snapshot.documents.map {
                    PurchaseMapper.documentToPurchaseEntity(it.id, it.data.orEmpty())
                }
                launch { purchaseDao.replacePurchases(householdId, entities) }
            }
        launch {
            purchaseDao.observePurchases(householdId).collect { send(it.map(PurchaseMapper::toDomain)) }
        }
        awaitClose { registration.remove() }
    }.distinctUntilChanged()

    override fun observePurchase(purchaseId: String): Flow<Purchase?> = channelFlow {
        val registration = purchases.document(purchaseId).addSnapshotListener { snapshot, _ ->
            snapshot ?: return@addSnapshotListener
            launch {
                if (snapshot.exists()) {
                    val entity = PurchaseMapper.documentToPurchaseEntity(snapshot.id, snapshot.data.orEmpty())
                    purchaseDao.upsertPurchases(listOf(entity))
                } else {
                    purchaseDao.deletePurchase(purchaseId)
                }
            }
        }
        launch {
            purchaseDao.observePurchase(purchaseId).collect { send(it?.let(PurchaseMapper::toDomain)) }
        }
        awaitClose { registration.remove() }
    }.distinctUntilChanged()

    override fun observeItems(purchaseId: String): Flow<List<PurchaseItem>> = channelFlow {
        val registration = purchaseItems.whereEqualTo(PurchaseItemFields.PURCHASE_ID, purchaseId)
            .addSnapshotListener { snapshot, _ ->
                snapshot ?: return@addSnapshotListener
                val entities = snapshot.documents.map { PurchaseMapper.documentToItemEntity(it.id, it.data.orEmpty()) }
                launch { purchaseDao.replaceItems(purchaseId, entities) }
            }
        launch {
            purchaseDao.observeItems(purchaseId).collect { send(it.map(PurchaseMapper::itemToDomain)) }
        }
        awaitClose { registration.remove() }
    }.distinctUntilChanged()

    override suspend fun createPurchaseForList(listId: String, householdId: String, weekLabel: String): AdminResult =
        write {
            val existing = purchases.whereEqualTo(PurchaseFields.LIST_ID, listId).limit(1).get().await()
            if (existing.isEmpty) {
                purchases.add(
                    mapOf(
                        PurchaseFields.LIST_ID to listId,
                        PurchaseFields.HOUSEHOLD_ID to householdId,
                        PurchaseFields.WEEK_LABEL to weekLabel,
                        PurchaseFields.TOTAL to 0,
                        PurchaseFields.RECEIPT_URL to null,
                        PurchaseFields.RECEIPT_PROCESSED to false,
                        PurchaseFields.CREATED_AT to Timestamp.now()
                    )
                ).await()
            }
        }

    override suspend fun updateItem(purchaseId: String, itemId: String, name: String?, unitPrice: Double): AdminResult =
        write {
            val item = purchaseItems.document(itemId)
            val quantity = (item.get().await().get(PurchaseItemFields.QUANTITY) as? Number)?.toInt() ?: 1
            val fields = mutableMapOf<String, Any>(
                PurchaseItemFields.UNIT_PRICE to unitPrice,
                PurchaseItemFields.TOTAL_PRICE to Money.round2(unitPrice * quantity)
            )
            name?.let { fields[PurchaseItemFields.NAME] = it }
            item.update(fields).await()
            val total = purchaseItems.whereEqualTo(PurchaseItemFields.PURCHASE_ID, purchaseId).get().await()
                .documents
                .sumOf { (it.get(PurchaseItemFields.TOTAL_PRICE) as? Number)?.toDouble() ?: 0.0 }
            purchases.document(purchaseId).update(PurchaseFields.TOTAL, Money.round2(total)).await()
        }

    /** One batch replaces the lines and updates the purchase, so the total always matches the lines. */
    override suspend fun saveReceipt(
        purchaseId: String,
        photo: ByteArray,
        storeName: String?,
        lines: List<ReceiptLine>
    ): AdminResult = write {
        val file = storage.reference.child("receipts/$purchaseId/${UUID.randomUUID()}.jpg")
        file.putBytes(photo).await()
        val url = file.downloadUrl.await().toString()
        val oldLines = purchaseItems.whereEqualTo(PurchaseItemFields.PURCHASE_ID, purchaseId).get().await()
        val batch = firestore.batch()
        oldLines.documents.forEach { batch.delete(it.reference) }
        lines.forEach { line ->
            batch.set(
                purchaseItems.document(),
                mapOf(
                    PurchaseItemFields.PURCHASE_ID to purchaseId,
                    PurchaseItemFields.NAME to line.name,
                    PurchaseItemFields.QUANTITY to 1,
                    PurchaseItemFields.UNIT_PRICE to line.price,
                    PurchaseItemFields.TOTAL_PRICE to line.price
                )
            )
        }
        batch.update(
            purchases.document(purchaseId),
            mapOf(
                PurchaseFields.RECEIPT_URL to url,
                PurchaseFields.RECEIPT_PROCESSED to true,
                PurchaseFields.TOTAL to Money.round2(lines.sumOf { it.price }),
                PurchaseFields.STORE_NAME to storeName
            )
        )
        batch.commit().await()
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun write(block: suspend () -> Unit): AdminResult = try {
        block()
        AdminResult.Success
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        AdminResult.Failure(if (e.isNetworkError()) AdminError.NETWORK else AdminError.UNKNOWN)
    }

    private fun Exception.isNetworkError(): Boolean = this is FirebaseNetworkException ||
        (this is FirebaseFirestoreException && code == FirebaseFirestoreException.Code.UNAVAILABLE)

    private companion object {
        const val PURCHASES = "purchases"
        const val PURCHASE_ITEMS = "purchaseItems"
    }
}
