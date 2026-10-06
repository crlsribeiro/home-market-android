package app.carlosribeiro.homemarket.presentation.auth

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.carlosribeiro.homemarket.R
import app.carlosribeiro.homemarket.presentation.components.ErrorText
import app.carlosribeiro.homemarket.presentation.components.FormTextField
import app.carlosribeiro.homemarket.presentation.components.formKeyboard
import app.carlosribeiro.homemarket.presentation.theme.HomeMarketTheme

/** iOS `ForgotPasswordView` and the web "Reset password" dialog: email form, then "Email sent". */
@Composable
fun ForgotPasswordDialog(state: ForgotPasswordUiState, onEvent: (ForgotPasswordUiEvent) -> Unit) {
    val dismiss = { onEvent(ForgotPasswordUiEvent.Dismiss) }
    if (state.isSent) {
        AlertDialog(
            onDismissRequest = dismiss,
            title = { Text(stringResource(R.string.forgot_sent_title)) },
            text = { Text(stringResource(R.string.forgot_sent_message, state.email.trim())) },
            confirmButton = { TextButton(onClick = dismiss) { Text(stringResource(R.string.action_close)) } }
        )
        return
    }
    val error = state.error.resolve()
    val send = { onEvent(ForgotPasswordUiEvent.Send) }
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text(stringResource(R.string.forgot_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.forgot_message), style = MaterialTheme.typography.bodyMedium)
                FormTextField(
                    value = state.email,
                    onValueChange = { onEvent(ForgotPasswordUiEvent.EmailChanged(it)) },
                    label = stringResource(R.string.auth_email),
                    leadingIcon = R.drawable.ic_mail,
                    error = error.on(AuthField.EMAIL),
                    enabled = !state.isSending,
                    keyboardOptions = formKeyboard(type = KeyboardType.Email, imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { if (!state.isSending) send() })
                )
                error.general?.let { ErrorText(it) }
            }
        },
        confirmButton = {
            TextButton(onClick = send, enabled = !state.isSending) {
                Text(stringResource(R.string.forgot_send))
            }
        },
        dismissButton = { TextButton(onClick = dismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ForgotPasswordDialogPreview() {
    HomeMarketTheme(dynamicColor = false) {
        ForgotPasswordDialog(state = ForgotPasswordUiState(isOpen = true, email = "maria@example.com"), onEvent = {})
    }
}
