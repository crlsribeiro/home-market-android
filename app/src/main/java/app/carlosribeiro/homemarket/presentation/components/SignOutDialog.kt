package app.carlosribeiro.homemarket.presentation.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.carlosribeiro.homemarket.R

@Composable
fun SignOutDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.auth_sign_out_title)) },
        text = { Text(stringResource(R.string.auth_sign_out_message)) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.auth_sign_out)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}
