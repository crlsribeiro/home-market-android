package app.carlosribeiro.homemarket.presentation.list

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.carlosribeiro.homemarket.R
import app.carlosribeiro.homemarket.domain.model.ItemError
import app.carlosribeiro.homemarket.domain.usecase.AddItemUseCase
import app.carlosribeiro.homemarket.presentation.components.FormTextField
import app.carlosribeiro.homemarket.presentation.components.PhotoSourceButtons
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
    val inset = Modifier.padding(horizontal = 16.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.add_item_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = inset
        )
        FormTextField(
            value = state.name,
            onValueChange = { onEvent(AddItemUiEvent.NameChanged(it)) },
            label = stringResource(R.string.add_item_name),
            error = stringResource(R.string.add_item_error_name_required)
                .takeIf { state.error == ItemError.NAME_REQUIRED },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Next
            ),
            modifier = inset.padding(top = 8.dp)
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
            modifier = inset.fillMaxWidth()
        )
        UrgentRow(urgent = state.urgent, onUrgentChange = { onEvent(AddItemUiEvent.UrgentChanged(it)) })
        PhotoSection(
            photoUri = state.photoUri,
            onPhoto = { onEvent(AddItemUiEvent.PhotoPicked(it)) },
            onRemovePhoto = { onEvent(AddItemUiEvent.RemovePhoto) },
            modifier = inset
        )
        SheetActions(
            canSubmit = state.canSubmit,
            onCancel = { onEvent(AddItemUiEvent.Dismiss) },
            onSubmit = { onEvent(AddItemUiEvent.Submit) },
            modifier = inset.padding(top = 8.dp)
        )
    }
}

@Composable
private fun SheetActions(
    canSubmit: Boolean,
    onCancel: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
    ) {
        TextButton(onClick = onCancel) { Text(stringResource(R.string.action_cancel)) }
        Button(onClick = onSubmit, enabled = canSubmit) { Text(stringResource(R.string.add_item_action)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuantityStepper(quantity: Int, onDecrease: () -> Unit, onIncrease: () -> Unit) {
    ListItem(
        headlineContent = { Text(stringResource(R.string.add_item_quantity_value, quantity)) },
        trailingContent = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalIconButton(onClick = onDecrease, enabled = quantity > AddItemUseCase.MIN_QUANTITY) {
                    Icon(
                        painterResource(R.drawable.ic_remove),
                        contentDescription = stringResource(R.string.add_item_decrease)
                    )
                }
                FilledTonalIconButton(onClick = onIncrease, enabled = quantity < AddItemUseCase.MAX_QUANTITY) {
                    Icon(
                        painterResource(R.drawable.ic_add),
                        contentDescription = stringResource(R.string.add_item_increase)
                    )
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = BottomSheetDefaults.ContainerColor)
    )
}

/** The whole row toggles, as Material 3 recommends for a list item with a switch. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UrgentRow(urgent: Boolean, onUrgentChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(stringResource(R.string.add_item_urgent)) },
        trailingContent = { Switch(checked = urgent, onCheckedChange = null) },
        colors = ListItemDefaults.colors(containerColor = BottomSheetDefaults.ContainerColor),
        modifier = Modifier.toggleable(value = urgent, role = Role.Switch, onValueChange = onUrgentChange)
    )
}

@Composable
private fun PhotoSection(
    photoUri: String?,
    onPhoto: (String) -> Unit,
    onRemovePhoto: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (photoUri != null) {
            AsyncImage(
                model = photoUri,
                contentDescription = stringResource(R.string.add_item_photo_preview),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PhotoPreviewHeight)
                    .clip(MaterialTheme.shapes.medium)
            )
            TextButton(onClick = onRemovePhoto) { Text(stringResource(R.string.add_item_remove_photo)) }
        }
        PhotoSourceButtons(onPhoto = onPhoto, enabled = true)
    }
}

private val PhotoPreviewHeight = 160.dp

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddItemFormPreview() {
    HomeMarketTheme(dynamicColor = false) {
        Surface(color = BottomSheetDefaults.ContainerColor) {
            AddItemForm(state = AddItemUiState(isOpen = true, name = "Leite", quantity = 2, urgent = true), onEvent = {
            })
        }
    }
}
