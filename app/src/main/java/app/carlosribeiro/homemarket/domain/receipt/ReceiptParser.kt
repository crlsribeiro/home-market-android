package app.carlosribeiro.homemarket.domain.receipt

/**
 * iOS `HistoryService.extractStoreName` and `parseReceiptText`, rule for rule (docs/backend.md,
 * "Receipt parsing rules"). The parser targets H-E-B (US) receipts and USD prices.
 */
object ReceiptParser {
    /** iOS writes this single line when nothing is recognized, so the admin can edit it. */
    const val PLACEHOLDER_NAME = "No items recognized (tap the pencil to add one)"

    private const val MAX_STORE_NAME_LENGTH = 40
    private const val MAX_PRICE = 500.0
    private const val FALLBACK_NAME = "Item"
    private const val MIN_NAME_LENGTH = 2

    private val priceToken = Regex("""\d+\.\d{2}""")
    private val leadingIndex = Regex("""^\d+\s+""")

    /** Tax codes H-E-B prints next to a line. A fixed set, because real names can end in two capitals. */
    private val knownTaxCodes = listOf("T", "F", "FW", "TF", "TW", "TX", "HQ")
    private val stopWords = listOf(
        "subtotal", "total sale", "tax", "visa", "mastercard", "cash", "change", "items purchased", "account #"
    )

    /** The first line with a letter, when it is short enough to be a store name. */
    fun storeName(text: String): String? = lines(text)
        .firstOrNull { line -> line.any(Char::isLetter) }
        ?.takeIf { it.length <= MAX_STORE_NAME_LENGTH }

    /** Every item with a price above 0 and up to 500, or [placeholder] when there is none. */
    fun itemsOrPlaceholder(text: String): List<ReceiptLine> = items(text).ifEmpty { listOf(placeholder) }

    val placeholder = ReceiptLine(PLACEHOLDER_NAME, 0.0)

    fun items(text: String): List<ReceiptLine> {
        val state = ParserState()
        for (line in lines(text)) {
            val lower = line.lowercase()
            if (stopWords.any { lower.contains(it) }) break
            when {
                // A weight row ("2 Ea. @ 1/ 3.82 F 7.64"): its last price belongs to the pending name.
                "@" in line -> lastPrice(line)?.let { state.pendingPrice = it }

                priceToken.containsMatchIn(line) -> {
                    state.flush()
                    pricedRow(line).forEach { state.add(it.name, it.price) }
                }

                // No price: noise, or a weight item's name waiting for its "@" row.
                else -> {
                    state.flush()
                    state.pendingName = cleanName(line)
                }
            }
        }
        state.flush()
        return state.items
    }

    private fun lastPrice(line: String): Double? = priceToken.findAll(line).lastOrNull()?.value?.toDoubleOrNull()

    /** Every price on the row, each with the text before it: two rows that OCR fused still give both items. */
    private fun pricedRow(line: String): List<ReceiptLine> {
        var nameStart = 0
        return priceToken.findAll(line).mapNotNull { match ->
            val name = line.substring(nameStart, match.range.first)
            nameStart = match.range.last + 1
            match.value.toDoubleOrNull()?.let { ReceiptLine(cleanName(name), it) }
        }.toList()
    }

    private fun cleanName(raw: String): String {
        var name = raw.trim().replace(leadingIndex, "").trim()
        knownTaxCodes.firstOrNull { name.uppercase().endsWith(" $it") }?.let { name = name.dropLast(it.length) }
        val trimmed = name.trim()
        // A price never disappears because its name did not clean up: an editable "Item" is better.
        return if (trimmed.length < MIN_NAME_LENGTH || trimmed.uppercase() in knownTaxCodes) FALLBACK_NAME else trimmed
    }

    private fun lines(text: String): List<String> = text.split("\n").map { it.trim() }.filter { it.isNotEmpty() }

    private class ParserState {
        val items = mutableListOf<ReceiptLine>()
        var pendingName: String? = null
        var pendingPrice: Double? = null

        fun add(name: String, price: Double) {
            if (price > 0 && price <= MAX_PRICE) items.add(ReceiptLine(name, price))
        }

        fun flush() {
            val name = pendingName
            val price = pendingPrice
            if (name != null && price != null) add(name, price)
            pendingName = null
            pendingPrice = null
        }
    }
}
