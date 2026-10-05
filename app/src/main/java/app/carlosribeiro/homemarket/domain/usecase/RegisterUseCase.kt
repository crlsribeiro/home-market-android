package app.carlosribeiro.homemarket.domain.usecase

import app.carlosribeiro.homemarket.domain.model.AuthResult
import app.carlosribeiro.homemarket.domain.model.Registration
import app.carlosribeiro.homemarket.domain.repository.AuthRepository
import javax.inject.Inject

class RegisterUseCase @Inject constructor(private val authRepository: AuthRepository) {
    suspend operator fun invoke(registration: Registration): AuthResult = authRepository.register(
        registration.copy(
            firstName = registration.firstName.trim(),
            lastName = registration.lastName.trim(),
            email = registration.email.trim()
        )
    )
}
