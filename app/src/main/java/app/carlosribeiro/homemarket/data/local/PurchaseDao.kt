package app.carlosribeiro.homemarket.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PurchaseDao {
    @Query(
        "SELECT purchases.*, lists.weekStart AS listWeekStart, lists.weekEnd AS listWeekEnd FROM purchases " +
            "LEFT JOIN lists ON lists.id = purchases.listId WHERE purchases.householdId = :householdId " +
            "ORDER BY purchases.createdAt DESC"
    )
    fun observePurchases(householdId: String): Flow<List<PurchaseWithWeek>>

    @Query(
        "SELECT purchases.*, lists.weekStart AS listWeekStart, lists.weekEnd AS listWeekEnd FROM purchases " +
            "LEFT JOIN lists ON lists.id = purchases.listId WHERE purchases.id = :purchaseId"
    )
    fun observePurchase(purchaseId: String): Flow<PurchaseWithWeek?>

    @Query("SELECT * FROM purchase_items WHERE purchaseId = :purchaseId ORDER BY rowid ASC")
    fun observeItems(purchaseId: String): Flow<List<PurchaseItemEntity>>

    @Query("DELETE FROM purchases WHERE id = :purchaseId")
    suspend fun deletePurchase(purchaseId: String)

    @Query("DELETE FROM purchases WHERE householdId = :householdId")
    suspend fun deletePurchases(householdId: String)

    @Upsert
    suspend fun upsertPurchases(purchases: List<PurchaseEntity>)

    @Transaction
    suspend fun replacePurchases(householdId: String, purchases: List<PurchaseEntity>) {
        deletePurchases(householdId)
        upsertPurchases(purchases)
    }

    @Query("DELETE FROM purchase_items WHERE purchaseId = :purchaseId")
    suspend fun deleteItems(purchaseId: String)

    @Upsert
    suspend fun upsertItems(items: List<PurchaseItemEntity>)

    @Transaction
    suspend fun replaceItems(purchaseId: String, items: List<PurchaseItemEntity>) {
        deleteItems(purchaseId)
        upsertItems(items)
    }
}
