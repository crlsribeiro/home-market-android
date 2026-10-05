package app.carlosribeiro.homemarket.domain.model

/**
 * iOS `ItemDetailView` price, shown to the admin only (docs/backend.md, decision 4).
 *
 * @property unitPrice the unit price of the receipt line with the item's name, or null until a receipt
 * with that line was uploaded for the item's list ("available after receipt upload").
 */
data class ItemPrice(val unitPrice: Double?, val quantity: Int) {
    /** iOS: unit price times the item's quantity. */
    val total: Double? get() = unitPrice?.let { it * quantity }
}
