package app.carlosribeiro.homemarket.domain.week

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class WeekCalendarTest {

    private val saoPaulo = ZoneId.of("America/Sao_Paulo")

    private fun at(localDateTime: String) = LocalDateTime.parse(localDateTime).atZone(saoPaulo).toInstant()

    @Test
    fun weekStartsOnMondayAtMidnight() {
        val week = WeekCalendar.weekOf(at("2026-10-01T15:30:00"), saoPaulo)

        assertEquals(LocalDate.of(2026, 9, 28), week.monday)
        assertEquals(at("2026-09-28T00:00:00"), week.start)
    }

    @Test
    fun weekEndsOnSundayAtTheLastMillisecond() {
        val week = WeekCalendar.weekOf(at("2026-10-01T15:30:00"), saoPaulo)

        assertEquals(at("2026-10-04T23:59:59.999"), week.end)
    }

    @Test
    fun sundayBelongsToTheWeekThatStartedSixDaysEarlier() {
        val week = WeekCalendar.weekOf(at("2026-10-04T22:00:00"), saoPaulo)

        assertEquals(LocalDate.of(2026, 9, 28), week.monday)
    }

    @Test
    fun mondayStartsANewWeek() {
        val week = WeekCalendar.weekOf(at("2026-10-05T00:00:00"), saoPaulo)

        assertEquals(LocalDate.of(2026, 10, 5), week.monday)
    }

    @Test
    fun labelUsesTheWebPortugueseMonthOfTheSunday() {
        assertEquals("28 – 4 OUT", WeekCalendar.weekOf(at("2026-09-30T10:00:00"), saoPaulo).label)
        assertEquals("5 – 11 OUT", WeekCalendar.weekOf(at("2026-10-06T10:00:00"), saoPaulo).label)
        assertEquals("28 – 3 JAN", WeekCalendar.weekOf(at("2026-12-30T10:00:00"), saoPaulo).label)
    }

    @Test
    fun listIdIsTheHouseholdIdAndTheMondayDate() {
        val week = WeekCalendar.weekOf(at("2026-10-01T15:30:00"), saoPaulo)

        assertEquals("aB3dE5fG7hJ9kL1mN2pQ_2026-09-28", WeekCalendar.listId("aB3dE5fG7hJ9kL1mN2pQ", week))
    }

    @Test
    fun weekDependsOnTheDeviceTimeZone() {
        // Monday 02:00 UTC is still Sunday evening in São Paulo.
        val instant = Instant.parse("2026-10-05T02:00:00Z")

        assertEquals(LocalDate.of(2026, 10, 5), WeekCalendar.weekOf(instant, ZoneId.of("UTC")).monday)
        assertEquals(LocalDate.of(2026, 9, 28), WeekCalendar.weekOf(instant, saoPaulo).monday)
    }
}
