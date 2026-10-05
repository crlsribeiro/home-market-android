package app.carlosribeiro.homemarket.domain.receipt

/**
 * iOS `HistoryService.reconstructReadingOrder`. OCR returns text in blocks, not printed rows, and not
 * always in page order. Tokens are grouped into rows by vertical overlap with the row's first token
 * (a fixed reference, so a row never grows into the next one), then each row is read left to right.
 */
object ReceiptReadingOrder {
    fun text(tokens: List<TextToken>): String {
        val rows = mutableListOf<MutableList<TextToken>>()
        tokens.sortedBy { it.top + it.bottom }.forEach { token ->
            val reference = rows.lastOrNull()?.first()
            if (reference != null && token.top < reference.bottom && token.bottom > reference.top) {
                rows.last().add(token)
            } else {
                rows.add(mutableListOf(token))
            }
        }
        return rows.joinToString("\n") { row -> row.sortedBy { it.left }.joinToString(" ") { it.text } }
    }
}
