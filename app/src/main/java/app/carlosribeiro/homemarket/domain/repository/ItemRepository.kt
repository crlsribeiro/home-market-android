package app.carlosribeiro.homemarket.domain.repository

import app.carlosribeiro.homemarket.domain.model.ItemResult
import app.carlosribeiro.homemarket.domain.model.ListItem
import app.carlosribeiro.homemarket.domain.model.WeekList
import kotlinx.coroutines.flow.Flow

/** One item and its list, for the item detail screen. */
interface ItemRepository {
    /** The item, or null once it was deleted. */
    fun observeItem(itemId: String): Flow<ListItem?>

    /** The list the item was added to, when it is in the cache. */
    fun observeList(listId: String): Flow<WeekList?>

    suspend fun updateNotes(itemId: String, notes: String): ItemResult

    suspend fun removeItem(itemId: String): ItemResult

    /** Overwrites the photo at the item's Storage path and stores the new download URL. */
    suspend fun replacePhoto(item: ListItem, photo: ByteArray): ItemResult
}
