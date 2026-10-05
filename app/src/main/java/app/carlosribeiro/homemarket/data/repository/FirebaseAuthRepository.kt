package app.carlosribeiro.homemarket.data.repository

import app.carlosribeiro.homemarket.data.mapper.UserFields
import app.carlosribeiro.homemarket.data.mapper.UserMapper
import app.carlosribeiro.homemarket.domain.model.AppUser
import app.carlosribeiro.homemarket.domain.model.AuthError
import app.carlosribeiro.homemarket.domain.model.AuthResult
import app.carlosribeiro.homemarket.domain.model.Registration
import app.carlosribeiro.homemarket.domain.repository.AuthRepository
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await

@Singleton
class FirebaseAuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    private fun userDocument(uid: String) = firestore.collection(USERS).document(uid)

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeCurrentUser(): Flow<AppUser?> = auth.authStateFlow()
        .flatMapLatest { firebaseUser -> firebaseUser?.let(::userDocumentUpdates) ?: flowOf(null) }
        .distinctUntilChanged()

    private fun userDocumentUpdates(firebaseUser: FirebaseUser): Flow<AppUser?> = callbackFlow {
        val registration = userDocument(firebaseUser.uid).addSnapshotListener { snapshot, _ ->
            val data = snapshot?.data
            if (snapshot != null && data == null) {
                // First sign-in from this account: create the document like the other clients do.
                createUserDocumentIfMissing(firebaseUser)
            }
            trySend(
                UserMapper.fromDocument(
                    uid = firebaseUser.uid,
                    data = data.orEmpty(),
                    authDisplayName = firebaseUser.displayName,
                    authEmail = firebaseUser.email
                )
            )
        }
        awaitClose { registration.remove() }
    }

    /** Writes the default document only when none exists, so it never overwrites registration data. */
    private fun createUserDocumentIfMissing(firebaseUser: FirebaseUser) {
        val reference = userDocument(firebaseUser.uid)
        firestore.runTransaction { transaction ->
            if (!transaction.get(reference).exists()) {
                transaction.set(
                    reference,
                    mapOf(
                        UserFields.UID to firebaseUser.uid,
                        UserFields.DISPLAY_NAME to (
                            firebaseUser.displayName?.takeIf { it.isNotBlank() }
                                ?: UserFields.DEFAULT_DISPLAY_NAME
                            ),
                        UserFields.EMAIL to firebaseUser.email.orEmpty(),
                        UserFields.PHOTO_URL to firebaseUser.photoUrl?.toString(),
                        UserFields.HOUSEHOLD_ID to null,
                        UserFields.ROLE to UserFields.ROLE_MEMBER,
                        UserFields.JOINED_AT to FieldValue.serverTimestamp()
                    )
                )
            }
        }
    }

    override suspend fun signIn(email: String, password: String): AuthResult = runAuth {
        auth.signInWithEmailAndPassword(email, password).await()
    }

    /**
     * Same as the other clients: a Firebase Google credential. `users/{uid}` is created on the first
     * sign-in by the auth state listener, without `provider` (docs/backend.md).
     */
    override suspend fun signInWithGoogle(idToken: String): AuthResult = runAuth {
        auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
    }

    override suspend fun register(registration: Registration): AuthResult = runAuth {
        val user = checkNotNull(
            auth.createUserWithEmailAndPassword(registration.email, registration.password).await().user
        )
        val displayName = "${registration.firstName} ${registration.lastName}"
        user.updateProfile(userProfileChangeRequest { this.displayName = displayName }).await()
        userDocument(user.uid).set(
            mapOf(
                UserFields.UID to user.uid,
                UserFields.EMAIL to (user.email ?: registration.email),
                UserFields.DISPLAY_NAME to displayName,
                UserFields.FIRST_NAME to registration.firstName,
                UserFields.LAST_NAME to registration.lastName,
                UserFields.PHONE to registration.phone,
                UserFields.PHONE_COUNTRY_CODE to registration.phoneCountryCode,
                UserFields.PHOTO_URL to null,
                UserFields.PROVIDER to PROVIDER_EMAIL,
                UserFields.HOUSEHOLD_ID to null,
                UserFields.ROLE to UserFields.ROLE_MEMBER,
                UserFields.JOINED_AT to FieldValue.serverTimestamp()
            )
        ).await()
    }

    /** iOS `AuthService.resetPassword`. */
    override suspend fun sendPasswordReset(email: String): AuthResult = runAuth {
        auth.sendPasswordResetEmail(email).await()
    }

    override suspend fun signOut() {
        auth.signOut()
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun runAuth(block: suspend () -> Unit): AuthResult = try {
        block()
        AuthResult.Success
    } catch (e: Exception) {
        AuthResult.Failure(e.toAuthError())
    }

    private companion object {
        const val USERS = "users"
        const val PROVIDER_EMAIL = "email"
    }
}

private fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}.distinctUntilChanged { old, new -> old?.uid == new?.uid }

private fun Exception.toAuthError(): AuthError = when (this) {
    is FirebaseAuthWeakPasswordException -> AuthError.WEAK_PASSWORD

    is FirebaseAuthUserCollisionException -> AuthError.EMAIL_ALREADY_IN_USE

    is FirebaseAuthInvalidUserException -> AuthError.INVALID_CREDENTIALS

    is FirebaseAuthInvalidCredentialsException ->
        if (errorCode == "ERROR_INVALID_EMAIL") AuthError.INVALID_EMAIL else AuthError.INVALID_CREDENTIALS

    is FirebaseNetworkException -> AuthError.NETWORK

    else -> AuthError.UNKNOWN
}
