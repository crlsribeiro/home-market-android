package app.carlosribeiro.homemarket.data.repository

import app.carlosribeiro.homemarket.data.mapper.UserFields
import app.carlosribeiro.homemarket.domain.model.ProfileError
import app.carlosribeiro.homemarket.domain.model.ProfileResult
import app.carlosribeiro.homemarket.domain.repository.ProfileRepository
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

/** Same writes as iOS `AuthService.updateProfile`, `uploadAvatar` and `updateEmail`. */
@Singleton
class FirebaseProfileRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val deletionGuard: AccountDeletionGuard
) : ProfileRepository {

    override suspend fun updatePhone(phone: String, phoneCountryCode: String): ProfileResult = withUser { user ->
        firestore.collection(USERS).document(user.uid)
            .update(mapOf(UserFields.PHONE to phone, UserFields.PHONE_COUNTRY_CODE to phoneCountryCode))
            .await()
        ProfileResult.Success()
    }

    override suspend fun updateAvatar(photo: ByteArray): ProfileResult = withUser { user ->
        val file = storage.reference.child("users/${user.uid}/avatar")
        file.putBytes(photo).await()
        val url = file.downloadUrl.await()
        user.updateProfile(userProfileChangeRequest { photoUri = url }).await()
        firestore.collection(USERS).document(user.uid).update(UserFields.PHOTO_URL, url.toString()).await()
        ProfileResult.Success()
    }

    override suspend fun requestEmailChange(newEmail: String): ProfileResult = withUser { user ->
        user.verifyBeforeUpdateEmail(newEmail).await()
        ProfileResult.Success(emailVerificationSentTo = newEmail)
    }

    override fun hasRecentSignIn(): Boolean {
        val lastSignIn = auth.currentUser?.metadata?.lastSignInTimestamp ?: return false
        return System.currentTimeMillis() - lastSignIn < RECENT_SIGN_IN_MILLIS
    }

    override suspend fun deleteAccount(): ProfileResult = withUser { user ->
        deletionGuard.deleting(user.uid) {
            firestore.collection(USERS).document(user.uid).delete().await()
            user.delete().await()
        }
        ProfileResult.Success()
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun withUser(block: suspend (FirebaseUser) -> ProfileResult): ProfileResult {
        val user = auth.currentUser ?: return ProfileResult.Failure(ProfileError.NOT_SIGNED_IN)
        return try {
            block(user)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ProfileResult.Failure(e.toProfileError())
        }
    }

    private companion object {
        const val USERS = "users"

        /** Firebase asks for a new sign-in for sensitive changes after about five minutes. */
        const val RECENT_SIGN_IN_MILLIS = 5 * 60 * 1000L
    }
}

internal fun Exception.toProfileError(): ProfileError = when (this) {
    is FirebaseAuthRecentLoginRequiredException -> ProfileError.REQUIRES_RECENT_LOGIN
    is FirebaseAuthUserCollisionException -> ProfileError.EMAIL_IN_USE
    is FirebaseAuthInvalidCredentialsException -> ProfileError.EMAIL_INVALID
    is FirebaseNetworkException -> ProfileError.NETWORK
    else -> ProfileError.UNKNOWN
}
