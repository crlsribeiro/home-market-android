package app.carlosribeiro.homemarket.domain.repository

import app.carlosribeiro.homemarket.domain.model.AppUser
import app.carlosribeiro.homemarket.domain.model.Household
import app.carlosribeiro.homemarket.domain.model.HouseholdResult
import kotlinx.coroutines.flow.Flow

interface HouseholdRepository {
    /** Emits the household from the local cache while keeping it in sync with Firestore. */
    fun observeHousehold(householdId: String): Flow<Household?>

    /** Emits the household members from the local cache while keeping them in sync. */
    fun observeMembers(householdId: String): Flow<List<AppUser>>

    /** Creates `households/{householdId}` with [uid] as admin and links the user to it. */
    suspend fun createHousehold(householdId: String, name: String, inviteToken: String, uid: String): HouseholdResult

    /** Finds the household with [inviteToken], adds [uid] as a member and links the user to it. */
    suspend fun joinHousehold(inviteToken: String, uid: String): HouseholdResult

    /** Replaces the invite token; the old one stops working. */
    suspend fun updateInviteToken(householdId: String, inviteToken: String): HouseholdResult
}
