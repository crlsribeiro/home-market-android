package app.carlosribeiro.homemarket.domain.week

import app.carlosribeiro.homemarket.domain.model.Week
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/** Ports `getWeekStart`, `getWeekEnd` and `getWeekLabel` from the web app's `src/lib/utils.ts`. */
object WeekCalendar {
    private val portugueseMonths = listOf(
        "JAN", "FEV", "MAR", "ABR", "MAI", "JUN", "JUL", "AGO", "SET", "OUT", "NOV", "DEZ"
    )

    /** Web `weekEnd`: Sunday at 23:59:59.999. */
    private val endOfDay: LocalTime = LocalTime.MAX.truncatedTo(ChronoUnit.MILLIS)

    /** The week that contains [now]. Sunday belongs to the week that started six days earlier. */
    fun weekOf(now: Instant, zone: ZoneId): Week {
        val monday = now.atZone(zone).toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val sunday = monday.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
        return Week(
            monday = monday,
            start = monday.atStartOfDay(zone).toInstant(),
            end = sunday.atTime(endOfDay).atZone(zone).toInstant(),
            label = "${monday.dayOfMonth} – ${sunday.dayOfMonth} ${portugueseMonths[sunday.monthValue - 1]}"
        )
    }

    /** iOS list document id: `{householdId}_{yyyy-MM-dd}` of the week's Monday, see docs/backend.md. */
    fun listId(householdId: String, week: Week): String = "${householdId}_${week.monday}"
}
