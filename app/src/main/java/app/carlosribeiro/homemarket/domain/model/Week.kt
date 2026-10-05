package app.carlosribeiro.homemarket.domain.model

import java.time.Instant
import java.time.LocalDate

/**
 * A Monday-to-Sunday week in the device time zone.
 *
 * @property monday the date of [start], used in the list document id.
 * @property label the Portuguese label written to `lists.weekLabel`, like the web app.
 */
data class Week(val monday: LocalDate, val start: Instant, val end: Instant, val label: String)
