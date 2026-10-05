package app.carlosribeiro.homemarket.domain.model

import java.time.Instant

/**
 * A closed list's purchase record, as stored in `purchases/{purchaseId}` (docs/backend.md).
 *
 * @property weekStart start of the linked list's week, or null when that list is not cached. The UI then
 * falls back to the stored Portuguese [weekLabel] (decision 1).
 */
data class Purchase(
    val id: String,
    val listId: String,
    val householdId: String,
    val weekLabel: String,
    val weekStart: Instant?,
    val weekEnd: Instant?,
    val total: Double,
    val receiptUrl: String?,
    val receiptProcessed: Boolean,
    val createdAt: Instant?,
    val storeName: String?
)

/** One receipt line, as stored in `purchaseItems/{purchaseItemId}`. */
data class PurchaseItem(
    val id: String,
    val purchaseId: String,
    val name: String,
    val quantity: Int,
    val unitPrice: Double,
    val totalPrice: Double
)

/** A purchase with its lines, for the detail screen. */
data class PurchaseDetail(val purchase: Purchase, val items: List<PurchaseItem>)
