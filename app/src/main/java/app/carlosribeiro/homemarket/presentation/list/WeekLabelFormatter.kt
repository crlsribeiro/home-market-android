package app.carlosribeiro.homemarket.presentation.list

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Formats a week from `weekStart` and `weekEnd` with the device locale, for example "28 Sep – 4 Oct"
 * or "5 – 11 Oct". The stored `weekLabel` is never shown (docs/backend.md, decision 1).
 */
object WeekLabelFormatter {
    fun format(weekStart: Instant, weekEnd: Instant, locale: Locale, zone: ZoneId = ZoneId.systemDefault()): String {
        val start = weekStart.atZone(zone).toLocalDate()
        val end = weekEnd.atZone(zone).toLocalDate()
        val dayMonth = DateTimeFormatter.ofPattern("d MMM", locale)
        val startText = if (start.month == end.month) start.dayOfMonth.toString() else start.format(dayMonth)
        return "$startText – ${end.format(dayMonth)}"
    }
}
