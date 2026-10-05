package app.carlosribeiro.homemarket.domain.receipt

/** One piece of recognized text and its box in image pixels, with y growing downwards. */
data class TextToken(val text: String, val left: Int, val top: Int, val right: Int, val bottom: Int)
