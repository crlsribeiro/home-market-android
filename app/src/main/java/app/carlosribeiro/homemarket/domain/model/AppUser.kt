package app.carlosribeiro.homemarket.domain.model

/** A signed-in user, as stored in `users/{uid}`. */
data class AppUser(
    val uid: String,
    val displayName: String,
    val email: String,
    val photoUrl: String?,
    val householdId: String?,
    val role: UserRole,
    val firstName: String = "",
    val lastName: String = "",
    val phone: String = "",
    val phoneCountryCode: String = PhoneCountry.BRAZIL.dialCode
) {
    /**
     * iOS `AccountSettingsView.resolvedName`: Google sign-ins and some older accounts have no separate
     * first and last name, so the full name is split on its first space.
     */
    val resolvedName: Pair<String, String>
        get() {
            if (firstName.isNotEmpty() && lastName.isNotEmpty()) return firstName to lastName
            val parts = firstName.ifEmpty { displayName }.split(' ').filter { it.isNotEmpty() }
            return parts.firstOrNull().orEmpty() to parts.drop(1).joinToString(" ")
        }
}

enum class UserRole {
    ADMIN,
    MEMBER
}
