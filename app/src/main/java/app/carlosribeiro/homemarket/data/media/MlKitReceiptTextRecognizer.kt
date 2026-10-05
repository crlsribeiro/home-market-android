package app.carlosribeiro.homemarket.data.media

import android.content.Context
import android.net.Uri
import app.carlosribeiro.homemarket.domain.receipt.ReceiptTextRecognizer
import app.carlosribeiro.homemarket.domain.receipt.TextToken
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

/**
 * ML Kit on-device text recognition, in place of Apple Vision (iOS) and tesseract.js (web). Each
 * recognized line becomes a token; `InputImage.fromFilePath` applies the photo's EXIF rotation.
 */
class MlKitReceiptTextRecognizer @Inject constructor(@ApplicationContext private val context: Context) :
    ReceiptTextRecognizer {

    @Suppress("TooGenericExceptionCaught")
    override suspend fun recognize(uri: String): List<TextToken>? {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        return try {
            val image = InputImage.fromFilePath(context, Uri.parse(uri))
            recognizer.process(image).await().textBlocks
                .flatMap { it.lines }
                .mapNotNull { line ->
                    line.boundingBox?.let { box -> TextToken(line.text, box.left, box.top, box.right, box.bottom) }
                }
        } catch (e: CancellationException) {
            throw e
        } catch (ignored: Exception) {
            // An unreadable file (IOException) or a recognition failure (MlKitException).
            null
        } finally {
            recognizer.close()
        }
    }
}
