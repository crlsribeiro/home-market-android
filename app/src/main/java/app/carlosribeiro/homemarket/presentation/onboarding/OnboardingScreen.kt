package app.carlosribeiro.homemarket.presentation.onboarding

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.carlosribeiro.homemarket.R
import app.carlosribeiro.homemarket.domain.model.HouseholdError
import app.carlosribeiro.homemarket.presentation.components.ErrorText
import app.carlosribeiro.homemarket.presentation.components.FormTextField
import app.carlosribeiro.homemarket.presentation.components.SubmitButton
import app.carlosribeiro.homemarket.presentation.components.formKeyboard
import app.carlosribeiro.homemarket.presentation.theme.HomeMarketTheme

@Composable
fun OnboardingRoute(viewModel: OnboardingViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    OnboardingScreen(state = state, onEvent = viewModel::onEvent)
}

@Composable
fun OnboardingScreen(state: OnboardingUiState, onEvent: (OnboardingUiEvent) -> Unit, modifier: Modifier = Modifier) {
    Scaffold(modifier = modifier) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.onboarding_welcome),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 32.dp)
            )
            Text(
                text = stringResource(R.string.onboarding_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            when (val mode = state.mode) {
                null -> ModeOptions(onSelect = { onEvent(OnboardingUiEvent.SelectMode(it)) })
                else -> HouseholdForm(mode = mode, state = state, onEvent = onEvent)
            }
        }
    }
}

@Composable
private fun ModeOptions(onSelect: (OnboardingMode) -> Unit) {
    OptionCard(
        title = R.string.onboarding_create_title,
        subtitle = R.string.onboarding_create_subtitle,
        onClick = { onSelect(OnboardingMode.CREATE) }
    )
    OptionCard(
        title = R.string.onboarding_join_title,
        subtitle = R.string.onboarding_join_subtitle,
        onClick = { onSelect(OnboardingMode.JOIN) }
    )
}

@Composable
private fun OptionCard(@StringRes title: Int, @StringRes subtitle: Int, onClick: () -> Unit) {
    OutlinedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HouseholdForm(mode: OnboardingMode, state: OnboardingUiState, onEvent: (OnboardingUiEvent) -> Unit) {
    val isCreate = mode == OnboardingMode.CREATE
    TextButton(onClick = { onEvent(OnboardingUiEvent.Back) }, enabled = !state.isSubmitting) {
        Text(stringResource(R.string.action_back))
    }
    Text(
        text = stringResource(if (isCreate) R.string.onboarding_name_hint else R.string.onboarding_code_hint),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    FormTextField(
        value = state.value,
        onValueChange = { onEvent(OnboardingUiEvent.ValueChanged(it)) },
        label = stringResource(if (isCreate) R.string.onboarding_name_label else R.string.onboarding_code_label),
        enabled = !state.isSubmitting,
        keyboardOptions = formKeyboard(
            imeAction = ImeAction.Done,
            capitalization = if (isCreate) KeyboardCapitalization.Words else KeyboardCapitalization.None
        ),
        keyboardActions = KeyboardActions(onDone = { onEvent(OnboardingUiEvent.Submit) })
    )
    state.error?.let { ErrorText(stringResource(it.messageRes())) }
    SubmitButton(
        text = stringResource(if (isCreate) R.string.onboarding_create_action else R.string.onboarding_join_action),
        isLoading = state.isSubmitting,
        onClick = { onEvent(OnboardingUiEvent.Submit) }
    )
}

@StringRes
private fun HouseholdError.messageRes(): Int = when (this) {
    HouseholdError.NAME_REQUIRED -> R.string.onboarding_error_name_required
    HouseholdError.TOKEN_REQUIRED -> R.string.onboarding_error_code_required
    HouseholdError.TOKEN_NOT_FOUND -> R.string.onboarding_error_code_not_found
    HouseholdError.NETWORK -> R.string.auth_error_network
    HouseholdError.NOT_SIGNED_IN, HouseholdError.NOT_ADMIN, HouseholdError.UNKNOWN -> R.string.auth_error_unknown
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun OnboardingScreenPreview() {
    HomeMarketTheme(dynamicColor = false) {
        OnboardingScreen(state = OnboardingUiState(), onEvent = {})
    }
}

@Preview(name = "Join", showBackground = true)
@Composable
private fun OnboardingJoinPreview() {
    HomeMarketTheme(dynamicColor = false) {
        OnboardingScreen(
            state = OnboardingUiState(mode = OnboardingMode.JOIN, error = HouseholdError.TOKEN_NOT_FOUND),
            onEvent = {}
        )
    }
}
