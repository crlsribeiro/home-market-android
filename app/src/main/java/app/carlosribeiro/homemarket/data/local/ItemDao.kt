package app.carlosribeiro.homemarket.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {
    @Query(
        "SELECT * FROM items WHERE listId = :listId AND status IN ('pending', 'purchased', 'not_found') " +
            "ORDER BY createdAt ASC"
    )
    fun observeItems(listId: String): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE householdId = :householdId AND status = 'rolled_over' ORDER BY createdAt ASC")
    fun observeNextWeekItems(householdId: String): Flow<List<ItemEntity>>

    @Query("DELETE FROM items WHERE listId = :listId")
    suspend fun deleteItemsOfList(listId: String)

    @Query("DELETE FROM items WHERE householdId = :householdId AND status = 'rolled_over'")
    suspend fun deleteNextWeekItems(householdId: String)

    @Upsert
    suspend fun upsertItems(items: List<ItemEntity>)

    @Transaction
    suspend fun replaceItemsOfList(listId: String, items: List<ItemEntity>) {
        deleteItemsOfList(listId)
        upsertItems(items)
    }

    @Transaction
    suspend fun replaceNextWeekItems(householdId: String, items: List<ItemEntity>) {
        deleteNextWeekItems(householdId)
        upsertItems(items)
    }
}
