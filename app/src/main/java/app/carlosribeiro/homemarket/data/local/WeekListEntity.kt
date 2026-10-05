package app.carlosribeiro.homemarket.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Cached `lists/{listId}`. Times are epoch milliseconds. */
@Entity(tableName = "lists", indices = [Index("householdId")])
data class WeekListEntity(
    @PrimaryKey val id: String,
    val householdId: String,
    val weekStart: Long,
    val weekEnd: Long,
    val status: String,
    val createdAt: Long?
)
