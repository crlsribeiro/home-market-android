package app.carlosribeiro.homemarket.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.carlosribeiro.homemarket.domain.model.AuthResult
import app.carlosribeiro.homemarket.domain.usecase.SendPasswordResetUseCase
import app.carlosribeiro.homemarket.domain.validation.AuthValidation
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ForgotPasswordUiState(
    val isOpen: Boolean = false,
    val email: String = "",
    val isSending: Boolean = false,
    val isSent: Boolean = false,
    val error: AuthFormError? = null
)

sealed interface ForgotPasswordUiEvent {
    /** Opens the dialog with the email typed on the login screen. */
    data class Open(val email: String) : ForgotPasswordUiEvent

    data object Dismiss : ForgotPasswordUiEvent

    data class EmailChanged(val value: String) : ForgotPasswordUiEvent

    data object Send : ForgotPasswordUiEvent
}

/** iOS `ForgotPasswordView`: send the reset email, then show a confirmation. */
@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(private val sendPasswordReset: SendPasswordResetUseCase) :
    ViewModel() {

    private val mutableState = MutableStateFlow(ForgotPasswordUiState())
    val state: StateFlow<ForgotPasswordUiState> = mutableState.asStateFlow()

    fun onEvent(event: ForgotPasswordUiEvent) {
        when (event) {
            is ForgotPasswordUiEvent.Open ->
                mutableState.value =
                    ForgotPasswordUiState(isOpen = true, email = event.email)

            ForgotPasswordUiEvent.Dismiss -> mutableState.value = ForgotPasswordUiState()

            is ForgotPasswordUiEvent.EmailChanged -> mutableState.update { it.copy(email = event.value, error = null) }

            ForgotPasswordUiEvent.Send -> send()
        }
    }

    private fun send() {
        val current = mutableState.value
        if (current.isSending) return
        AuthValidation.validateEmail(current.email)?.let { error ->
            mutableState.update { it.copy(error = AuthFormError.Validation(error)) }
            return
        }
        mutableState.update { it.copy(isSending = true, error = null) }
        viewModelScope.launch {
            val result = sendPasswordReset(current.email)
            mutableState.update {
                it.copy(
                    isSending = false,
                    isSent = result is AuthResult.Success,
                    error = (result as? AuthResult.Failure)?.let { failure -> AuthFormError.Auth(failure.error) }
                )
            }
        }
    }
}
