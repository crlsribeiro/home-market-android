package app.carlosribeiro.homemarket.domain.usecase

import app.carlosribeiro.homemarket.domain.model.AppUser
import app.carlosribeiro.homemarket.domain.model.ApprovalStatus
import app.carlosribeiro.homemarket.domain.model.ItemError
import app.carlosribeiro.homemarket.domain.model.ItemResult
import app.carlosribeiro.homemarket.domain.model.ListError
import app.carlosribeiro.homemarket.domain.model.ListResult
import app.carlosribeiro.homemarket.domain.model.ListStatus
import app.carlosribeiro.homemarket.domain.model.NewItem
import app.carlosribeiro.homemarket.domain.model.WeekList
import app.carlosribeiro.homemarket.domain.repository.AuthRepository
import app.carlosribeiro.homemarket.domain.repository.ListRepository
import app.carlosribeiro.homemarket.domain.week.WeekCalendar
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** What the user typed in the add-item sheet. */
data class ItemInput(val name: String, val quantity: Int, val notes: String, val urgent: Boolean)

/**
 * iOS `ListViewModel.addItem`: any member can add an item. With no current list, this week's list is
 * created first (deterministic id, so two devices land on the same list). Items added after the list
 * left `open` wait for the admin's approval.
 */
class AddItemUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val listRepository: ListRepository,
    private val clock: Clock
) {
    /** Two quick adds on this device wait for the same list creation instead of racing. */
    private val listCreation = Mutex()

    suspend operator fun invoke(input: ItemInput, photo: ByteArray?): ItemResult {
        val name = input.name.trim()
        val user = authRepository.observeCurrentUser().first()
        val householdId = user?.householdId
        return when {
            name.isEmpty() -> ItemResult.Failure(ItemError.NAME_REQUIRED)

            user == null || householdId == null -> ItemResult.Failure(ItemError.NOT_SIGNED_IN)

            else -> when (val list = currentOrNewList(householdId)) {
                is ListResult.Failure -> ItemResult.Failure(list.error.toItemError())
                is ListResult.Success -> listRepository.addItem(newItem(list.list, user, input, name), photo)
            }
        }
    }

    private suspend fun currentOrNewList(householdId: String): ListResult = listCreation.withLock {
        val current = listRepository.observeCurrentList(householdId).first()
        if (current != null) return@withLock ListResult.Success(current)
        val created = listRepository.createWeekList(householdId, WeekCalendar.weekOf(clock.instant(), clock.zone))
        if (created is ListResult.Success && !created.list.status.isActive) {
            ListResult.Failure(ListError.CLOSED_THIS_WEEK)
        } else {
            created
        }
    }

    private fun newItem(list: WeekList, user: AppUser, input: ItemInput, name: String) = NewItem(
        listId = list.id,
        householdId = list.householdId,
        name = name,
        quantity = input.quantity.coerceIn(MIN_QUANTITY, MAX_QUANTITY),
        notes = input.notes,
        urgent = input.urgent,
        addedByUid = user.uid,
        addedByName = user.displayName,
        approvalStatus = if (list.status == ListStatus.OPEN) ApprovalStatus.NOT_REQUIRED else ApprovalStatus.PENDING
    )

    private fun ListError.toItemError(): ItemError = when (this) {
        ListError.CLOSED_THIS_WEEK -> ItemError.CLOSED_THIS_WEEK
        ListError.NETWORK -> ItemError.NETWORK
        ListError.NOT_SIGNED_IN -> ItemError.NOT_SIGNED_IN
        ListError.NOT_ADMIN, ListError.UNKNOWN -> ItemError.UNKNOWN
    }

    companion object {
        /** The iOS quantity stepper range. */
        const val MIN_QUANTITY = 1
        const val MAX_QUANTITY = 99
    }
}
