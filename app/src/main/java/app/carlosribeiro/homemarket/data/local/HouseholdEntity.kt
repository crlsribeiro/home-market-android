package app.carlosribeiro.homemarket.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "households")
data class HouseholdEntity(
    @PrimaryKey val id: String,
    val name: String,
    val adminUid: String,
    val inviteToken: String,
    val memberUids: List<String>,
)
