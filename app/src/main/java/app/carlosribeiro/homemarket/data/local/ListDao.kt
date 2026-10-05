package app.carlosribeiro.homemarket.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ListDao {
    /** Same rule as the web and iOS apps: the newest list by `createdAt` that is not closed. */
    @Query(
        "SELECT * FROM lists WHERE householdId = :householdId AND status IN ('open', 'locked', 'shopping') " +
            "ORDER BY createdAt DESC LIMIT 1"
    )
    fun observeCurrentList(householdId: String): Flow<WeekListEntity?>

    @Query("SELECT * FROM lists WHERE id = :listId")
    fun observeList(listId: String): Flow<WeekListEntity?>

    @Query("DELETE FROM lists WHERE householdId = :householdId")
    suspend fun deleteLists(householdId: String)

    @Upsert
    suspend fun upsertLists(lists: List<WeekListEntity>)

    @Transaction
    suspend fun replaceLists(householdId: String, lists: List<WeekListEntity>) {
        deleteLists(householdId)
        upsertLists(lists)
    }
}
