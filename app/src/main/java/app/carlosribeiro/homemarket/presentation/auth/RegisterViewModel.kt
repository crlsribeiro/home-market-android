package app.carlosribeiro.homemarket.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.carlosribeiro.homemarket.domain.model.AuthResult
import app.carlosribeiro.homemarket.domain.model.PhoneCountry
import app.carlosribeiro.homemarket.domain.model.Registration
import app.carlosribeiro.homemarket.domain.usecase.RegisterUseCase
import app.carlosribeiro.homemarket.domain.util.PhotoCompressor
import app.carlosribeiro.homemarket.domain.validation.AuthValidation
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegisterUiState(
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val phone: String = "",
    val country: PhoneCountry = PhoneCountry.BRAZIL,
    val photoUri: String? = null,
    val isLoading: Boolean = false,
    val error: AuthFormError? = null
)

sealed interface RegisterUiEvent {
    data class FirstNameChanged(val value: String) : RegisterUiEvent

    data class LastNameChanged(val value: String) : RegisterUiEvent

    data class EmailChanged(val value: String) : RegisterUiEvent

    data class PasswordChanged(val value: String) : RegisterUiEvent

    data class ConfirmPasswordChanged(val value: String) : RegisterUiEvent

    data class PhoneChanged(val value: String) : RegisterUiEvent

    data class CountryChanged(val country: PhoneCountry) : RegisterUiEvent

    data class PhotoPicked(val uri: String) : RegisterUiEvent

    data object RemovePhoto : RegisterUiEvent

    data object Submit : RegisterUiEvent
}

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val register: RegisterUseCase,
    private val photoCompressor: PhotoCompressor
) : ViewModel() {

    private val _state = MutableStateFlow(RegisterUiState())
    val state: StateFlow<RegisterUiState> = _state.asStateFlow()

    fun onEvent(event: RegisterUiEvent) {
        when (event) {
            is RegisterUiEvent.FirstNameChanged -> _state.update { it.copy(firstName = event.value, error = null) }

            is RegisterUiEvent.LastNameChanged -> _state.update { it.copy(lastName = event.value, error = null) }

            is RegisterUiEvent.EmailChanged -> _state.update { it.copy(email = event.value, error = null) }

            is RegisterUiEvent.PasswordChanged -> _state.update { it.copy(password = event.value, error = null) }

            is RegisterUiEvent.ConfirmPasswordChanged ->
                _state.update { it.copy(confirmPassword = event.value, error = null) }

            is RegisterUiEvent.PhoneChanged ->
                _state.update { it.copy(phone = it.country.formatted(event.value), error = null) }

            is RegisterUiEvent.CountryChanged -> _state.update { it.copy(country = event.country, phone = "") }

            is RegisterUiEvent.PhotoPicked -> _state.update { it.copy(photoUri = event.uri) }

            RegisterUiEvent.RemovePhoto -> _state.update { it.copy(photoUri = null) }

            RegisterUiEvent.Submit -> submit()
        }
    }

    private fun submit() {
        val current = _state.value
        if (current.isLoading) return
        val registration = Registration(
            firstName = current.firstName,
            lastName = current.lastName,
            email = current.email,
            password = current.password,
            phone = current.phone,
            phoneCountryCode = current.country.dialCode
        )
        AuthValidation.validateRegistration(registration, current.confirmPassword)?.let { error ->
            _state.update { it.copy(error = AuthFormError.Validation(error)) }
            return
        }
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val photo = current.photoUri?.let { photoCompressor.compress(it) }
            val result = register(registration, photo)
            _state.update {
                it.copy(
                    isLoading = false,
                    error = (result as? AuthResult.Failure)?.let { f ->
                        AuthFormError.Auth(f.error)
                    }
                )
            }
        }
    }
}
