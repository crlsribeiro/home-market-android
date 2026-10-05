package app.carlosribeiro.homemarket.domain.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RandomTokenGeneratorTest {

    private val generator = RandomTokenGenerator()

    @Test
    fun generatesTheRequestedLengthFromTheWebAlphabet() {
        repeat(100) {
            val token = generator.generate(8)
            assertEquals(8, token.length)
            assertTrue(token, token.all { it in 'A'..'Z' || it in 'a'..'z' || it in '0'..'9' })
        }
    }
}
