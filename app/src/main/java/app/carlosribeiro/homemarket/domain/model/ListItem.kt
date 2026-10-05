package app.carlosribeiro.homemarket.domain.model

import java.time.Instant

/** An item of a weekly list, as stored in `items/{itemId}`. */
data class ListItem(
    val id: String,
    val listId: String,
    val householdId: String,
    val name: String,
    val quantity: Int,
    val notes: String,
    val urgent: Boolean,
    val addedByUid: String,
    val addedByName: String,
    val status: ItemStatus,
    val approvalStatus: ApprovalStatus,
    val notFoundResolved: Boolean,
    val photoUrl: String?,
    val createdAt: Instant?
) {
    /** Still to buy: pending and not waiting for the admin's approval. */
    val isPendingPurchase: Boolean
        get() = status == ItemStatus.PENDING && approvalStatus != ApprovalStatus.PENDING

    val isUrgentToBuy: Boolean get() = urgent && status != ItemStatus.PURCHASED

    /** Web: a not-found item its author still has to decide about. */
    fun awaitsNotFoundDecisionBy(uid: String): Boolean =
        status == ItemStatus.NOT_FOUND && addedByUid == uid && !notFoundResolved
}

enum class ItemStatus {
    PENDING,
    PURCHASED,
    NOT_FOUND,
    ROLLED_OVER
}

enum class ApprovalStatus {
    NOT_REQUIRED,
    PENDING,
    APPROVED,
    REJECTED
}
