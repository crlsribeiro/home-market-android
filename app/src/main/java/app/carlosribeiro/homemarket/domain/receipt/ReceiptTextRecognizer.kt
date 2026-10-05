package app.carlosribeiro.homemarket.domain.receipt

/** On-device OCR of a receipt photo (docs/backend.md: no backend function is involved). */
fun interface ReceiptTextRecognizer {
    /** Returns null when the photo cannot be read. */
    suspend fun recognize(uri: String): List<TextToken>?
}
