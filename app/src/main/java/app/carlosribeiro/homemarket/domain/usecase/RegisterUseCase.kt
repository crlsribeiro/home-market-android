package app.carlosribeiro.homemarket.domain.usecase

import app.carlosribeiro.homemarket.domain.model.AuthResult
import app.carlosribeiro.homemarket.domain.model.Registration
import app.carlosribeiro.homemarket.domain.repository.AuthRepository
import app.carlosribeiro.homemarket.domain.repository.ProfileRepository
import javax.inject.Inject

/**
 * iOS `AuthService.registerUser`: the account and `users/{uid}` come first; the optional profile photo
 * is uploaded afterwards and a failed upload does not fail the registration.
 */
class RegisterUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository
) {
    suspend operator fun invoke(registration: Registration, photo: ByteArray? = null): AuthResult {
        val result = authRepository.register(
            registration.copy(
                firstName = registration.firstName.trim(),
                lastName = registration.lastName.trim(),
                email = registration.email.trim(),
                phone = registration.phone.trim()
            )
        )
        if (result is AuthResult.Success && photo != null) profileRepository.updateAvatar(photo)
        return result
    }
}
