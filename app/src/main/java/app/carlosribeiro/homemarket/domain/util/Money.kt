package app.carlosribeiro.homemarket.domain.util

import java.math.BigDecimal
import java.math.RoundingMode

/** Money rules shared with the web and iOS apps: amounts are numbers rounded to 2 decimals. */
object Money {
    /** Rounds half up to cents, like `Math.round(x * 100) / 100` on the web and iOS. */
    fun round2(value: Double): Double = BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).toDouble()

    /** iOS `EditPurchaseItemView`: a comma is accepted as the decimal separator. Negative or invalid input is null. */
    fun parsePrice(text: String): Double? {
        val value = text.trim().replace(',', '.').toDoubleOrNull()
        return value?.takeIf { it.isFinite() && it >= 0 }
    }
}
