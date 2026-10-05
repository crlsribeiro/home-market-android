package app.carlosribeiro.homemarket.presentation.list

import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class WeekLabelFormatterTest {

    private val zone = ZoneId.of("America/Sao_Paulo")

    private fun at(value: String) = LocalDateTime.parse(value).atZone(zone).toInstant()

    @Test
    fun weekAcrossTwoMonths_showsBothMonths() {
        val label = WeekLabelFormatter.format(at("2026-09-28T00:00"), at("2026-10-04T23:59:59"), Locale.US, zone)

        assertEquals("28 Sep – 4 Oct", label)
    }

    @Test
    fun weekInOneMonth_showsTheMonthOnce() {
        val label = WeekLabelFormatter.format(at("2026-10-05T00:00"), at("2026-10-11T23:59:59"), Locale.US, zone)

        assertEquals("5 – 11 Oct", label)
    }
}
