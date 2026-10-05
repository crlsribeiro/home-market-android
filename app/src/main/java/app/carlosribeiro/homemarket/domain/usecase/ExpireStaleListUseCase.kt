package app.carlosribeiro.homemarket.domain.usecase

import app.carlosribeiro.homemarket.domain.model.AdminResult
import app.carlosribeiro.homemarket.domain.model.WeekList
import app.carlosribeiro.homemarket.domain.repository.ListLifecycleRepository
import app.carlosribeiro.homemarket.domain.repository.ListRepository
import app.carlosribeiro.homemarket.domain.week.WeekCalendar
import java.time.Clock
import javax.inject.Inject

/**
 * iOS `ListViewModel.expireIfStale`: when the current list's week has ended and nobody closed it,
 * any member's app runs the weekly cut without a purchase record, then creates this week's list.
 * Two devices doing it at once write the same values and land on the same new list (deterministic id).
 */
class ExpireStaleListUseCase @Inject constructor(
    private val lifecycleRepository: ListLifecycleRepository,
    private val listRepository: ListRepository,
    private val clock: Clock
) {
    fun isStale(list: WeekList): Boolean = list.weekEnd.isBefore(clock.instant())

    /** Returns null when the list is not stale and nothing was written. */
    suspend operator fun invoke(list: WeekList): AdminResult? {
        if (!isStale(list)) return null
        val cut = lifecycleRepository.weeklyCut(list.id)
        if (cut is AdminResult.Success) {
            listRepository.createWeekList(list.householdId, WeekCalendar.weekOf(clock.instant(), clock.zone))
        }
        return cut
    }
}
