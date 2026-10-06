@file:OptIn(ExperimentalMaterial3Api::class)

package app.carlosribeiro.homemarket.presentation.item

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.carlosribeiro.homemarket.R
import app.carlosribeiro.homemarket.domain.model.ListItem
import app.carlosribeiro.homemarket.presentation.components.BrandButton
import app.carlosribeiro.homemarket.presentation.components.BrandButtonStyle
import app.carlosribeiro.homemarket.presentation.components.StatusLabel
import app.carlosribeiro.homemarket.presentation.components.StatusTone
import app.carlosribeiro.homemarket.presentation.components.brandFieldColors

/** Name, urgent badge, and the notes with the iOS inline edit (Cancel / Save). */
@Composable
fun NotesSection(item: ListItem, state: ItemDetailUiState, onEvent: (ItemDetailUiEvent) -> Unit) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            if (item.urgent) {
                StatusLabel(stringResource(R.string.item_badge_urgent), StatusTone.WARNING)
            }
        }
        if (state.isEditingNotes) {
            NotesEditor(draft = state.draftNotes, onEvent = onEvent)
        } else {
            Text(
                text = item.notes.ifEmpty { stringResource(R.string.item_detail_no_notes) },
                style = MaterialTheme.typography.bodyMedium,
                color = if (item.notes.isEmpty()) {
                    MaterialTheme.colorScheme.outline
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            TextButton(
                onClick = { onEvent(ItemDetailUiEvent.EditNotes) },
                contentPadding = PaddingValues(horizontal = 0.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
            ) {
                Icon(painterResource(R.drawable.ic_edit), contentDescription = null, modifier = Modifier.size(16.dp))
                Text(
                    text = stringResource(R.string.item_detail_edit_notes),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun NotesEditor(draft: String, onEvent: (ItemDetailUiEvent) -> Unit) {
    OutlinedTextField(
        value = draft,
        onValueChange = { onEvent(ItemDetailUiEvent.DraftNotesChanged(it)) },
        placeholder = { Text(stringResource(R.string.item_detail_notes_hint)) },
        minLines = 2,
        maxLines = 4,
        shape = MaterialTheme.shapes.medium,
        colors = brandFieldColors(),
        modifier = Modifier
            .fillMaxWidth()
    )
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        BrandButton(
            text = stringResource(R.string.action_cancel),
            onClick = { onEvent(ItemDetailUiEvent.CancelNotes) },
            style = BrandButtonStyle.OUTLINE,
            modifier = Modifier.weight(1f)
        )
        BrandButton(
            text = stringResource(R.string.action_save),
            onClick = { onEvent(ItemDetailUiEvent.SaveNotes) },
            modifier = Modifier.weight(1f)
        )
    }
}

/** iOS two-step removal in place: "Remove from list", then Cancel and "Confirm removal" side by side. */
@Composable
fun RemoveSection(isConfirming: Boolean, onEvent: (ItemDetailUiEvent) -> Unit) {
    if (isConfirming) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BrandButton(
                text = stringResource(R.string.action_cancel),
                onClick = { onEvent(ItemDetailUiEvent.CancelRemove) },
                style = BrandButtonStyle.OUTLINE,
                modifier = Modifier.weight(1f)
            )
            BrandButton(
                text = stringResource(R.string.item_detail_confirm_remove),
                onClick = { onEvent(ItemDetailUiEvent.ConfirmRemove) },
                style = BrandButtonStyle.DESTRUCTIVE,
                modifier = Modifier.weight(1f)
            )
        }
    } else {
        BrandButton(
            text = stringResource(R.string.item_detail_remove),
            onClick = { onEvent(ItemDetailUiEvent.AskRemove) },
            style = BrandButtonStyle.DESTRUCTIVE_OUTLINE
        )
    }
}
