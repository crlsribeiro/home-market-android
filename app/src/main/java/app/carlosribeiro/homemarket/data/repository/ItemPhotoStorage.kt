package app.carlosribeiro.homemarket.data.repository

import com.google.firebase.storage.FirebaseStorage
import javax.inject.Inject
import kotlinx.coroutines.tasks.await

/** Item photos at `households/{householdId}/items/{itemId}/photo`, the path the other clients use. */
class ItemPhotoStorage @Inject constructor(private val storage: FirebaseStorage) {
    /** Uploads (or overwrites) the photo and returns its download URL. */
    suspend fun upload(householdId: String, itemId: String, photo: ByteArray): String {
        val file = storage.reference.child("households/$householdId/items/$itemId/photo")
        file.putBytes(photo).await()
        return file.downloadUrl.await().toString()
    }
}
