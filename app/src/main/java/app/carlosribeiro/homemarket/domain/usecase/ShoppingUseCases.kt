package app.carlosribeiro.homemarket.domain.usecase

import app.carlosribeiro.homemarket.domain.model.AdminResult
import app.carlosribeiro.homemarket.domain.model.ItemStatus
import app.carlosribeiro.homemarket.domain.model.ListItem
import app.carlosribeiro.homemarket.domain.model.ListStatus
import app.carlosribeiro.homemarket.domain.model.WeekList
import app.carlosribeiro.homemarket.domain.repository.ListLifecycleRepository
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
 * "Close list" at the end of shopping: the weekly cut (docs/backend.md). The purchase record that
 * follows it on the other clients comes with milestone M7.
 */
class CloseShoppingListUseCase @Inject constructor(
    private val adminCheck: AdminCheck,
    private val repository: ListLifecycleRepository
) {
    suspend operator fun invoke(list: WeekList): AdminResult = adminCheck.guarded(list.status == ListStatus.SHOPPING) {
        repository.weeklyCut(list.id)
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
 * "Not available" follows the web (docs/backend.md, decision 3): `status: "not_found"`, which makes
 * the `onItemNotFound` Cloud Function notify the person who added the item.
 */
class MarkNotFoundUseCase @Inject constructor(
    private val adminCheck: AdminCheck,
    private val repository: ListLifecycleRepository
) {
    suspend operator fun invoke(item: ListItem): AdminResult = adminCheck.guarded(item.status == ItemStatus.PENDING) {
        repository.setItemStatus(item.id, ItemStatus.NOT_FOUND)
    }
}

/** Shopping mode actions, grouped for the view model. */
class ShoppingActions @Inject constructor(
    val abandon: AbandonShoppingUseCase,
    val closeList: CloseShoppingListUseCase,
    val togglePurchased: TogglePurchasedUseCase,
    val markNotFound: MarkNotFoundUseCase
)
