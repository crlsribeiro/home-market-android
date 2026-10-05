package app.carlosribeiro.homemarket.domain.repository

import app.carlosribeiro.homemarket.domain.model.ItemResult
import app.carlosribeiro.homemarket.domain.model.ListItem
import app.carlosribeiro.homemarket.domain.model.ListResult
import app.carlosribeiro.homemarket.domain.model.NewItem
import app.carlosribeiro.homemarket.domain.model.Week
import app.carlosribeiro.homemarket.domain.model.WeekList
import kotlinx.coroutines.flow.Flow

interface ListRepository {
    /** The newest open, locked or shopping list of the household, or null when there is none. */
    fun observeCurrentList(householdId: String): Flow<WeekList?>

    /** Pending, purchased and not-found items of the list, oldest first. */
    fun observeItems(listId: String): Flow<List<ListItem>>

    /** The household's `rolled_over` items, oldest first. */
    fun observeNextWeekItems(householdId: String): Flow<List<ListItem>>

    /**
     * Creates the household's list for [week] with the deterministic id, unless that document already
     * exists, and returns the list stored under that id.
     */
    suspend fun createWeekList(householdId: String, week: Week): ListResult

    /**
     * Writes a new item with `status: "pending"`, then uploads [photo] (JPEG bytes) to
     * `households/{householdId}/items/{itemId}/photo` and stores its download URL.
     */
    suspend fun addItem(item: NewItem, photo: ByteArray?): ItemResult
}
