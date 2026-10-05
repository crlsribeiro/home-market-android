package app.carlosribeiro.homemarket.presentation.auth

import android.content.res.Configuration
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.carlosribeiro.homemarket.R
import app.carlosribeiro.homemarket.domain.validation.AuthValidation
import app.carlosribeiro.homemarket.presentation.components.ErrorText
import app.carlosribeiro.homemarket.presentation.components.FormTextField
import app.carlosribeiro.homemarket.presentation.components.SubmitButton
import app.carlosribeiro.homemarket.presentation.components.formKeyboard
import app.carlosribeiro.homemarket.presentation.theme.HomeMarketTheme

@Composable
fun RegisterRoute(onBack: () -> Unit, viewModel: RegisterViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    RegisterScreen(state = state, onEvent = viewModel::onEvent, onBack = onBack)
}

@Composable
fun RegisterScreen(
    state: RegisterUiState,
    onEvent: (RegisterUiEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    AuthFormLayout(modifier = modifier) {
        Text(
            text = stringResource(R.string.auth_create_account),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        RegisterFields(state = state, onEvent = onEvent)
        state.error?.let { ErrorText(stringResource(it.messageRes())) }
        SubmitButton(
            text = stringResource(R.string.auth_create_account),
            isLoading = state.isLoading,
            onClick = { onEvent(RegisterUiEvent.Submit) }
        )
        TextButton(onClick = onBack, enabled = !state.isLoading) {
            Text(stringResource(R.string.auth_have_account))
        }
    }
}

@Composable
private fun RegisterFields(state: RegisterUiState, onEvent: (RegisterUiEvent) -> Unit) {
    val enabled = !state.isLoading
    FormTextField(
        value = state.firstName,
        onValueChange = { onEvent(RegisterUiEvent.FirstNameChanged(it)) },
        label = stringResource(R.string.auth_first_name),
        enabled = enabled,
        keyboardOptions = formKeyboard(capitalization = KeyboardCapitalization.Words)
    )
    FormTextField(
        value = state.lastName,
        onValueChange = { onEvent(RegisterUiEvent.LastNameChanged(it)) },
        label = stringResource(R.string.auth_last_name),
        enabled = enabled,
        keyboardOptions = formKeyboard(capitalization = KeyboardCapitalization.Words)
    )
    FormTextField(
        value = state.email,
        onValueChange = { onEvent(RegisterUiEvent.EmailChanged(it)) },
        label = stringResource(R.string.auth_email),
        enabled = enabled,
        keyboardOptions = formKeyboard(type = KeyboardType.Email)
    )
    FormTextField(
        value = state.password,
        onValueChange = { onEvent(RegisterUiEvent.PasswordChanged(it)) },
        label = stringResource(R.string.auth_password_hint, AuthValidation.MIN_PASSWORD_LENGTH),
        enabled = enabled,
        keyboardOptions = formKeyboard(type = KeyboardType.Password)
    )
    FormTextField(
        value = state.confirmPassword,
        onValueChange = { onEvent(RegisterUiEvent.ConfirmPasswordChanged(it)) },
        label = stringResource(R.string.auth_confirm_password),
        enabled = enabled,
        keyboardOptions = formKeyboard(type = KeyboardType.Password, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onEvent(RegisterUiEvent.Submit) })
    )
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RegisterScreenPreview() {
    HomeMarketTheme(dynamicColor = false) {
        RegisterScreen(state = RegisterUiState(firstName = "Maria"), onEvent = {}, onBack = {})
    }
}
