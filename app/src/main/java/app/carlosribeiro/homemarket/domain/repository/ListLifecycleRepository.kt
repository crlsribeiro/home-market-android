package app.carlosribeiro.homemarket.domain.repository

import app.carlosribeiro.homemarket.domain.model.AdminResult
import app.carlosribeiro.homemarket.domain.model.ListStatus

/** Admin writes that move a list or its items through their statuses (docs/backend.md). */
interface ListLifecycleRepository {
    /** Writes `lists/{listId}.status`, plus `closedAt: serverTimestamp()` when the status is closed. */
    suspend fun updateStatus(listId: String, status: ListStatus): AdminResult

    /** `approvalStatus: "approved"`; the item stays pending to buy. */
    suspend fun approveItem(itemId: String): AdminResult

    /** `approvalStatus: "rejected"` and `status: "rolled_over"`. */
    suspend fun rejectItem(itemId: String): AdminResult
}
