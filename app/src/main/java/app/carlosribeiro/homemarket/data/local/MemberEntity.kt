package app.carlosribeiro.homemarket.data.local

import androidx.room.Entity

@Entity(tableName = "members", primaryKeys = ["householdId", "uid"])
data class MemberEntity(
    val householdId: String,
    val uid: String,
    val displayName: String,
    val email: String,
    val photoUrl: String?,
    val role: String
)
