package app.carlosribeiro.homemarket.domain.usecase

import app.carlosribeiro.homemarket.domain.model.AppUser
import app.carlosribeiro.homemarket.domain.repository.HouseholdRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveMembersUseCase @Inject constructor(
    private val householdRepository: HouseholdRepository,
) {
    operator fun invoke(householdId: String): Flow<List<AppUser>> = householdRepository.observeMembers(householdId)
}
