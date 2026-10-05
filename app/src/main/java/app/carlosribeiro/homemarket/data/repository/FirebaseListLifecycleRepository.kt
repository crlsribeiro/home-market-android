package app.carlosribeiro.homemarket.data.repository

import app.carlosribeiro.homemarket.data.mapper.ItemFields
import app.carlosribeiro.homemarket.data.mapper.ListFields
import app.carlosribeiro.homemarket.domain.model.AdminError
import app.carlosribeiro.homemarket.domain.model.AdminResult
import app.carlosribeiro.homemarket.domain.model.ListStatus
import app.carlosribeiro.homemarket.domain.repository.ListLifecycleRepository
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

/** Same writes as the web `useList` hook and iOS `ListService`. The listeners bring the result into Room. */
@Singleton
class FirebaseListLifecycleRepository @Inject constructor(private val firestore: FirebaseFirestore) :
    ListLifecycleRepository {

    override suspend fun updateStatus(listId: String, status: ListStatus): AdminResult = write {
        val fields = mutableMapOf<String, Any>(ListFields.STATUS to status.value())
        if (status == ListStatus.CLOSED) fields[ListFields.CLOSED_AT] = FieldValue.serverTimestamp()
        firestore.collection(LISTS).document(listId).update(fields).await()
    }

    override suspend fun approveItem(itemId: String): AdminResult = write {
        firestore.collection(ITEMS).document(itemId)
            .update(ItemFields.APPROVAL_STATUS, ItemFields.APPROVAL_APPROVED)
            .await()
    }

    override suspend fun rejectItem(itemId: String): AdminResult = write {
        firestore.collection(ITEMS).document(itemId)
            .update(
                mapOf(
                    ItemFields.APPROVAL_STATUS to ItemFields.APPROVAL_REJECTED,
                    ItemFields.STATUS to ItemFields.STATUS_ROLLED_OVER
                )
            )
            .await()
    }

    private fun ListStatus.value(): String = when (this) {
        ListStatus.OPEN -> ListFields.STATUS_OPEN
        ListStatus.LOCKED -> ListFields.STATUS_LOCKED
        ListStatus.SHOPPING -> ListFields.STATUS_SHOPPING
        ListStatus.CLOSED -> ListFields.STATUS_CLOSED
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
        const val LISTS = "lists"
        const val ITEMS = "items"
    }
}
