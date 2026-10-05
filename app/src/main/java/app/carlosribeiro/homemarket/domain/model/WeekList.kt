package app.carlosribeiro.homemarket.domain.model

import java.time.Instant

/** A household's shopping list for one week, as stored in `lists/{listId}`. */
data class WeekList(
    val id: String,
    val householdId: String,
    val weekStart: Instant,
    val weekEnd: Instant,
    val status: ListStatus,
    val createdAt: Instant?
)

enum class ListStatus {
    OPEN,
    LOCKED,
    SHOPPING,
    CLOSED;

    /** Open, locked and shopping lists can be the household's current list. */
    val isActive: Boolean get() = this != CLOSED
}
