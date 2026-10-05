package app.carlosribeiro.homemarket.domain.receipt

import org.junit.Assert.assertEquals
import org.junit.Test

class ReceiptReadingOrderTest {

    @Test
    fun tokensOnTheSamePrintedRowAreJoinedLeftToRight() {
        val tokens = listOf(
            TextToken("6.88", left = 400, top = 102, right = 460, bottom = 118),
            TextToken("H-E-B", left = 100, top = 10, right = 200, bottom = 40),
            TextToken("T", left = 360, top = 104, right = 370, bottom = 116),
            TextToken("3 DESCHUTES", left = 10, top = 100, right = 200, bottom = 120),
            TextToken("MILK 3.49", left = 10, top = 130, right = 200, bottom = 150)
        )

        assertEquals("H-E-B\n3 DESCHUTES T 6.88\nMILK 3.49", ReceiptReadingOrder.text(tokens))
    }

    @Test
    fun rowsAreComparedWithTheirFirstTokenSoTheyDoNotCascade() {
        // Each token overlaps the next one a little, but not the first token of the row.
        val tokens = listOf(
            TextToken("A", left = 0, top = 0, right = 10, bottom = 20),
            TextToken("B", left = 20, top = 15, right = 30, bottom = 35),
            TextToken("C", left = 40, top = 30, right = 50, bottom = 50)
        )

        assertEquals("A B\nC", ReceiptReadingOrder.text(tokens))
    }
}
