package app.carlosribeiro.homemarket.domain.usecase

import app.carlosribeiro.homemarket.domain.model.Household
import app.carlosribeiro.homemarket.domain.repository.HouseholdRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveHouseholdUseCase @Inject constructor(
    private val householdRepository: HouseholdRepository,
) {
    operator fun invoke(householdId: String): Flow<Household?> = householdRepository.observeHousehold(householdId)
}
