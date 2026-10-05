package app.carlosribeiro.homemarket.data.repository

import app.carlosribeiro.homemarket.data.local.ItemDao
import app.carlosribeiro.homemarket.data.local.ListDao
import app.carlosribeiro.homemarket.data.mapper.ItemFields
import app.carlosribeiro.homemarket.data.mapper.ListMapper
import app.carlosribeiro.homemarket.domain.model.ItemError
import app.carlosribeiro.homemarket.domain.model.ItemResult
import app.carlosribeiro.homemarket.domain.model.ListItem
import app.carlosribeiro.homemarket.domain.model.WeekList
import app.carlosribeiro.homemarket.domain.repository.ItemRepository
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/** Item detail reads and writes (iOS `ItemDetailView` and `ListService`). Reads come from Room. */
@Singleton
class FirebaseItemRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val itemDao: ItemDao,
    private val listDao: ListDao,
    private val photoStorage: ItemPhotoStorage
) : ItemRepository {

    private fun itemDocument(itemId: String) = firestore.collection(ITEMS).document(itemId)

    /** Listens to the item itself, so the screen stays live after the list screen stops listening. */
    override fun observeItem(itemId: String): Flow<ListItem?> = channelFlow {
        val registration = itemDocument(itemId).addSnapshotListener { snapshot, _ ->
            snapshot ?: return@addSnapshotListener
            val data = snapshot.getData(DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            launch {
                if (data == null) {
                    itemDao.deleteItem(itemId)
                } else {
                    itemDao.upsertItems(listOf(ListMapper.documentToItemEntity(itemId, data)))
                }
            }
        }
        launch {
            itemDao.observeItem(itemId).collect { send(it?.let(ListMapper::itemEntityToDomain)) }
        }
        awaitClose { registration.remove() }
    }.distinctUntilChanged()

    override fun observeList(listId: String): Flow<WeekList?> =
        listDao.observeList(listId).map { it?.let(ListMapper::listEntityToDomain) }.distinctUntilChanged()

    override suspend fun updateNotes(itemId: String, notes: String): ItemResult = write {
        itemDocument(itemId).update(ItemFields.NOTES, notes).await()
    }

    override suspend fun removeItem(itemId: String): ItemResult = write {
        itemDocument(itemId).delete().await()
    }

    override suspend fun replacePhoto(item: ListItem, photo: ByteArray): ItemResult {
        val result = write {
            val url = photoStorage.upload(item.householdId, item.id, photo)
            itemDocument(item.id).update(ItemFields.PHOTO_URL, url).await()
        }
        return if (result is ItemResult.Failure && result.error != ItemError.NETWORK) {
            ItemResult.Failure(ItemError.PHOTO_UPLOAD)
        } else {
            result
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun write(block: suspend () -> Unit): ItemResult = try {
        block()
        ItemResult.Success
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        ItemResult.Failure(if (e.isNetworkError()) ItemError.NETWORK else ItemError.UNKNOWN)
    }

    private fun Exception.isNetworkError(): Boolean = this is FirebaseNetworkException ||
        (this is FirebaseFirestoreException && code == FirebaseFirestoreException.Code.UNAVAILABLE)

    private companion object {
        const val ITEMS = "items"
    }
}
