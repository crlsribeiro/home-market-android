package app.carlosribeiro.homemarket.presentation.components

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import app.carlosribeiro.homemarket.R

/**
 * The menu a photo control opens, like the iOS action sheet: take a photo or choose one from the
 * gallery. Place it in the same box as the control so it anchors there.
 */
@Composable
fun PhotoSourceMenu(expanded: Boolean, onDismiss: () -> Unit, onPhoto: (String) -> Unit) {
    val photoPicker = rememberPhotoPicker(onPhoto)
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.add_item_camera)) },
            leadingIcon = { Icon(painterResource(R.drawable.ic_photo_camera), contentDescription = null) },
            onClick = {
                onDismiss()
                photoPicker.takePhoto()
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.add_item_gallery)) },
            leadingIcon = { Icon(painterResource(R.drawable.ic_photo_library), contentDescription = null) },
            onClick = {
                onDismiss()
                photoPicker.pickFromGallery()
            }
        )
    }
}
