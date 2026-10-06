@file:OptIn(ExperimentalMaterial3Api::class)

package app.carlosribeiro.homemarket.presentation.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import app.carlosribeiro.homemarket.R
import app.carlosribeiro.homemarket.presentation.components.ErrorText

/** iOS `DeleteAccountView`: "Yes, delete my account" or "No, keep my account". */
@Composable
fun DeleteAccountDialog(state: DeleteAccountState, onEvent: (AccountUiEvent) -> Unit) {
    AlertDialog(
        onDismissRequest = { onEvent(AccountUiEvent.CancelDelete) },
        properties = DialogProperties(dismissOnBackPress = !state.isDeleting, dismissOnClickOutside = false),
        icon = {
            Icon(
                painter = painterResource(R.drawable.ic_delete),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text(stringResource(R.string.account_delete_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.account_delete_message))
                state.error?.let { ErrorText(stringResource(it.messageRes())) }
                if (state.isDeleting) CircularProgressIndicator()
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onEvent(AccountUiEvent.ConfirmDelete) },
                enabled = !state.isDeleting,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text(stringResource(R.string.account_delete_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = { onEvent(AccountUiEvent.CancelDelete) }, enabled = !state.isDeleting) {
                Text(stringResource(R.string.account_delete_keep))
            }
        }
    )
}
