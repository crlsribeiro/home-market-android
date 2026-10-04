package app.carlosribeiro.homemarket.domain.usecase

import app.carlosribeiro.homemarket.domain.repository.AuthRepository
import javax.inject.Inject

class SignOutUseCase @Inject constructor(private val authRepository: AuthRepository) {
    suspend operator fun invoke() = authRepository.signOut()
}
