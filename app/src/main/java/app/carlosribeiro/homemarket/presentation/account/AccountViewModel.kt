package app.carlosribeiro.homemarket.presentation.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.carlosribeiro.homemarket.domain.model.AppUser
import app.carlosribeiro.homemarket.domain.model.Household
import app.carlosribeiro.homemarket.domain.model.PhoneCountry
import app.carlosribeiro.homemarket.domain.model.ProfileError
import app.carlosribeiro.homemarket.domain.model.ProfileResult
import app.carlosribeiro.homemarket.domain.usecase.ObserveCurrentUserUseCase
import app.carlosribeiro.homemarket.domain.usecase.ObserveHouseholdUseCase
import app.carlosribeiro.homemarket.domain.usecase.ProfileInput
import app.carlosribeiro.homemarket.domain.usecase.SaveProfileUseCase
import app.carlosribeiro.homemarket.domain.usecase.UpdateAvatarUseCase
import app.carlosribeiro.homemarket.domain.util.PhotoCompressor
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Confirmation shown under the form after a save. */
sealed interface AccountMessage {
    data object Saved : AccountMessage

    data class CheckNewEmail(val email: String) : AccountMessage

    data object PhotoUpdated : AccountMessage
}

data class AccountUiState(
    val user: AppUser? = null,
    val household: Household? = null,
    val email: String = "",
    val phone: String = "",
    val country: PhoneCountry = PhoneCountry.BRAZIL,
    val isSaving: Boolean = false,
    val isUploadingPhoto: Boolean = false,
    val pendingPhotoUri: String? = null,
    val message: AccountMessage? = null,
    val error: ProfileError? = null
)

sealed interface AccountUiEvent {
    data class EmailChanged(val value: String) : AccountUiEvent

    data class PhoneChanged(val value: String) : AccountUiEvent

    data class CountryChanged(val country: PhoneCountry) : AccountUiEvent

    data object Save : AccountUiEvent

    data class PhotoPicked(val uri: String) : AccountUiEvent
}

/** iOS `AccountSettingsView`: photo, read-only names, email, phone, privacy policy and sign-out. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AccountViewModel @Inject constructor(
    observeCurrentUser: ObserveCurrentUserUseCase,
    observeHousehold: ObserveHouseholdUseCase,
    private val saveProfile: SaveProfileUseCase,
    private val updateAvatar: UpdateAvatarUseCase,
    private val photoCompressor: PhotoCompressor
) : ViewModel() {

    private val mutableState = MutableStateFlow(AccountUiState())
    val state: StateFlow<AccountUiState> = mutableState.asStateFlow()

    init {
        val users = observeCurrentUser().distinctUntilChanged()
        viewModelScope.launch {
            // Like iOS loadFields: the form follows the stored profile whenever it changes.
            users.collect { user ->
                mutableState.update {
                    it.copy(
                        user = user,
                        email = user?.email.orEmpty(),
                        phone = user?.phone.orEmpty(),
                        country = PhoneCountry.matching(user?.phoneCountryCode)
                    )
                }
            }
        }
        viewModelScope.launch {
            users.map { it?.householdId }
                .distinctUntilChanged()
                .flatMapLatest { id -> if (id == null) flowOf(null) else observeHousehold(id) }
                .collect { household -> mutableState.update { it.copy(household = household) } }
        }
    }

    fun onEvent(event: AccountUiEvent) {
        when (event) {
            is AccountUiEvent.EmailChanged -> mutableState.update { it.copy(email = event.value, message = null) }

            is AccountUiEvent.PhoneChanged ->
                mutableState.update { it.copy(phone = it.country.formatted(event.value), message = null) }

            is AccountUiEvent.CountryChanged -> mutableState.update { it.copy(country = event.country, phone = "") }

            AccountUiEvent.Save -> save()

            is AccountUiEvent.PhotoPicked -> uploadPhoto(event.uri)
        }
    }

    private fun save() {
        val form = mutableState.value
        if (form.isSaving) return
        mutableState.update { it.copy(isSaving = true, error = null, message = null) }
        viewModelScope.launch {
            val result = saveProfile(ProfileInput(form.email, form.phone, form.country))
            mutableState.update {
                when (result) {
                    is ProfileResult.Success -> it.copy(
                        isSaving = false,
                        message = result.emailVerificationSentTo?.let(AccountMessage::CheckNewEmail)
                            ?: AccountMessage.Saved
                    )

                    is ProfileResult.Failure -> it.copy(isSaving = false, error = result.error)
                }
            }
        }
    }

    private fun uploadPhoto(uri: String) {
        mutableState.update { it.copy(pendingPhotoUri = uri, isUploadingPhoto = true, error = null, message = null) }
        viewModelScope.launch {
            val photo = photoCompressor.compress(uri)
            val result = if (photo == null) ProfileResult.Failure(ProfileError.UNKNOWN) else updateAvatar(photo)
            mutableState.update {
                it.copy(
                    isUploadingPhoto = false,
                    message = AccountMessage.PhotoUpdated.takeIf { result is ProfileResult.Success },
                    error = (result as? ProfileResult.Failure)?.error
                )
            }
        }
    }
}
