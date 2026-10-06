package app.carlosribeiro.homemarket.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.carlosribeiro.homemarket.R

/** "Camera" and "Gallery" side by side in the iOS outline style, for item and receipt photos. */
@Composable
fun PhotoSourceButtons(onPhoto: (String) -> Unit, enabled: Boolean, modifier: Modifier = Modifier) {
    val photoPicker = rememberPhotoPicker(onPhoto)
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        BrandButton(
            text = stringResource(R.string.add_item_camera),
            onClick = photoPicker.takePhoto,
            style = BrandButtonStyle.OUTLINE,
            icon = R.drawable.ic_photo_camera,
            enabled = enabled,
            modifier = Modifier.weight(1f)
        )
        BrandButton(
            text = stringResource(R.string.add_item_gallery),
            onClick = photoPicker.pickFromGallery,
            style = BrandButtonStyle.OUTLINE,
            icon = R.drawable.ic_photo_library,
            enabled = enabled,
            modifier = Modifier.weight(1f)
        )
    }
}
