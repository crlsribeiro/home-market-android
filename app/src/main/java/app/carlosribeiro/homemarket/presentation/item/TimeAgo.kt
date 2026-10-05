package app.carlosribeiro.homemarket.presentation.item

import java.time.Duration
import java.time.Instant

/** Ports `timeAgo` from the web app's `src/lib/utils.ts`: now, minutes, hours or days ago. */
sealed interface TimeAgo {
    data object Unknown : TimeAgo

    data object Now : TimeAgo

    data class Minutes(val value: Long) : TimeAgo

    data class Hours(val value: Long) : TimeAgo

    data class Days(val value: Long) : TimeAgo

    companion object {
        private const val MINUTES_PER_HOUR = 60
        private const val HOURS_PER_DAY = 24

        fun between(createdAt: Instant?, now: Instant): TimeAgo {
            if (createdAt == null) return Unknown
            val minutes = Duration.between(createdAt, now).toMinutes()
            val hours = minutes / MINUTES_PER_HOUR
            return when {
                minutes < 1 -> Now
                minutes < MINUTES_PER_HOUR -> Minutes(minutes)
                hours < HOURS_PER_DAY -> Hours(hours)
                else -> Days(hours / HOURS_PER_DAY)
            }
        }
    }
}
