package app.carlosribeiro.homemarket.data.mapper

import app.carlosribeiro.homemarket.domain.model.AppUser
import app.carlosribeiro.homemarket.domain.model.UserRole

/** Field names of `users/{uid}`, see docs/backend.md. */
object UserFields {
    const val UID = "uid"
    const val DISPLAY_NAME = "displayName"
    const val EMAIL = "email"
    const val PHOTO_URL = "photoURL"
    const val HOUSEHOLD_ID = "householdId"
    const val ROLE = "role"
    const val JOINED_AT = "joinedAt"
    const val FIRST_NAME = "firstName"
    const val LAST_NAME = "lastName"
    const val PHONE = "phone"
    const val PHONE_COUNTRY_CODE = "phoneCountryCode"
    const val PROVIDER = "provider"

    const val ROLE_ADMIN = "admin"
    const val ROLE_MEMBER = "member"

    /** Display name the web app writes when Auth has none. */
    const val DEFAULT_DISPLAY_NAME = "Usuário"
}

object UserMapper {
    /**
     * Maps a `users/{uid}` document. Same defaults as the web client: a missing or unknown role is a
     * member, a missing name falls back to the Auth values.
     */
    fun fromDocument(uid: String, data: Map<String, Any?>, authDisplayName: String?, authEmail: String?): AppUser =
        AppUser(
            uid = uid,
            displayName = (data[UserFields.DISPLAY_NAME] as? String)?.takeIf { it.isNotBlank() }
                ?: authDisplayName?.takeIf { it.isNotBlank() }
                ?: UserFields.DEFAULT_DISPLAY_NAME,
            email = (data[UserFields.EMAIL] as? String) ?: authEmail.orEmpty(),
            photoUrl = data[UserFields.PHOTO_URL] as? String,
            householdId = (data[UserFields.HOUSEHOLD_ID] as? String)?.takeIf { it.isNotBlank() },
            role = if (data[UserFields.ROLE] == UserFields.ROLE_ADMIN) UserRole.ADMIN else UserRole.MEMBER
        )
}
