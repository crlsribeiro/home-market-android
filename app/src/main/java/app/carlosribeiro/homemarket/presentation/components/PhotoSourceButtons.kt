@file:OptIn(ExperimentalMaterial3Api::class)

package app.carlosribeiro.homemarket.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.carlosribeiro.homemarket.R

/** "Camera" and "Gallery" side by side, for the item photo and the receipt photo. */
@Composable
fun PhotoSourceButtons(onPhoto: (String) -> Unit, enabled: Boolean, modifier: Modifier = Modifier) {
    val photoPicker = rememberPhotoPicker(onPhoto)
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = photoPicker.takePhoto, enabled = enabled, modifier = Modifier.weight(1f)) {
            Icon(painterResource(R.drawable.ic_photo_camera), contentDescription = null)
            Text(stringResource(R.string.add_item_camera), modifier = Modifier.padding(start = 8.dp))
        }
        OutlinedButton(onClick = photoPicker.pickFromGallery, enabled = enabled, modifier = Modifier.weight(1f)) {
            Icon(painterResource(R.drawable.ic_photo_library), contentDescription = null)
            Text(stringResource(R.string.add_item_gallery), modifier = Modifier.padding(start = 8.dp))
        }
    }
}
