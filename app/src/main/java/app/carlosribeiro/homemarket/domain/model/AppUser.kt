package app.carlosribeiro.homemarket.domain.model

/** A signed-in user, as stored in `users/{uid}`. */
data class AppUser(
    val uid: String,
    val displayName: String,
    val email: String,
    val photoUrl: String?,
    val householdId: String?,
    val role: UserRole
)

enum class UserRole {
    ADMIN,
    MEMBER
}
