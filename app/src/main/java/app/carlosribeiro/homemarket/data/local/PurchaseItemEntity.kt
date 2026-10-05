package app.carlosribeiro.homemarket.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Cached `purchaseItems/{purchaseItemId}`. */
@Entity(tableName = "purchase_items", indices = [Index("purchaseId")])
data class PurchaseItemEntity(
    @PrimaryKey val id: String,
    val purchaseId: String,
    val name: String,
    val quantity: Int,
    val unitPrice: Double,
    val totalPrice: Double
)
