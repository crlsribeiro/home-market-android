package app.carlosribeiro.homemarket.domain.repository

import app.carlosribeiro.homemarket.domain.model.AdminResult
import app.carlosribeiro.homemarket.domain.model.ItemStatus
import app.carlosribeiro.homemarket.domain.model.ListStatus

/** Admin writes that move a list or its items through their statuses (docs/backend.md). */
interface ListLifecycleRepository {
    /** Writes `lists/{listId}.status`, plus `closedAt: serverTimestamp()` when the status is closed. */
    suspend fun updateStatus(listId: String, status: ListStatus): AdminResult

    /**
     * The weekly cut, as one batch (docs/backend.md): the list becomes `closed` with `closedAt`, and
     * every item of the list that is still `pending` and not awaiting approval becomes `rolled_over`.
     * The items are read from Firestore at cut time, not from the screen's cache.
     */
    suspend fun weeklyCut(listId: String): AdminResult

    /** Writes `items/{itemId}.status` (purchased, back to pending, or not found). */
    suspend fun setItemStatus(itemId: String, status: ItemStatus): AdminResult

    /** `approvalStatus: "approved"`; the item stays pending to buy. */
    suspend fun approveItem(itemId: String): AdminResult

    /** `approvalStatus: "rejected"` and `status: "rolled_over"`. */
    suspend fun rejectItem(itemId: String): AdminResult
}
