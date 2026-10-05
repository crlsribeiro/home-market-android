package app.carlosribeiro.homemarket.domain.usecase

import app.carlosribeiro.homemarket.domain.model.ListError
import app.carlosribeiro.homemarket.domain.model.ListResult
import app.carlosribeiro.homemarket.domain.model.UserRole
import app.carlosribeiro.homemarket.domain.repository.AuthRepository
import app.carlosribeiro.homemarket.domain.repository.ListRepository
import app.carlosribeiro.homemarket.domain.week.WeekCalendar
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * The admin creates this week's list. Members wait for the admin, as on iOS.
 *
 * If this week's list was already closed (closed early, before Sunday), the deterministic id points at
 * that closed list. Nothing is written and the result is [ListError.CLOSED_THIS_WEEK]: a new list can
 * be created next Monday (docs/backend.md, open question 2).
 */
class CreateWeekListUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val listRepository: ListRepository,
    private val clock: Clock
) {
    suspend operator fun invoke(): ListResult {
        val user = authRepository.observeCurrentUser().first()
        val householdId = user?.householdId
        return when {
            user == null -> ListResult.Failure(ListError.NOT_SIGNED_IN)

            householdId == null || user.role != UserRole.ADMIN -> ListResult.Failure(ListError.NOT_ADMIN)

            else -> listRepository.createWeekList(householdId, WeekCalendar.weekOf(clock.instant(), clock.zone))
                .rejectClosed()
        }
    }

    private fun ListResult.rejectClosed(): ListResult = if (this is ListResult.Success &&
        !list.status.isActive
    ) {
        ListResult.Failure(ListError.CLOSED_THIS_WEEK)
    } else {
        this
    }
}
