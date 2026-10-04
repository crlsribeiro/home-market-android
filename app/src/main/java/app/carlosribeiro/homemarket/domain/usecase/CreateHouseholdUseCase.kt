package app.carlosribeiro.homemarket.domain.usecase

import app.carlosribeiro.homemarket.domain.model.HouseholdError
import app.carlosribeiro.homemarket.domain.model.HouseholdResult
import app.carlosribeiro.homemarket.domain.repository.HouseholdRepository
import app.carlosribeiro.homemarket.domain.util.TokenGenerator
import javax.inject.Inject

/** Creates a household with a 20-character id and an 8-character invite token; the creator becomes admin. */
class CreateHouseholdUseCase @Inject constructor(
    private val householdRepository: HouseholdRepository,
    private val tokenGenerator: TokenGenerator
) {
    suspend operator fun invoke(name: String, uid: String): HouseholdResult {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return HouseholdResult.Failure(HouseholdError.NAME_REQUIRED)
        return householdRepository.createHousehold(
            householdId = tokenGenerator.generate(HOUSEHOLD_ID_LENGTH),
            name = trimmed,
            inviteToken = tokenGenerator.generate(INVITE_TOKEN_LENGTH),
            uid = uid
        )
    }

    companion object {
        const val HOUSEHOLD_ID_LENGTH = 20
        const val INVITE_TOKEN_LENGTH = 8
    }
}
