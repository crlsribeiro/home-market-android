package app.carlosribeiro.homemarket.presentation.item

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.carlosribeiro.homemarket.R
import app.carlosribeiro.homemarket.domain.model.ListItem
import app.carlosribeiro.homemarket.presentation.list.StatusBadge

/** Name, urgent badge, and the notes with the iOS inline edit (Cancel / Save). */
@Composable
fun NotesSection(item: ListItem, state: ItemDetailUiState, onEvent: (ItemDetailUiEvent) -> Unit) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = item.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            if (item.urgent) {
                StatusBadge(stringResource(R.string.item_badge_urgent), MaterialTheme.colorScheme.error)
            }
        }
        if (state.isEditingNotes) {
            OutlinedTextField(
                value = state.draftNotes,
                onValueChange = { onEvent(ItemDetailUiEvent.DraftNotesChanged(it)) },
                placeholder = { Text(stringResource(R.string.item_detail_notes_hint)) },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { onEvent(ItemDetailUiEvent.CancelNotes) }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.action_cancel))
                }
                Button(onClick = { onEvent(ItemDetailUiEvent.SaveNotes) }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.action_save))
                }
            }
        } else {
            Text(
                text = item.notes.ifEmpty { stringResource(R.string.item_detail_no_notes) },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = { onEvent(ItemDetailUiEvent.EditNotes) }) {
                Icon(painterResource(R.drawable.ic_edit), contentDescription = null)
                Text(stringResource(R.string.item_detail_edit_notes), modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

/** iOS two-step removal: "Remove from list", then Cancel / Confirm removal. */
@Composable
fun RemoveSection(isConfirming: Boolean, onEvent: (ItemDetailUiEvent) -> Unit) {
    val destructive = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.error,
        contentColor = MaterialTheme.colorScheme.onError
    )
    if (isConfirming) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { onEvent(ItemDetailUiEvent.CancelRemove) }, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.action_cancel))
            }
            Button(
                onClick = { onEvent(ItemDetailUiEvent.ConfirmRemove) },
                colors = destructive,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.item_detail_confirm_remove))
            }
        }
    } else {
        OutlinedButton(
            onClick = { onEvent(ItemDetailUiEvent.AskRemove) },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(painterResource(R.drawable.ic_delete), contentDescription = null)
            Text(stringResource(R.string.item_detail_remove), modifier = Modifier.padding(start = 8.dp))
        }
    }
}
