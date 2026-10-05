package app.carlosribeiro.homemarket.domain.repository

import app.carlosribeiro.homemarket.domain.model.ProfileResult

/** The signed-in user's own profile (iOS `AuthService` account settings). */
interface ProfileRepository {
    /** Writes `phone` and `phoneCountryCode` on `users/{uid}`; names are never rewritten here. */
    suspend fun updatePhone(phone: String, phoneCountryCode: String): ProfileResult

    /** Uploads to `users/{uid}/avatar`, then sets the Auth photo URL and `users/{uid}.photoURL`. */
    suspend fun updateAvatar(photo: ByteArray): ProfileResult

    /** Firebase verify-before-update: the login email changes only after the link sent to [newEmail]. */
    suspend fun requestEmailChange(newEmail: String): ProfileResult
}
