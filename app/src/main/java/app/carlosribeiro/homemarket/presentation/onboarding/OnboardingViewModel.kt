package app.carlosribeiro.homemarket.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.carlosribeiro.homemarket.domain.model.HouseholdError
import app.carlosribeiro.homemarket.domain.model.HouseholdResult
import app.carlosribeiro.homemarket.domain.usecase.CreateHouseholdUseCase
import app.carlosribeiro.homemarket.domain.usecase.JoinHouseholdUseCase
import app.carlosribeiro.homemarket.domain.usecase.ObserveCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class OnboardingMode {
    CREATE,
    JOIN
}

data class OnboardingUiState(
    val mode: OnboardingMode? = null,
    val value: String = "",
    val isSubmitting: Boolean = false,
    val error: HouseholdError? = null
)

sealed interface OnboardingUiEvent {
    data class SelectMode(val mode: OnboardingMode) : OnboardingUiEvent

    data object Back : OnboardingUiEvent

    data class ValueChanged(val value: String) : OnboardingUiEvent

    data object Submit : OnboardingUiEvent
}

/**
 * Create a household or join one with an invite code. On success the user document gets a
 * `householdId`, the session updates and the app moves on to the household screens by itself.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val createHousehold: CreateHouseholdUseCase,
    private val joinHousehold: JoinHouseholdUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    fun onEvent(event: OnboardingUiEvent) {
        when (event) {
            is OnboardingUiEvent.SelectMode -> _state.update { OnboardingUiState(mode = event.mode) }
            OnboardingUiEvent.Back -> if (!_state.value.isSubmitting) _state.update { OnboardingUiState() }
            is OnboardingUiEvent.ValueChanged -> _state.update { it.copy(value = event.value, error = null) }
            OnboardingUiEvent.Submit -> submit()
        }
    }

    private fun submit() {
        val current = _state.value
        val mode = current.mode ?: return
        if (current.isSubmitting) return
        _state.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            val uid = observeCurrentUser().first()?.uid
            val result = when {
                uid == null -> HouseholdResult.Failure(HouseholdError.NOT_SIGNED_IN)
                mode == OnboardingMode.CREATE -> createHousehold(current.value, uid)
                else -> joinHousehold(current.value, uid)
            }
            _state.update {
                it.copy(isSubmitting = false, error = (result as? HouseholdResult.Failure)?.error)
            }
        }
    }
}
