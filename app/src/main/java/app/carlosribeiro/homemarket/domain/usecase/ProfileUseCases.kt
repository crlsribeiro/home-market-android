package app.carlosribeiro.homemarket.domain.usecase

import app.carlosribeiro.homemarket.domain.model.PhoneCountry
import app.carlosribeiro.homemarket.domain.model.ProfileError
import app.carlosribeiro.homemarket.domain.model.ProfileResult
import app.carlosribeiro.homemarket.domain.repository.AuthRepository
import app.carlosribeiro.homemarket.domain.repository.ProfileRepository
import app.carlosribeiro.homemarket.domain.validation.AuthValidation
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** What the account settings form holds. */
data class ProfileInput(val email: String, val phone: String, val country: PhoneCountry)

/**
 * iOS `AccountSettingsView.save`: the phone is saved first; a different email then starts the
 * verify-before-update flow.
 */
class SaveProfileUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository
) {
    suspend operator fun invoke(input: ProfileInput): ProfileResult {
        val user = authRepository.observeCurrentUser().first()
        val phone = input.phone.trim()
        val email = input.email.trim()
        val emailChanged = email.isNotEmpty() && email != user?.email
        return when {
            user == null -> ProfileResult.Failure(ProfileError.NOT_SIGNED_IN)

            !input.country.isValidOrEmpty(phone) -> ProfileResult.Failure(ProfileError.PHONE_INCOMPLETE)

            emailChanged && AuthValidation.validateEmail(
                email
            ) != null -> ProfileResult.Failure(ProfileError.EMAIL_INVALID)

            else -> save(phone, input.country, email.takeIf { emailChanged })
        }
    }

    private suspend fun save(phone: String, country: PhoneCountry, newEmail: String?): ProfileResult {
        val saved = profileRepository.updatePhone(phone, country.dialCode)
        return if (saved is ProfileResult.Failure || newEmail == null) {
            saved
        } else {
            profileRepository.requestEmailChange(newEmail)
        }
    }
}

class UpdateAvatarUseCase @Inject constructor(private val profileRepository: ProfileRepository) {
    suspend operator fun invoke(photo: ByteArray): ProfileResult = profileRepository.updateAvatar(photo)
}
