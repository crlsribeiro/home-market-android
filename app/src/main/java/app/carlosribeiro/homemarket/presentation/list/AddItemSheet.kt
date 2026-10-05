package app.carlosribeiro.homemarket.presentation.list

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.carlosribeiro.homemarket.R
import app.carlosribeiro.homemarket.domain.usecase.AddItemUseCase
import app.carlosribeiro.homemarket.presentation.components.rememberPhotoPicker
import app.carlosribeiro.homemarket.presentation.theme.HomeMarketTheme
import coil3.compose.AsyncImage

/** iOS `AddItemView`: name, quantity stepper (1–99), notes, urgent toggle and optional photo. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemSheet(state: AddItemUiState, onEvent: (AddItemUiEvent) -> Unit) {
    ModalBottomSheet(
        onDismissRequest = { onEvent(AddItemUiEvent.Dismiss) },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        AddItemForm(state = state, onEvent = onEvent)
    }
}

@Composable
fun AddItemForm(state: AddItemUiState, onEvent: (AddItemUiEvent) -> Unit, modifier: Modifier = Modifier) {
    val photoPicker = rememberPhotoPicker { onEvent(AddItemUiEvent.PhotoPicked(it)) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .navigationBarsPadding()
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = stringResource(R.string.add_item_title), style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(
            value = state.name,
            onValueChange = { onEvent(AddItemUiEvent.NameChanged(it)) },
            label = { Text(stringResource(R.string.add_item_name)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth()
        )
        QuantityStepper(
            quantity = state.quantity,
            onDecrease = { onEvent(AddItemUiEvent.DecreaseQuantity) },
            onIncrease = { onEvent(AddItemUiEvent.IncreaseQuantity) }
        )
        OutlinedTextField(
            value = state.notes,
            onValueChange = { onEvent(AddItemUiEvent.NotesChanged(it)) },
            label = { Text(stringResource(R.string.add_item_notes)) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth()
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.add_item_urgent),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Switch(checked = state.urgent, onCheckedChange = { onEvent(AddItemUiEvent.UrgentChanged(it)) })
        }
        PhotoSection(
            photoUri = state.photoUri,
            onTakePhoto = photoPicker.takePhoto,
            onPickPhoto = photoPicker.pickFromGallery,
            onRemovePhoto = { onEvent(AddItemUiEvent.RemovePhoto) }
        )
        SheetActions(
            canSubmit = state.canSubmit,
            onCancel = { onEvent(AddItemUiEvent.Dismiss) },
            onSubmit = { onEvent(AddItemUiEvent.Submit) }
        )
    }
}

@Composable
private fun SheetActions(canSubmit: Boolean, onCancel: () -> Unit, onSubmit: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.action_cancel))
        }
        Button(onClick = onSubmit, enabled = canSubmit, modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.add_item_action))
        }
    }
}

@Composable
private fun QuantityStepper(quantity: Int, onDecrease: () -> Unit, onIncrease: () -> Unit) {
    val description = stringResource(R.string.add_item_quantity_value, quantity)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f)
                .semantics { contentDescription = description }
        )
        FilledTonalIconButton(onClick = onDecrease, enabled = quantity > AddItemUseCase.MIN_QUANTITY) {
            Icon(painterResource(R.drawable.ic_remove), contentDescription = stringResource(R.string.add_item_decrease))
        }
        FilledTonalIconButton(onClick = onIncrease, enabled = quantity < AddItemUseCase.MAX_QUANTITY) {
            Icon(painterResource(R.drawable.ic_add), contentDescription = stringResource(R.string.add_item_increase))
        }
    }
}

@Composable
private fun PhotoSection(
    photoUri: String?,
    onTakePhoto: () -> Unit,
    onPickPhoto: () -> Unit,
    onRemovePhoto: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (photoUri != null) {
            Surface(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                AsyncImage(
                    model = photoUri,
                    contentDescription = stringResource(R.string.add_item_photo_preview),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            }
            TextButton(onClick = onRemovePhoto) { Text(stringResource(R.string.add_item_remove_photo)) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onTakePhoto, modifier = Modifier.weight(1f)) {
                Icon(painterResource(R.drawable.ic_photo_camera), contentDescription = null)
                Text(stringResource(R.string.add_item_camera), modifier = Modifier.padding(start = 8.dp))
            }
            OutlinedButton(onClick = onPickPhoto, modifier = Modifier.weight(1f)) {
                Icon(painterResource(R.drawable.ic_photo_library), contentDescription = null)
                Text(stringResource(R.string.add_item_gallery), modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AddItemFormPreview() {
    HomeMarketTheme(dynamicColor = false) {
        Surface {
            AddItemForm(state = AddItemUiState(isOpen = true, name = "Leite", quantity = 2, urgent = true), onEvent = {
            })
        }
    }
}
