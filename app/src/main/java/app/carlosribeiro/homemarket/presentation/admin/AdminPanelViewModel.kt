package app.carlosribeiro.homemarket.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.carlosribeiro.homemarket.domain.model.AppUser
import app.carlosribeiro.homemarket.domain.model.Household
import app.carlosribeiro.homemarket.domain.model.HouseholdError
import app.carlosribeiro.homemarket.domain.model.HouseholdResult
import app.carlosribeiro.homemarket.domain.usecase.ObserveCurrentUserUseCase
import app.carlosribeiro.homemarket.domain.usecase.ObserveHouseholdUseCase
import app.carlosribeiro.homemarket.domain.usecase.ObserveMembersUseCase
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
    val isRegenerating: Boolean = false,
    val error: HouseholdError? = null
)

sealed interface AdminPanelUiEvent {
    data object RegenerateInviteToken : AdminPanelUiEvent
}

@HiltViewModel
class AdminPanelViewModel @Inject constructor(
    observeCurrentUser: ObserveCurrentUserUseCase,
    observeHousehold: ObserveHouseholdUseCase,
    observeMembers: ObserveMembersUseCase,
    private val regenerateInviteToken: RegenerateInviteTokenUseCase
) : ViewModel() {

    private val action = MutableStateFlow(ActionState())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val householdData = observeCurrentUser()
        .map { it?.householdId }
        .distinctUntilChanged()
        .flatMapLatest { id ->
            if (id == null) {
                flowOf(null to emptyList())
            } else {
                combine(observeHousehold(id), observeMembers(id)) { household, members -> household to members }
            }
        }

    val state: StateFlow<AdminPanelUiState> = combine(householdData, action) { (household, members), action ->
        AdminPanelUiState(
            household = household,
            members = members,
            isRegenerating = action.isRegenerating,
            error = action.error
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), AdminPanelUiState())

    fun onEvent(event: AdminPanelUiEvent) {
        when (event) {
            AdminPanelUiEvent.RegenerateInviteToken -> regenerate()
        }
    }

    private fun regenerate() {
        if (action.value.isRegenerating) return
        action.value = ActionState(isRegenerating = true)
        viewModelScope.launch {
            val result = regenerateInviteToken()
            action.update { ActionState(error = (result as? HouseholdResult.Failure)?.error) }
        }
    }

    private data class ActionState(val isRegenerating: Boolean = false, val error: HouseholdError? = null)

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
