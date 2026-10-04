package app.carlosribeiro.homemarket.domain.usecase

import app.carlosribeiro.homemarket.domain.model.HouseholdError
import app.carlosribeiro.homemarket.domain.model.HouseholdResult
import app.carlosribeiro.homemarket.domain.repository.HouseholdRepository
import javax.inject.Inject

/** Joins a household by invite token. Tokens are case-sensitive, so only whitespace is trimmed. */
class JoinHouseholdUseCase @Inject constructor(
    private val householdRepository: HouseholdRepository,
) {
    suspend operator fun invoke(inviteToken: String, uid: String): HouseholdResult {
        val trimmed = inviteToken.trim()
        if (trimmed.isEmpty()) return HouseholdResult.Failure(HouseholdError.TOKEN_REQUIRED)
        return householdRepository.joinHousehold(trimmed, uid)
    }
}
