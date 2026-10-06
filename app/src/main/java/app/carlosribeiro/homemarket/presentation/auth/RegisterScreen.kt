@file:OptIn(ExperimentalMaterial3Api::class)

package app.carlosribeiro.homemarket.presentation.auth

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.carlosribeiro.homemarket.R
import app.carlosribeiro.homemarket.domain.validation.AuthValidation
import app.carlosribeiro.homemarket.presentation.components.ErrorText
import app.carlosribeiro.homemarket.presentation.components.FormTextField
import app.carlosribeiro.homemarket.presentation.components.PhoneField
import app.carlosribeiro.homemarket.presentation.components.SubmitButton
import app.carlosribeiro.homemarket.presentation.components.formKeyboard
import app.carlosribeiro.homemarket.presentation.theme.HomeMarketTheme

@Composable
fun RegisterRoute(onBack: () -> Unit, viewModel: RegisterViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    RegisterScreen(state = state, onEvent = viewModel::onEvent, onBack = onBack)
}

/**
 * iOS `RegisterView`: the brand-green header with the back button, the title and the optional photo,
 * then the form on the page background.
 */
@Composable
fun RegisterScreen(
    state: RegisterUiState,
    onEvent: (RegisterUiEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val error = state.error.resolve()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        RegisterHeader(
            photoUri = state.photoUri,
            enabled = !state.isLoading,
            onBack = onBack,
            onPhotoPicked = { onEvent(RegisterUiEvent.PhotoPicked(it)) },
            onRemovePhoto = { onEvent(RegisterUiEvent.RemovePhoto) }
        )
        Column(
            modifier = Modifier
                .widthIn(max = MaxFormWidth)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            RegisterNameFields(state = state, error = error, onEvent = onEvent)
            RegisterCredentialFields(state = state, error = error, onEvent = onEvent)
            PhoneField(
                phone = state.phone,
                country = state.country,
                onPhoneChange = { onEvent(RegisterUiEvent.PhoneChanged(it)) },
                onCountryChange = { onEvent(RegisterUiEvent.CountryChanged(it)) },
                label = stringResource(R.string.register_phone_optional)
            )
            error.general?.let { ErrorText(it, modifier = Modifier.fillMaxWidth()) }
            SubmitButton(
                text = stringResource(R.string.auth_create_account),
                isLoading = state.isLoading,
                onClick = { onEvent(RegisterUiEvent.Submit) },
                modifier = Modifier.padding(top = 4.dp)
            )
            LegalLinks()
        }
    }
}

@Composable
private fun RegisterNameFields(state: RegisterUiState, error: ResolvedAuthError, onEvent: (RegisterUiEvent) -> Unit) {
    // "Name required" covers both names: flag the first one that is still blank.
    val nameError = error.on(AuthField.NAME)
    val firstNameMissing = state.firstName.isBlank()
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        FormTextField(
            value = state.firstName,
            onValueChange = { onEvent(RegisterUiEvent.FirstNameChanged(it)) },
            label = stringResource(R.string.auth_first_name),
            leadingIcon = R.drawable.ic_person,
            required = true,
            error = nameError?.takeIf { firstNameMissing },
            enabled = !state.isLoading,
            keyboardOptions = formKeyboard(capitalization = KeyboardCapitalization.Words),
            modifier = Modifier.weight(1f)
        )
        FormTextField(
            value = state.lastName,
            onValueChange = { onEvent(RegisterUiEvent.LastNameChanged(it)) },
            label = stringResource(R.string.auth_last_name),
            leadingIcon = R.drawable.ic_person,
            required = true,
            error = nameError?.takeUnless { firstNameMissing },
            enabled = !state.isLoading,
            keyboardOptions = formKeyboard(capitalization = KeyboardCapitalization.Words),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun RegisterCredentialFields(
    state: RegisterUiState,
    error: ResolvedAuthError,
    onEvent: (RegisterUiEvent) -> Unit
) {
    FormTextField(
        value = state.email,
        onValueChange = { onEvent(RegisterUiEvent.EmailChanged(it)) },
        label = stringResource(R.string.auth_email),
        leadingIcon = R.drawable.ic_mail,
        placeholder = stringResource(R.string.auth_email_placeholder),
        required = true,
        error = error.on(AuthField.EMAIL),
        enabled = !state.isLoading,
        keyboardOptions = formKeyboard(type = KeyboardType.Email)
    )
    FormTextField(
        value = state.password,
        onValueChange = { onEvent(RegisterUiEvent.PasswordChanged(it)) },
        label = stringResource(R.string.auth_password),
        leadingIcon = R.drawable.ic_lock,
        placeholder = stringResource(R.string.auth_password_requirement, AuthValidation.MIN_PASSWORD_LENGTH),
        required = true,
        error = error.on(AuthField.PASSWORD),
        enabled = !state.isLoading,
        keyboardOptions = formKeyboard(type = KeyboardType.Password)
    )
    FormTextField(
        value = state.confirmPassword,
        onValueChange = { onEvent(RegisterUiEvent.ConfirmPasswordChanged(it)) },
        label = stringResource(R.string.auth_confirm_password),
        leadingIcon = R.drawable.ic_lock,
        placeholder = stringResource(R.string.auth_confirm_password_placeholder),
        required = true,
        error = error.on(AuthField.CONFIRM_PASSWORD),
        enabled = !state.isLoading,
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
