package app.carlosribeiro.homemarket.domain.usecase

import app.carlosribeiro.homemarket.domain.model.AuthResult
import app.carlosribeiro.homemarket.domain.repository.AuthRepository
import javax.inject.Inject

class SignInUseCase @Inject constructor(private val authRepository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String): AuthResult =
        authRepository.signIn(email.trim(), password)
}
