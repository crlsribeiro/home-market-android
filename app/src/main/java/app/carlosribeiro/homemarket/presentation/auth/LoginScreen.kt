@file:OptIn(ExperimentalMaterial3Api::class)

package app.carlosribeiro.homemarket.presentation.auth

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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
fun LoginRoute(
    onCreateAccount: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
    forgotPasswordViewModel: ForgotPasswordViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val forgotState by forgotPasswordViewModel.state.collectAsStateWithLifecycle()
    val googleSignIn = rememberGoogleSignIn { viewModel.onEvent(LoginUiEvent.GoogleResult(it)) }
    LoginScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onCreateAccount = onCreateAccount,
        onGoogleSignIn = googleSignIn,
        onForgotPassword = { forgotPasswordViewModel.onEvent(ForgotPasswordUiEvent.Open(state.email)) }
    )
    if (forgotState.isOpen) {
        ForgotPasswordDialog(state = forgotState, onEvent = forgotPasswordViewModel::onEvent)
    }
}

@Composable
fun LoginScreen(
    state: LoginUiState,
    onEvent: (LoginUiEvent) -> Unit,
    onCreateAccount: () -> Unit,
    modifier: Modifier = Modifier,
    onGoogleSignIn: () -> Unit = {},
    onForgotPassword: () -> Unit = {}
) {
    val error = state.error.resolve()
    AuthFormLayout(modifier = modifier) {
        LoginHeader()
        LoginFields(state = state, error = error, onEvent = onEvent, onForgotPassword = onForgotPassword)
        error.general?.let { ErrorText(it, modifier = Modifier.fillMaxWidth()) }
        SubmitButton(
            text = stringResource(R.string.auth_sign_in),
            isLoading = state.isLoading,
            onClick = { onEvent(LoginUiEvent.Submit) }
        )
        GoogleSignInSection(enabled = !state.isLoading, onClick = onGoogleSignIn)
        FilledTonalButton(
            onClick = onCreateAccount,
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.auth_create_account))
        }
    }
}

/** The app's cart mark, its name and the tagline. */
@Composable
private fun LoginHeader() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_shopping_cart),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(56.dp)
        )
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = stringResource(R.string.app_tagline),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun LoginFields(
    state: LoginUiState,
    error: ResolvedAuthError,
    onEvent: (LoginUiEvent) -> Unit,
    onForgotPassword: () -> Unit
) {
    FormTextField(
        value = state.email,
        onValueChange = { onEvent(LoginUiEvent.EmailChanged(it)) },
        label = stringResource(R.string.auth_email),
        leadingIcon = R.drawable.ic_mail,
        error = error.on(AuthField.EMAIL),
        enabled = !state.isLoading,
        keyboardOptions = formKeyboard(type = KeyboardType.Email)
    )
    Column(modifier = Modifier.fillMaxWidth()) {
        FormTextField(
            value = state.password,
            onValueChange = { onEvent(LoginUiEvent.PasswordChanged(it)) },
            label = stringResource(R.string.auth_password),
            leadingIcon = R.drawable.ic_lock,
            error = error.on(AuthField.PASSWORD),
            enabled = !state.isLoading,
            keyboardOptions = formKeyboard(type = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onEvent(LoginUiEvent.Submit) })
        )
        TextButton(
            onClick = onForgotPassword,
            enabled = !state.isLoading,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text(stringResource(R.string.forgot_link))
        }
    }
}

@Composable
private fun GoogleSignInSection(enabled: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f))
        Text(
            text = stringResource(R.string.auth_or),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        HorizontalDivider(modifier = Modifier.weight(1f))
    }
    OutlinedButton(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.auth_continue_with_google))
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
