package app.carlosribeiro.homemarket.data.local

import org.junit.Assert.assertEquals
import org.junit.Test

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun stringList_roundTrips() {
        val list = listOf("u1", "u2", "u3")

        assertEquals(list, converters.toStringList(converters.fromStringList(list)))
    }

    @Test
    fun emptyList_roundTrips() {
        assertEquals(emptyList<String>(), converters.toStringList(converters.fromStringList(emptyList())))
    }
}
