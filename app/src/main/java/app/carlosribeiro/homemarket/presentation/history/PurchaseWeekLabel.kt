package app.carlosribeiro.homemarket.presentation.history

import app.carlosribeiro.homemarket.domain.model.Purchase
import app.carlosribeiro.homemarket.presentation.list.WeekLabelFormatter
import java.time.ZoneId
import java.util.Locale

/** The linked list's week in the device locale; the stored Portuguese label only when that list is unknown. */
fun Purchase.weekText(locale: Locale, zone: ZoneId = ZoneId.systemDefault()): String {
    val start = weekStart
    val end = weekEnd
    return if (start != null && end != null) WeekLabelFormatter.format(start, end, locale, zone) else weekLabel
}
