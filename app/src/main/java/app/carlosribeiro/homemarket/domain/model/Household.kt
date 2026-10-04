package app.carlosribeiro.homemarket.domain.model

/** A family household, as stored in `households/{householdId}`. */
data class Household(
    val id: String,
    val name: String,
    val adminUid: String,
    val inviteToken: String,
    val memberUids: List<String>,
)
