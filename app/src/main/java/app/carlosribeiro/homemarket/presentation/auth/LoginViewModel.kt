package app.carlosribeiro.homemarket.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.carlosribeiro.homemarket.domain.model.AuthError
import app.carlosribeiro.homemarket.domain.model.AuthResult
import app.carlosribeiro.homemarket.domain.usecase.SignInUseCase
import app.carlosribeiro.homemarket.domain.usecase.SignInWithGoogleUseCase
import app.carlosribeiro.homemarket.domain.validation.AuthValidation
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: AuthFormError? = null
)

sealed interface LoginUiEvent {
    data class EmailChanged(val value: String) : LoginUiEvent

    data class PasswordChanged(val value: String) : LoginUiEvent

    data object Submit : LoginUiEvent

    data class GoogleResult(val result: GoogleSignInResult) : LoginUiEvent
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val signIn: SignInUseCase,
    private val signInWithGoogle: SignInWithGoogleUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onEvent(event: LoginUiEvent) {
        when (event) {
            is LoginUiEvent.EmailChanged -> _state.update { it.copy(email = event.value, error = null) }
            is LoginUiEvent.PasswordChanged -> _state.update { it.copy(password = event.value, error = null) }
            LoginUiEvent.Submit -> submit()
            is LoginUiEvent.GoogleResult -> onGoogleResult(event.result)
        }
    }

    private fun submit() {
        val current = _state.value
        if (current.isLoading) return
        AuthValidation.validateSignIn(current.email, current.password)?.let { error ->
            _state.update { it.copy(error = AuthFormError.Validation(error)) }
            return
        }
        runSignIn { signIn(current.email, current.password) }
    }

    private fun onGoogleResult(result: GoogleSignInResult) {
        if (_state.value.isLoading) return
        when (result) {
            is GoogleSignInResult.Token -> runSignIn { signInWithGoogle(result.idToken) }

            GoogleSignInResult.Cancelled -> Unit

            GoogleSignInResult.Unavailable ->
                _state.update { it.copy(error = AuthFormError.Auth(AuthError.GOOGLE_UNAVAILABLE)) }
        }
    }

    private fun runSignIn(call: suspend () -> AuthResult) {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            // On success the session flow switches to the home screen; this screen just stops loading.
            val result = call()
            _state.update {
                it.copy(
                    isLoading = false,
                    error = (result as? AuthResult.Failure)?.let { f -> AuthFormError.Auth(f.error) }
                )
            }
        }
    }
}
