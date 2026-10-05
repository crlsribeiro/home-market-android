package app.carlosribeiro.homemarket.presentation.auth

import android.content.res.Configuration
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.carlosribeiro.homemarket.R
import app.carlosribeiro.homemarket.presentation.components.ErrorText
import app.carlosribeiro.homemarket.presentation.components.FormTextField
import app.carlosribeiro.homemarket.presentation.components.SubmitButton
import app.carlosribeiro.homemarket.presentation.components.formKeyboard
import app.carlosribeiro.homemarket.presentation.theme.HomeMarketTheme

@Composable
fun LoginRoute(onCreateAccount: () -> Unit, viewModel: LoginViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val googleSignIn = rememberGoogleSignIn { viewModel.onEvent(LoginUiEvent.GoogleResult(it)) }
    LoginScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onCreateAccount = onCreateAccount,
        onGoogleSignIn = googleSignIn
    )
}

@Composable
fun LoginScreen(
    state: LoginUiState,
    onEvent: (LoginUiEvent) -> Unit,
    onCreateAccount: () -> Unit,
    modifier: Modifier = Modifier,
    onGoogleSignIn: () -> Unit = {}
) {
    AuthFormLayout(modifier = modifier) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = stringResource(R.string.app_tagline),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        FormTextField(
            value = state.email,
            onValueChange = { onEvent(LoginUiEvent.EmailChanged(it)) },
            label = stringResource(R.string.auth_email),
            enabled = !state.isLoading,
            keyboardOptions = formKeyboard(type = KeyboardType.Email)
        )
        FormTextField(
            value = state.password,
            onValueChange = { onEvent(LoginUiEvent.PasswordChanged(it)) },
            label = stringResource(R.string.auth_password),
            enabled = !state.isLoading,
            keyboardOptions = formKeyboard(type = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onEvent(LoginUiEvent.Submit) })
        )
        state.error?.let { ErrorText(stringResource(it.messageRes())) }
        SubmitButton(
            text = stringResource(R.string.auth_sign_in),
            isLoading = state.isLoading,
            onClick = { onEvent(LoginUiEvent.Submit) }
        )
        Text(
            text = stringResource(R.string.auth_or),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedButton(
            onClick = onGoogleSignIn,
            enabled = !state.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(stringResource(R.string.auth_continue_with_google))
        }
        TextButton(onClick = onCreateAccount, enabled = !state.isLoading) {
            Text(stringResource(R.string.auth_create_account))
        }
    }
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LoginScreenPreview() {
    HomeMarketTheme(dynamicColor = false) {
        LoginScreen(state = LoginUiState(email = "maria@example.com"), onEvent = {}, onCreateAccount = {})
    }
}
