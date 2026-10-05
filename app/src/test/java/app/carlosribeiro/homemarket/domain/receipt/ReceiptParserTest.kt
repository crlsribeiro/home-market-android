package app.carlosribeiro.homemarket.domain.receipt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReceiptParserTest {

    @Test
    fun storeName_isTheFirstLineWithALetterWhenItIsShort() {
        assertEquals("H-E-B", ReceiptParser.storeName("\n  0042 \n H-E-B \n6.88"))
        assertNull(ReceiptParser.storeName("1234\n" + "A".repeat(41)))
        assertNull(ReceiptParser.storeName("1234\n5678"))
    }

    @Test
    fun items_stripTheIndexAndTheTaxCodeAndStopAtTheTotals() {
        val text = """
            H-E-B
            3 DESCHUTES FRESH SQUEEZED T 6.88
            12 SIERRA NEVADA HAZY IPA VP 10.99
            BANANAS FW 1.25
            SUBTOTAL 19.12
            TOMATOES 2.00
        """.trimIndent()

        assertEquals(
            listOf(
                ReceiptLine("DESCHUTES FRESH SQUEEZED", 6.88),
                ReceiptLine("SIERRA NEVADA HAZY IPA VP", 10.99),
                ReceiptLine("BANANAS", 1.25)
            ),
            ReceiptParser.items(text)
        )
    }

    @Test
    fun items_weightRowGivesItsLastPriceToTheNameAbove() {
        val text = "AVOCADOS HASS\n2 Ea. @ 1/ 3.82 F 7.64\nMILK 3.49"

        assertEquals(
            listOf(ReceiptLine("AVOCADOS HASS", 7.64), ReceiptLine("MILK", 3.49)),
            ReceiptParser.items(text)
        )
    }

    @Test
    fun items_fusedRowsKeepEveryPrice() {
        assertEquals(
            listOf(ReceiptLine("MILK", 3.49), ReceiptLine("EGGS", 4.20)),
            ReceiptParser.items("MILK T 3.49 EGGS 4.20")
        )
    }

    @Test
    fun items_unnamedPriceBecomesItemAndOutOfRangePricesAreDropped() {
        assertEquals(
            listOf(ReceiptLine("Item", 2.50)),
            ReceiptParser.items("T 2.50\nFREE BAG 0.00\nTV 999.99")
        )
    }

    @Test
    fun itemsOrPlaceholder_writesTheIosPlaceholderWhenNothingIsRecognized() {
        assertEquals(
            listOf(ReceiptLine(ReceiptParser.PLACEHOLDER_NAME, 0.0)),
            ReceiptParser.itemsOrPlaceholder("H-E-B")
        )
    }
}
