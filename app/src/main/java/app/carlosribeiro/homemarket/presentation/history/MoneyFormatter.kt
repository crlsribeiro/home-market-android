package app.carlosribeiro.homemarket.presentation.history

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/** Amounts are US dollars, like the iOS app (receipt parsing targets H-E-B receipts, docs/backend.md). */
object MoneyFormatter {
    private val dollar: Currency = Currency.getInstance("USD")

    fun format(amount: Double, locale: Locale): String =
        NumberFormat.getCurrencyInstance(locale).apply { currency = dollar }.format(amount)
}
