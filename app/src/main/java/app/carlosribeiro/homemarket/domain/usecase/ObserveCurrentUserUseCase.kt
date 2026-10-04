package app.carlosribeiro.homemarket.domain.usecase

import app.carlosribeiro.homemarket.domain.model.AppUser
import app.carlosribeiro.homemarket.domain.repository.AuthRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveCurrentUserUseCase @Inject constructor(private val authRepository: AuthRepository) {
    operator fun invoke(): Flow<AppUser?> = authRepository.observeCurrentUser()
}
