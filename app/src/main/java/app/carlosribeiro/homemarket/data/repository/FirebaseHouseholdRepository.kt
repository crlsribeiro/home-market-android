package app.carlosribeiro.homemarket.data.repository

import app.carlosribeiro.homemarket.data.local.HouseholdDao
import app.carlosribeiro.homemarket.data.mapper.HouseholdFields
import app.carlosribeiro.homemarket.data.mapper.HouseholdMapper
import app.carlosribeiro.homemarket.data.mapper.UserFields
import app.carlosribeiro.homemarket.data.mapper.UserMapper
import app.carlosribeiro.homemarket.domain.model.AppUser
import app.carlosribeiro.homemarket.domain.model.Household
import app.carlosribeiro.homemarket.domain.model.HouseholdError
import app.carlosribeiro.homemarket.domain.model.HouseholdResult
import app.carlosribeiro.homemarket.domain.repository.HouseholdRepository
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Offline-first: Firestore listeners write into Room, and callers read Room. Writes go straight to
 * Firestore; the listeners bring the result back into Room.
 */
@Singleton
class FirebaseHouseholdRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val householdDao: HouseholdDao
) : HouseholdRepository {

    private fun householdDocument(id: String) = firestore.collection(HOUSEHOLDS).document(id)

    private fun userDocument(uid: String) = firestore.collection(USERS).document(uid)

    override fun observeHousehold(householdId: String): Flow<Household?> = channelFlow {
        val registration = householdDocument(householdId).addSnapshotListener { snapshot, _ ->
            val data = snapshot?.data
            launch {
                when {
                    data != null -> householdDao.upsertHousehold(HouseholdMapper.documentToEntity(householdId, data))
                    snapshot != null -> householdDao.deleteHousehold(householdId)
                }
            }
        }
        launch {
            householdDao.observeHousehold(householdId).collect { send(it?.let(HouseholdMapper::entityToDomain)) }
        }
        awaitClose { registration.remove() }
    }.distinctUntilChanged()

    override fun observeMembers(householdId: String): Flow<List<AppUser>> = channelFlow {
        val registration = householdDocument(householdId).addSnapshotListener { snapshot, _ ->
            val memberUids = snapshot?.data?.let { HouseholdMapper.documentToEntity(householdId, it).memberUids }
                ?: return@addSnapshotListener
            launch { refreshMembers(householdId, memberUids) }
        }
        launch {
            householdDao.observeMembers(householdId).collect { members ->
                send(members.map(HouseholdMapper::memberToDomain))
            }
        }
        awaitClose { registration.remove() }
    }.distinctUntilChanged()

    /** Same fan-out read as the other clients: one `users/{uid}` read per member uid. */
    @Suppress("TooGenericExceptionCaught")
    private suspend fun refreshMembers(householdId: String, memberUids: List<String>) {
        try {
            val members = coroutineScope {
                memberUids.map { uid ->
                    async {
                        userDocument(uid).get().await().data?.let { data ->
                            UserMapper.fromDocument(uid, data, authDisplayName = null, authEmail = null)
                        }
                    }
                }.awaitAll().filterNotNull()
            }
            householdDao.replaceMembers(householdId, members.map { HouseholdMapper.memberToEntity(householdId, it) })
        } catch (e: CancellationException) {
            throw e
        } catch (ignored: Exception) {
            // Offline or not allowed: keep showing the cached members.
        }
    }

    override suspend fun createHousehold(
        householdId: String,
        name: String,
        inviteToken: String,
        uid: String
    ): HouseholdResult = runHousehold {
        firestore.batch()
            .set(
                householdDocument(householdId),
                mapOf(
                    HouseholdFields.NAME to name,
                    HouseholdFields.ADMIN_UID to uid,
                    HouseholdFields.INVITE_TOKEN to inviteToken,
                    HouseholdFields.MEMBER_UIDS to listOf(uid),
                    HouseholdFields.CREATED_AT to FieldValue.serverTimestamp()
                )
            )
            .update(
                userDocument(uid),
                mapOf(
                    UserFields.HOUSEHOLD_ID to householdId,
                    UserFields.ROLE to UserFields.ROLE_ADMIN
                )
            )
            .commit()
            .await()
        HouseholdResult.Success(householdId)
    }

    override suspend fun joinHousehold(inviteToken: String, uid: String): HouseholdResult = runHousehold {
        val match = firestore.collection(HOUSEHOLDS)
            .whereEqualTo(HouseholdFields.INVITE_TOKEN, inviteToken)
            .limit(1)
            .get()
            .await()
            .documents
            .firstOrNull()
            ?: return@runHousehold HouseholdResult.Failure(HouseholdError.TOKEN_NOT_FOUND)
        firestore.batch()
            .update(match.reference, HouseholdFields.MEMBER_UIDS, FieldValue.arrayUnion(uid))
            .update(
                userDocument(uid),
                mapOf(
                    UserFields.HOUSEHOLD_ID to match.id,
                    UserFields.ROLE to UserFields.ROLE_MEMBER
                )
            )
            .commit()
            .await()
        HouseholdResult.Success(match.id)
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun runHousehold(block: suspend () -> HouseholdResult): HouseholdResult = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        HouseholdResult.Failure(e.toHouseholdError())
    }

    private fun Exception.toHouseholdError(): HouseholdError = when {
        this is FirebaseNetworkException -> HouseholdError.NETWORK

        this is FirebaseFirestoreException && code == FirebaseFirestoreException.Code.UNAVAILABLE ->
            HouseholdError.NETWORK

        else -> HouseholdError.UNKNOWN
    }

    private companion object {
        const val HOUSEHOLDS = "households"
        const val USERS = "users"
    }
}
