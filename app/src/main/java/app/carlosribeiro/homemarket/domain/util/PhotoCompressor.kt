package app.carlosribeiro.homemarket.domain.util

/** Turns a picked or captured photo into the JPEG bytes uploaded to Storage. */
fun interface PhotoCompressor {
    /** Returns null when the photo cannot be read. */
    suspend fun compress(uri: String): ByteArray?
}
