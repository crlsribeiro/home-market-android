package app.carlosribeiro.homemarket.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface HouseholdDao {
    @Query("SELECT * FROM households WHERE id = :householdId")
    fun observeHousehold(householdId: String): Flow<HouseholdEntity?>

    @Upsert
    suspend fun upsertHousehold(household: HouseholdEntity)

    @Query("DELETE FROM households WHERE id = :householdId")
    suspend fun deleteHousehold(householdId: String)

    @Query("SELECT * FROM members WHERE householdId = :householdId ORDER BY displayName COLLATE NOCASE")
    fun observeMembers(householdId: String): Flow<List<MemberEntity>>

    @Query("DELETE FROM members WHERE householdId = :householdId")
    suspend fun deleteMembers(householdId: String)

    @Upsert
    suspend fun upsertMembers(members: List<MemberEntity>)

    @Transaction
    suspend fun replaceMembers(householdId: String, members: List<MemberEntity>) {
        deleteMembers(householdId)
        upsertMembers(members)
    }
}
