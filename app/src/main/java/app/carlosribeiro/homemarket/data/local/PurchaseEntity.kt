package app.carlosribeiro.homemarket.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Cached `purchases/{purchaseId}`. Times are epoch milliseconds. */
@Entity(tableName = "purchases", indices = [Index("householdId")])
data class PurchaseEntity(
    @PrimaryKey val id: String,
    val listId: String,
    val householdId: String,
    val weekLabel: String,
    val total: Double,
    val receiptUrl: String?,
    val receiptProcessed: Boolean,
    val createdAt: Long?,
    val storeName: String?
)
