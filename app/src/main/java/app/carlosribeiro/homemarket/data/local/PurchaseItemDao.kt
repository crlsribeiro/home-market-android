package app.carlosribeiro.homemarket.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PurchaseItemDao {
    @Query("SELECT * FROM purchase_items WHERE purchaseId = :purchaseId ORDER BY rowid ASC")
    fun observeItems(purchaseId: String): Flow<List<PurchaseItemEntity>>

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
