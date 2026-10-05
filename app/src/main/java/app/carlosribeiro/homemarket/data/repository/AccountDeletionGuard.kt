package app.carlosribeiro.homemarket.data.repository

import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Deleting an account removes `users/{uid}` before the Auth user. The user document listener in
 * [FirebaseAuthRepository] would see the missing document and recreate it as a first sign-in, leaving an
 * orphan document. This guard tells the listener which account is being deleted.
 */
@Singleton
class AccountDeletionGuard @Inject constructor() {
    private val deletingUid = AtomicReference<String?>(null)

    fun isDeleting(uid: String): Boolean = deletingUid.get() == uid

    /** Runs [block] with [uid] marked as being deleted; the mark is cleared only if [block] fails. */
    suspend fun <T> deleting(uid: String, block: suspend () -> T): T {
        deletingUid.set(uid)
        return runCatching { block() }.onFailure { deletingUid.compareAndSet(uid, null) }.getOrThrow()
    }
}
