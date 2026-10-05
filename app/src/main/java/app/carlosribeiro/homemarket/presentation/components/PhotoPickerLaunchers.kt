package app.carlosribeiro.homemarket.presentation.components

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

/** Opens the camera or the system photo picker and reports the chosen photo's URI. */
class PhotoPickerLaunchers(val takePhoto: () -> Unit, val pickFromGallery: () -> Unit)

@Composable
fun rememberPhotoPicker(onPhoto: (String) -> Unit): PhotoPickerLaunchers {
    val context = LocalContext.current
    var pendingCameraUri by rememberSaveable { mutableStateOf<String?>(null) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        pendingCameraUri?.takeIf { saved }?.let(onPhoto)
        pendingCameraUri = null
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { onPhoto(it.toString()) }
    }
    return PhotoPickerLaunchers(
        takePhoto = {
            val uri = newCameraUri(context)
            pendingCameraUri = uri.toString()
            camera.launch(uri)
        },
        pickFromGallery = {
            gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    )
}

private fun newCameraUri(context: Context): Uri {
    val directory = File(context.cacheDir, "photos").apply { mkdirs() }
    val file = File(directory, "${UUID.randomUUID()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
