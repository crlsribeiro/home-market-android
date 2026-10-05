package app.carlosribeiro.homemarket.domain.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {

    @Test
    fun round2_roundsHalfUpToCents() {
        assertEquals(1.01, Money.round2(1.005), 0.0)
        assertEquals(10.47, Money.round2(3.49 * 3), 0.0)
        assertEquals(0.3, Money.round2(0.1 + 0.2), 0.0)
    }

    @Test
    fun parsePrice_acceptsADotOrACommaAndRejectsNegativeOrInvalidInput() {
        assertEquals(3.49, Money.parsePrice(" 3.49 ")!!, 0.0)
        assertEquals(3.49, Money.parsePrice("3,49")!!, 0.0)
        assertEquals(0.0, Money.parsePrice("0")!!, 0.0)
        assertNull(Money.parsePrice(""))
        assertNull(Money.parsePrice("abc"))
        assertNull(Money.parsePrice("-1"))
        assertNull(Money.parsePrice("NaN"))
    }
}
