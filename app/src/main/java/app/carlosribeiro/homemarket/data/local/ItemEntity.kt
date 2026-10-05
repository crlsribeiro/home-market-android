package app.carlosribeiro.homemarket.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Cached `items/{itemId}`, with every status. The DAO queries filter what each screen shows. */
@Entity(tableName = "items", indices = [Index("listId"), Index("householdId", "status")])
data class ItemEntity(
    @PrimaryKey val id: String,
    val listId: String,
    val householdId: String,
    val name: String,
    val quantity: Int,
    val notes: String,
    val urgent: Boolean,
    val addedByUid: String,
    val addedByName: String,
    val status: String,
    val approvalStatus: String,
    val notFoundResolved: Boolean,
    val photoUrl: String?,
    val createdAt: Long?
)
