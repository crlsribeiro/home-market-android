package app.carlosribeiro.homemarket.domain.usecase

import app.carlosribeiro.homemarket.domain.model.HouseholdError
import app.carlosribeiro.homemarket.domain.model.HouseholdResult
import app.carlosribeiro.homemarket.domain.model.UserRole
import app.carlosribeiro.homemarket.domain.repository.AuthRepository
import app.carlosribeiro.homemarket.domain.repository.HouseholdRepository
import app.carlosribeiro.homemarket.domain.util.TokenGenerator
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** Admin only: replaces the household invite token with a new 8-character one. */
class RegenerateInviteTokenUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val householdRepository: HouseholdRepository,
    private val tokenGenerator: TokenGenerator,
) {
    suspend operator fun invoke(): HouseholdResult {
        val user = authRepository.observeCurrentUser().first()
            ?: return HouseholdResult.Failure(HouseholdError.NOT_SIGNED_IN)
        val householdId = user.householdId
        if (householdId == null || user.role != UserRole.ADMIN) {
            return HouseholdResult.Failure(HouseholdError.NOT_ADMIN)
        }
        return householdRepository.updateInviteToken(
            householdId = householdId,
            inviteToken = tokenGenerator.generate(CreateHouseholdUseCase.INVITE_TOKEN_LENGTH),
        )
    }
}
