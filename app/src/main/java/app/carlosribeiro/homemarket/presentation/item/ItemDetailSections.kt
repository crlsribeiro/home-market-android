@file:OptIn(ExperimentalMaterial3Api::class)

package app.carlosribeiro.homemarket.presentation.item

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.carlosribeiro.homemarket.R
import app.carlosribeiro.homemarket.domain.model.ListItem
import app.carlosribeiro.homemarket.presentation.components.StatusLabel
import app.carlosribeiro.homemarket.presentation.components.StatusTone

/** Name, urgent label, and the notes with the iOS inline edit (Cancel / Save). */
@Composable
fun NotesSection(item: ListItem, state: ItemDetailUiState, onEvent: (ItemDetailUiEvent) -> Unit) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = item.name, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            if (item.urgent) {
                StatusLabel(stringResource(R.string.item_badge_urgent), StatusTone.WARNING)
            }
        }
        if (state.isEditingNotes) {
            NotesEditor(draft = state.draftNotes, onEvent = onEvent)
        } else {
            NotesLabel(onEdit = { onEvent(ItemDetailUiEvent.EditNotes) })
            Text(
                text = item.notes.ifEmpty { stringResource(R.string.item_detail_no_notes) },
                style = MaterialTheme.typography.bodyLarge,
                color = if (item.notes.isEmpty()) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

/** "Description" label with the edit affordance trailing. */
@Composable
private fun NotesLabel(onEdit: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.item_detail_notes_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onEdit) {
            Icon(
                painterResource(R.drawable.ic_edit),
                contentDescription = stringResource(R.string.item_detail_edit_notes)
            )
        }
    }
}

@Composable
private fun NotesEditor(draft: String, onEvent: (ItemDetailUiEvent) -> Unit) {
    OutlinedTextField(
        value = draft,
        onValueChange = { onEvent(ItemDetailUiEvent.DraftNotesChanged(it)) },
        label = { Text(stringResource(R.string.item_detail_notes_label)) },
        placeholder = { Text(stringResource(R.string.item_detail_notes_hint)) },
        minLines = 2,
        maxLines = 4,
        modifier = Modifier.fillMaxWidth()
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
    ) {
        TextButton(onClick = { onEvent(ItemDetailUiEvent.CancelNotes) }) {
            Text(stringResource(R.string.action_cancel))
        }
        Button(onClick = { onEvent(ItemDetailUiEvent.SaveNotes) }) {
            Text(stringResource(R.string.action_save))
        }
    }
}

/** iOS two-step removal: "Remove from list", then a confirmation dialog (Cancel / Confirm removal). */
@Composable
fun RemoveSection(isConfirming: Boolean, onEvent: (ItemDetailUiEvent) -> Unit) {
    val error = MaterialTheme.colorScheme.error
    OutlinedButton(
        onClick = { onEvent(ItemDetailUiEvent.AskRemove) },
        colors = ButtonDefaults.outlinedButtonColors(contentColor = error),
        border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(brush = SolidColor(error)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            painterResource(R.drawable.ic_delete),
            contentDescription = null,
            modifier = Modifier.padding(end = ButtonDefaults.IconSpacing)
        )
        Text(stringResource(R.string.item_detail_remove))
    }
    if (isConfirming) {
        AlertDialog(
            onDismissRequest = { onEvent(ItemDetailUiEvent.CancelRemove) },
            icon = { Icon(painterResource(R.drawable.ic_delete), contentDescription = null) },
            title = { Text(stringResource(R.string.item_detail_remove_dialog_title)) },
            text = { Text(stringResource(R.string.item_detail_remove_dialog_message)) },
            confirmButton = {
                TextButton(
                    onClick = { onEvent(ItemDetailUiEvent.ConfirmRemove) },
                    colors = ButtonDefaults.textButtonColors(contentColor = error)
                ) {
                    Text(stringResource(R.string.item_detail_confirm_remove))
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(ItemDetailUiEvent.CancelRemove) }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}
