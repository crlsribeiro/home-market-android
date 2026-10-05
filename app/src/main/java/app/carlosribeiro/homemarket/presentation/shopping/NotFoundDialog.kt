package app.carlosribeiro.homemarket.presentation.shopping

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import app.carlosribeiro.homemarket.R
import app.carlosribeiro.homemarket.domain.model.ListItem

/**
 * Web `NotFoundModal`: asks the member who added a not-found item what to do. Both answers write the
 * same resolution (the item moves to next week), as on the web, so the dialog only closes by choosing.
 */
@Composable
fun NotFoundDialog(item: ListItem, onResolve: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        title = { Text(stringResource(R.string.not_found_title)) },
        text = { Text(stringResource(R.string.not_found_message, item.name)) },
        confirmButton = { TextButton(onClick = onResolve) { Text(stringResource(R.string.not_found_keep)) } },
        dismissButton = { TextButton(onClick = onResolve) { Text(stringResource(R.string.not_found_discard)) } }
    )
}
