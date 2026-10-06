package app.carlosribeiro.homemarket.domain.usecase

import app.carlosribeiro.homemarket.domain.model.AdminResult
import app.carlosribeiro.homemarket.domain.model.ItemStatus
import app.carlosribeiro.homemarket.domain.model.ListItem
import app.carlosribeiro.homemarket.domain.model.ListStatus
import app.carlosribeiro.homemarket.domain.model.WeekList
import app.carlosribeiro.homemarket.domain.repository.ListLifecycleRepository
import app.carlosribeiro.homemarket.domain.repository.PurchaseRepository
import app.carlosribeiro.homemarket.domain.week.WeekCalendar
import java.time.Clock
import javax.inject.Inject

/** iOS cart button: the admin starts shopping from an open or locked list that has items. */
class StartShoppingUseCase @Inject constructor(
    private val adminCheck: AdminCheck,
    private val repository: ListLifecycleRepository
) {
    suspend operator fun invoke(list: WeekList, items: List<ListItem>): AdminResult {
        val allowed = (list.status == ListStatus.OPEN || list.status == ListStatus.LOCKED) && items.isNotEmpty()
        return adminCheck.guarded(allowed) { repository.updateStatus(list.id, ListStatus.SHOPPING) }
    }
}

/** iOS "Abandon shopping": `shopping → open`. Item statuses stay as they are. */
class AbandonShoppingUseCase @Inject constructor(
    private val adminCheck: AdminCheck,
    private val repository: ListLifecycleRepository
) {
    suspend operator fun invoke(list: WeekList): AdminResult = adminCheck.guarded(list.status == ListStatus.SHOPPING) {
        repository.updateStatus(list.id, ListStatus.OPEN)
    }
}

/**
 * iOS "Close list" at the end of shopping: the weekly cut (docs/backend.md), then the purchase record.
 * The record copies the Portuguese week label the list was created with (decision 1).
 */
class CloseShoppingListUseCase @Inject constructor(
    private val adminCheck: AdminCheck,
    private val repository: ListLifecycleRepository,
    private val purchaseRepository: PurchaseRepository,
    private val clock: Clock
) {
    suspend operator fun invoke(list: WeekList): AdminResult = adminCheck.guarded(list.status == ListStatus.SHOPPING) {
        when (val cut = repository.weeklyCut(list.id)) {
            AdminResult.Success -> {
                val weekLabel = WeekCalendar.weekOf(list.weekStart, clock.zone).label
                purchaseRepository.createPurchaseForList(list.id, list.householdId, weekLabel)
            }

            is AdminResult.Failure -> cut
        }
    }
}

/** "Got it" and undo: the web `toggleItem`, `pending ↔ purchased`. */
class TogglePurchasedUseCase @Inject constructor(
    private val adminCheck: AdminCheck,
    private val repository: ListLifecycleRepository
) {
    suspend operator fun invoke(item: ListItem): AdminResult {
        val next = when (item.status) {
            ItemStatus.PENDING -> ItemStatus.PURCHASED
            ItemStatus.PURCHASED -> ItemStatus.PENDING
            ItemStatus.NOT_FOUND, ItemStatus.ROLLED_OVER -> null
        }
        return adminCheck.guarded(next != null) { repository.setItemStatus(item.id, checkNotNull(next)) }
    }
}

/**
 * iOS "Not available" (docs/backend.md, decision 3): the item goes straight to next week, with
 * `status: "rolled_over"` and `notFoundResolved: true`. Nobody has to resolve it later.
 */
class MarkNotFoundUseCase @Inject constructor(
    private val adminCheck: AdminCheck,
    private val repository: ListLifecycleRepository
) {
    suspend operator fun invoke(item: ListItem): AdminResult = adminCheck.guarded(item.status == ItemStatus.PENDING) {
        repository.moveNotFoundToNextWeek(item.id)
    }
}

/** Shopping mode actions, grouped for the view model. */
class ShoppingActions @Inject constructor(
    val abandon: AbandonShoppingUseCase,
    val closeList: CloseShoppingListUseCase,
    val togglePurchased: TogglePurchasedUseCase,
    val markNotFound: MarkNotFoundUseCase
)
