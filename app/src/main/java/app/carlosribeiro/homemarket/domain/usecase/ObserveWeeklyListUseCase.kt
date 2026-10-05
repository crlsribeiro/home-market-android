package app.carlosribeiro.homemarket.domain.usecase

import app.carlosribeiro.homemarket.domain.model.ListItem
import app.carlosribeiro.homemarket.domain.model.WeekList
import app.carlosribeiro.homemarket.domain.model.WeeklyList
import app.carlosribeiro.homemarket.domain.repository.ListRepository
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class ObserveWeeklyListUseCase @Inject constructor(private val listRepository: ListRepository) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(householdId: String): Flow<WeeklyList> {
        val current: Flow<Pair<WeekList?, List<ListItem>>> = listRepository.observeCurrentList(householdId)
            .distinctUntilChanged()
            .flatMapLatest { list ->
                if (list == null) {
                    flowOf(null to emptyList())
                } else {
                    listRepository.observeItems(list.id).map { items -> list to items }
                }
            }
        return combine(current, listRepository.observeNextWeekItems(householdId)) { (list, items), nextWeek ->
            WeeklyList(currentList = list, items = items, nextWeekItems = nextWeek)
        }
    }
}
