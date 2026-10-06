@file:OptIn(ExperimentalMaterial3Api::class)

package app.carlosribeiro.homemarket.presentation.list

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.carlosribeiro.homemarket.R
import app.carlosribeiro.homemarket.domain.model.ItemError
import app.carlosribeiro.homemarket.domain.usecase.AddItemUseCase
import app.carlosribeiro.homemarket.presentation.components.BrandFieldLabel
import app.carlosribeiro.homemarket.presentation.components.FormTextField
import app.carlosribeiro.homemarket.presentation.components.PhotoSourceButtons
import app.carlosribeiro.homemarket.presentation.components.formKeyboard
import app.carlosribeiro.homemarket.presentation.theme.ControlHeight
import app.carlosribeiro.homemarket.presentation.theme.HomeMarketTheme
import app.carlosribeiro.homemarket.presentation.theme.brandColors
import coil3.compose.AsyncImage

/**
 * iOS `AddItemView`: a full-height sheet with Cancel, the title and Add on top, then the item name,
 * the quantity stepper (1–99), notes, the urgent switch and an optional photo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemSheet(state: AddItemUiState, onEvent: (AddItemUiEvent) -> Unit) {
    ModalBottomSheet(
        onDismissRequest = { onEvent(AddItemUiEvent.Dismiss) },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background
    ) {
        AddItemForm(state = state, onEvent = onEvent)
    }
}

@Composable
fun AddItemForm(state: AddItemUiState, onEvent: (AddItemUiEvent) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        SheetTopBar(
            canSubmit = state.canSubmit,
            onCancel = { onEvent(AddItemUiEvent.Dismiss) },
            onSubmit = { onEvent(AddItemUiEvent.Submit) }
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FormTextField(
                value = state.name,
                onValueChange = { onEvent(AddItemUiEvent.NameChanged(it)) },
                label = stringResource(R.string.add_item_name),
                leadingIcon = R.drawable.ic_shopping_cart,
                required = true,
                error = stringResource(R.string.add_item_error_name_required)
                    .takeIf { state.error == ItemError.NAME_REQUIRED },
                keyboardOptions = formKeyboard(capitalization = KeyboardCapitalization.Sentences)
            )
            QuantityStepper(
                quantity = state.quantity,
                onDecrease = { onEvent(AddItemUiEvent.DecreaseQuantity) },
                onIncrease = { onEvent(AddItemUiEvent.IncreaseQuantity) }
            )
            FormTextField(
                value = state.notes,
                onValueChange = { onEvent(AddItemUiEvent.NotesChanged(it)) },
                label = stringResource(R.string.add_item_notes_label),
                leadingIcon = R.drawable.ic_note,
                placeholder = stringResource(R.string.add_item_notes),
                keyboardOptions = formKeyboard(
                    imeAction = ImeAction.Done,
                    capitalization = KeyboardCapitalization.Sentences
                )
            )
            UrgentRow(urgent = state.urgent, onUrgentChange = { onEvent(AddItemUiEvent.UrgentChanged(it)) })
            PhotoSection(
                photoUri = state.photoUri,
                onPhoto = { onEvent(AddItemUiEvent.PhotoPicked(it)) },
                onRemovePhoto = { onEvent(AddItemUiEvent.RemovePhoto) }
            )
        }
    }
}

/** iOS navigation bar of the sheet: Cancel, the centered title and the bold Add. */
@Composable
private fun SheetTopBar(canSubmit: Boolean, onCancel: () -> Unit, onSubmit: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
    ) {
        TextButton(onClick = onCancel, modifier = Modifier.align(Alignment.CenterStart)) {
            Text(stringResource(R.string.action_cancel))
        }
        Text(
            text = stringResource(R.string.add_item_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .align(Alignment.Center)
                .semantics { heading() }
        )
        TextButton(onClick = onSubmit, enabled = canSubmit, modifier = Modifier.align(Alignment.CenterEnd)) {
            Text(stringResource(R.string.add_item_action), fontWeight = FontWeight.Bold)
        }
    }
}

/** The quantity in a field-shaped box with the minus and plus buttons, like the iOS stepper. */
@Composable
private fun QuantityStepper(quantity: Int, onDecrease: () -> Unit, onIncrease: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        BrandFieldLabel(stringResource(R.string.add_item_quantity))
        FieldBox {
            Text(
                text = stringResource(R.string.add_item_quantity_value, quantity),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
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
    }
}

/** The whole box toggles, as Material 3 recommends for a row with a switch. */
@Composable
private fun UrgentRow(urgent: Boolean, onUrgentChange: (Boolean) -> Unit) {
    FieldBox(modifier = Modifier.toggleable(value = urgent, role = Role.Switch, onValueChange = onUrgentChange)) {
        Text(
            text = stringResource(R.string.add_item_urgent),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Switch(checked = urgent, onCheckedChange = null)
    }
}

/** A white row in the field shape (iOS stepper and toggle boxes). */
@Composable
private fun FieldBox(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = ControlHeight)
            .clip(MaterialTheme.shapes.medium)
            .background(brandColors.card)
            .border(1.dp, brandColors.cardBorder, MaterialTheme.shapes.medium)
            .then(modifier)
            .padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content
    )
}

@Composable
private fun PhotoSection(photoUri: String?, onPhoto: (String) -> Unit, onRemovePhoto: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
            TextButton(
                onClick = onRemovePhoto,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text(stringResource(R.string.add_item_remove_photo))
            }
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
        Surface(color = MaterialTheme.colorScheme.background) {
            AddItemForm(state = AddItemUiState(isOpen = true, name = "Leite", quantity = 2, urgent = true), onEvent = {
            })
        }
    }
}
