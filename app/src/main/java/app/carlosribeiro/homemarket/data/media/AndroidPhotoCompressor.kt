package app.carlosribeiro.homemarket.data.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import app.carlosribeiro.homemarket.domain.util.PhotoCompressor
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import javax.inject.Inject
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Scales the photo down to at most [MAX_SIDE] pixels on its longer side, as a JPEG. */
class AndroidPhotoCompressor @Inject constructor(@ApplicationContext private val context: Context) :
    PhotoCompressor {

    override suspend fun compress(uri: String): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val bitmap = decode(Uri.parse(uri)) ?: return@withContext null
            ByteArrayOutputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
                out.toByteArray()
            }
        } catch (ignored: IOException) {
            null
        } catch (ignored: SecurityException) {
            null
        }
    }

    private fun decode(uri: Uri): Bitmap? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        // ImageDecoder applies the EXIF rotation of camera photos.
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
            val longer = max(info.size.width, info.size.height)
            if (longer > MAX_SIDE) {
                decoder.setTargetSize(info.size.width * MAX_SIDE / longer, info.size.height * MAX_SIDE / longer)
            }
        }
    } else {
        decodeSampled(uri)
    }

    private fun decodeSampled(uri: Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sampleSize = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sampleSize * 2) >= MAX_SIDE) sampleSize *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        return context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    }

    private companion object {
        const val MAX_SIDE = 1600
        const val JPEG_QUALITY = 80
    }
}
