package app.carlosribeiro.homemarket.domain.receipt

/** An item read from a receipt: written as one `purchaseItems` document with quantity 1. */
data class ReceiptLine(val name: String, val price: Double)
