package app.carlosribeiro.homemarket.domain.util

/** Money rules shared with the web and iOS apps: amounts are numbers rounded to 2 decimals. */
object Money {
    private const val CENTS = 100.0

    /**
     * The iOS and web formula, `(x * 100).rounded() / 100`, in floating point, so every client stores the
     * same total (1.005 becomes 1.0 there too, because 1.005 * 100 is 100.4999...).
     */
    fun round2(value: Double): Double = Math.round(value * CENTS) / CENTS

    /** iOS `EditPurchaseItemView`: a comma is accepted as the decimal separator. Negative or invalid input is null. */
    fun parsePrice(text: String): Double? {
        val value = text.trim().replace(',', '.').toDoubleOrNull()
        return value?.takeIf { it.isFinite() && it >= 0 }
    }
}
