package app.carlosribeiro.homemarket.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.carlosribeiro.homemarket.domain.model.AdminError
import app.carlosribeiro.homemarket.domain.model.AdminResult
import app.carlosribeiro.homemarket.domain.model.AppUser
import app.carlosribeiro.homemarket.domain.model.ApprovalStatus
import app.carlosribeiro.homemarket.domain.model.Household
import app.carlosribeiro.homemarket.domain.model.HouseholdError
import app.carlosribeiro.homemarket.domain.model.HouseholdResult
import app.carlosribeiro.homemarket.domain.model.ListItem
import app.carlosribeiro.homemarket.domain.model.WeekList
import app.carlosribeiro.homemarket.domain.usecase.ListAdminActions
import app.carlosribeiro.homemarket.domain.usecase.ObserveCurrentUserUseCase
import app.carlosribeiro.homemarket.domain.usecase.ObserveHouseholdUseCase
import app.carlosribeiro.homemarket.domain.usecase.ObserveMembersUseCase
import app.carlosribeiro.homemarket.domain.usecase.ObserveWeeklyListUseCase
import app.carlosribeiro.homemarket.domain.usecase.RegenerateInviteTokenUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminPanelUiState(
    val household: Household? = null,
    val members: List<AppUser> = emptyList(),
    val currentList: WeekList? = null,
    val items: List<ListItem> = emptyList(),
    val isRegenerating: Boolean = false,
    val error: HouseholdError? = null,
    val adminError: AdminError? = null
) {
    /** iOS "Pending approvals": every item of the current list that waits for the admin. */
    val pendingApprovals: List<ListItem> get() = items.filter { it.approvalStatus == ApprovalStatus.PENDING }
}

sealed interface AdminPanelUiEvent {
    data object RegenerateInviteToken : AdminPanelUiEvent

    data object LockList : AdminPanelUiEvent

    data object ReopenList : AdminPanelUiEvent

    data class Approve(val item: ListItem) : AdminPanelUiEvent

    data class Reject(val item: ListItem) : AdminPanelUiEvent

    data object DismissAdminError : AdminPanelUiEvent
}

@HiltViewModel
class AdminPanelViewModel @Inject constructor(
    observeCurrentUser: ObserveCurrentUserUseCase,
    observeHousehold: ObserveHouseholdUseCase,
    observeMembers: ObserveMembersUseCase,
    observeWeeklyList: ObserveWeeklyListUseCase,
    private val regenerateInviteToken: RegenerateInviteTokenUseCase,
    private val listActions: ListAdminActions
) : ViewModel() {

    private val action = MutableStateFlow(ActionState())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val householdData = observeCurrentUser()
        .map { it?.householdId }
        .distinctUntilChanged()
        .flatMapLatest { id ->
            if (id == null) {
                flowOf(HouseholdData())
            } else {
                combine(observeHousehold(id), observeMembers(id), observeWeeklyList(id)) { household, members, weekly ->
                    HouseholdData(household, members, weekly.currentList, weekly.items)
                }
            }
        }

    val state: StateFlow<AdminPanelUiState> = combine(householdData, action) { data, action ->
        AdminPanelUiState(
            household = data.household,
            members = data.members,
            currentList = data.currentList,
            items = data.items,
            isRegenerating = action.isRegenerating,
            error = action.error,
            adminError = action.adminError
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), AdminPanelUiState())

    fun onEvent(event: AdminPanelUiEvent) {
        when (event) {
            AdminPanelUiEvent.RegenerateInviteToken -> regenerate()

            AdminPanelUiEvent.LockList -> state.value.currentList?.let { list ->
                runAdmin { listActions.lockList(list) }
            }

            AdminPanelUiEvent.ReopenList -> state.value.currentList?.let { list ->
                runAdmin { listActions.reopenList(list) }
            }

            is AdminPanelUiEvent.Approve -> runAdmin { listActions.approveItem(event.item) }

            is AdminPanelUiEvent.Reject -> runAdmin { listActions.rejectItem(event.item) }

            AdminPanelUiEvent.DismissAdminError -> action.update { it.copy(adminError = null) }
        }
    }

    private fun regenerate() {
        if (action.value.isRegenerating) return
        action.update { it.copy(isRegenerating = true, error = null) }
        viewModelScope.launch {
            val result = regenerateInviteToken()
            action.update { it.copy(isRegenerating = false, error = (result as? HouseholdResult.Failure)?.error) }
        }
    }

    /** The listeners show the new status at once (also offline); only a failure needs the UI. */
    private fun runAdmin(call: suspend () -> AdminResult) {
        viewModelScope.launch {
            val result = call()
            if (result is AdminResult.Failure) action.update { it.copy(adminError = result.error) }
        }
    }

    private data class HouseholdData(
        val household: Household? = null,
        val members: List<AppUser> = emptyList(),
        val currentList: WeekList? = null,
        val items: List<ListItem> = emptyList()
    )

    private data class ActionState(
        val isRegenerating: Boolean = false,
        val error: HouseholdError? = null,
        val adminError: AdminError? = null
    )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
