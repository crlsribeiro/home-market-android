package app.carlosribeiro.homemarket.domain.util

import javax.inject.Inject
import kotlin.random.Random

/** Random ids and invite tokens from `[A-Za-z0-9]`, like `generateToken` in the web app. */
fun interface TokenGenerator {
    fun generate(length: Int): String
}

class RandomTokenGenerator @Inject constructor() : TokenGenerator {
    override fun generate(length: Int): String = buildString(length) {
        repeat(length) { append(ALPHABET[Random.nextInt(ALPHABET.length)]) }
    }

    private companion object {
        const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    }
}
