package app.carlosribeiro.homemarket.presentation.item

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class TimeAgoTest {

    private val now = Instant.parse("2026-10-05T12:00:00Z")

    @Test
    fun matchesTheWebTimeAgo() {
        assertEquals(TimeAgo.Unknown, TimeAgo.between(null, now))
        assertEquals(TimeAgo.Now, TimeAgo.between(now.minusSeconds(59), now))
        assertEquals(TimeAgo.Minutes(1), TimeAgo.between(now.minusSeconds(60), now))
        assertEquals(TimeAgo.Minutes(59), TimeAgo.between(now.minusSeconds(59 * 60), now))
        assertEquals(TimeAgo.Hours(1), TimeAgo.between(now.minusSeconds(60 * 60), now))
        assertEquals(TimeAgo.Hours(23), TimeAgo.between(now.minusSeconds(23 * 3600 + 59 * 60), now))
        assertEquals(TimeAgo.Days(2), TimeAgo.between(now.minusSeconds(50 * 3600), now))
    }
}
