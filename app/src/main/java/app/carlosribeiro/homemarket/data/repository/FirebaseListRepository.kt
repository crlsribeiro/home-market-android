package app.carlosribeiro.homemarket.data.repository

import app.carlosribeiro.homemarket.data.local.ItemDao
import app.carlosribeiro.homemarket.data.local.ListDao
import app.carlosribeiro.homemarket.data.mapper.ItemFields
import app.carlosribeiro.homemarket.data.mapper.ListFields
import app.carlosribeiro.homemarket.data.mapper.ListMapper
import app.carlosribeiro.homemarket.domain.model.ItemError
import app.carlosribeiro.homemarket.domain.model.ItemResult
import app.carlosribeiro.homemarket.domain.model.ListError
import app.carlosribeiro.homemarket.domain.model.ListItem
import app.carlosribeiro.homemarket.domain.model.ListResult
import app.carlosribeiro.homemarket.domain.model.NewItem
import app.carlosribeiro.homemarket.domain.model.Week
import app.carlosribeiro.homemarket.domain.model.WeekList
import app.carlosribeiro.homemarket.domain.repository.ListRepository
import app.carlosribeiro.homemarket.domain.week.WeekCalendar
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.storage.FirebaseStorage
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Offline-first, like [FirebaseHouseholdRepository]: the same queries as the web and iOS apps feed
 * Room, and callers read Room.
 */
@Singleton
class FirebaseListRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val listDao: ListDao,
    private val itemDao: ItemDao
) : ListRepository {

    private val lists get() = firestore.collection(LISTS)
    private val items get() = firestore.collection(ITEMS)

    override fun observeCurrentList(householdId: String): Flow<WeekList?> = channelFlow {
        val registration = lists.whereEqualTo(ListFields.HOUSEHOLD_ID, householdId)
            .addSnapshotListener { snapshot, _ ->
                snapshot ?: return@addSnapshotListener
                val entities = snapshot.documentsData().map { (id, data) -> ListMapper.documentToListEntity(id, data) }
                launch { listDao.replaceLists(householdId, entities) }
            }
        launch {
            listDao.observeCurrentList(householdId).collect { send(it?.let(ListMapper::listEntityToDomain)) }
        }
        awaitClose { registration.remove() }
    }.distinctUntilChanged()

    override fun observeItems(listId: String): Flow<List<ListItem>> = channelFlow {
        val registration = items.whereEqualTo(ItemFields.LIST_ID, listId)
            .addSnapshotListener { snapshot, _ ->
                snapshot ?: return@addSnapshotListener
                val entities = snapshot.documentsData().map { (id, data) -> ListMapper.documentToItemEntity(id, data) }
                launch { itemDao.replaceItemsOfList(listId, entities) }
            }
        launch {
            itemDao.observeItems(listId).collect { send(it.map(ListMapper::itemEntityToDomain)) }
        }
        awaitClose { registration.remove() }
    }.distinctUntilChanged()

    override fun observeNextWeekItems(householdId: String): Flow<List<ListItem>> = channelFlow {
        val registration = items.whereEqualTo(ItemFields.HOUSEHOLD_ID, householdId)
            .whereEqualTo(ItemFields.STATUS, ItemFields.STATUS_ROLLED_OVER)
            .addSnapshotListener { snapshot, _ ->
                snapshot ?: return@addSnapshotListener
                val entities = snapshot.documentsData().map { (id, data) -> ListMapper.documentToItemEntity(id, data) }
                launch { itemDao.replaceNextWeekItems(householdId, entities) }
            }
        launch {
            itemDao.observeNextWeekItems(householdId).collect { send(it.map(ListMapper::itemEntityToDomain)) }
        }
        awaitClose { registration.remove() }
    }.distinctUntilChanged()

    /** iOS `ListService.createList`: create-if-missing transaction on the deterministic id, then read back. */
    override suspend fun createWeekList(householdId: String, week: Week): ListResult = runList {
        val document = lists.document(WeekCalendar.listId(householdId, week))
        firestore.runTransaction { transaction ->
            if (!transaction.get(document).exists()) {
                transaction.set(
                    document,
                    mapOf(
                        ListFields.HOUSEHOLD_ID to householdId,
                        ListFields.WEEK_LABEL to week.label,
                        ListFields.WEEK_START to Timestamp(Date.from(week.start)),
                        ListFields.WEEK_END to Timestamp(Date.from(week.end)),
                        ListFields.STATUS to ListFields.STATUS_OPEN,
                        ListFields.CREATED_AT to FieldValue.serverTimestamp(),
                        ListFields.CLOSED_AT to null
                    )
                )
            }
        }.await()
        val data = document.get().await().estimatedData()
        ListResult.Success(ListMapper.listEntityToDomain(ListMapper.documentToListEntity(document.id, data)))
    }

    /** iOS `ListService.addItem`: create the document, then upload the photo and store its URL. */
    override suspend fun addItem(item: NewItem, photo: ByteArray?): ItemResult {
        val document = items.document()
        val saveError = runWrite { document.set(item.toDocument()).await() }
        return when {
            saveError != null -> ItemResult.Failure(saveError.toItemError())
            photo == null -> ItemResult.Success
            else -> uploadPhoto(item.householdId, document, photo)
        }
    }

    /** Same Storage path as the other clients; a replaced photo overwrites the file. */
    private suspend fun uploadPhoto(householdId: String, document: DocumentReference, photo: ByteArray): ItemResult {
        val uploadError = runWrite {
            val file = storage.reference.child("households/$householdId/items/${document.id}/photo")
            file.putBytes(photo).await()
            val url = file.downloadUrl.await()
            document.update(ItemFields.PHOTO_URL, url.toString()).await()
        }
        return if (uploadError == null) ItemResult.Success else ItemResult.Failure(ItemError.PHOTO_UPLOAD)
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun runList(block: suspend () -> ListResult): ListResult = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        ListResult.Failure(e.toListError())
    }

    /** Runs a write and returns its error, or null when it succeeded. */
    @Suppress("TooGenericExceptionCaught")
    private suspend fun runWrite(block: suspend () -> Unit): ListError? = try {
        block()
        null
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        e.toListError()
    }

    private fun ListError.toItemError(): ItemError =
        if (this == ListError.NETWORK) ItemError.NETWORK else ItemError.UNKNOWN

    private fun Exception.toListError(): ListError = when {
        this is FirebaseNetworkException -> ListError.NETWORK

        this is FirebaseFirestoreException && code == FirebaseFirestoreException.Code.UNAVAILABLE ->
            ListError.NETWORK

        else -> ListError.UNKNOWN
    }

    private companion object {
        const val LISTS = "lists"
        const val ITEMS = "items"
    }
}

private fun NewItem.toDocument(): Map<String, Any?> = mapOf(
    ItemFields.LIST_ID to listId,
    ItemFields.HOUSEHOLD_ID to householdId,
    ItemFields.NAME to name,
    ItemFields.QUANTITY to quantity,
    ItemFields.NOTES to notes,
    ItemFields.URGENT to urgent,
    ItemFields.ADDED_BY_UID to addedByUid,
    ItemFields.ADDED_BY_NAME to addedByName,
    ItemFields.STATUS to ItemFields.STATUS_PENDING,
    ItemFields.APPROVAL_STATUS to ListMapper.approvalStatusValue(approvalStatus),
    ItemFields.NOT_FOUND_RESOLVED to false,
    ItemFields.PHOTO_URL to null,
    ItemFields.CREATED_AT to FieldValue.serverTimestamp()
)

/** Pending server timestamps read as the local estimate, so a new list sorts as the newest at once. */
private fun DocumentSnapshot.estimatedData(): Map<String, Any?> =
    getData(DocumentSnapshot.ServerTimestampBehavior.ESTIMATE).orEmpty()

private fun QuerySnapshot.documentsData(): List<Pair<String, Map<String, Any?>>> =
    documents.map { it.id to it.estimatedData() }
