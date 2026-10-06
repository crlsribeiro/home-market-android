@file:OptIn(ExperimentalMaterial3Api::class)

package app.carlosribeiro.homemarket.presentation.onboarding

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.carlosribeiro.homemarket.R
import app.carlosribeiro.homemarket.domain.model.HouseholdError
import app.carlosribeiro.homemarket.presentation.components.AppMark
import app.carlosribeiro.homemarket.presentation.components.BrandCard
import app.carlosribeiro.homemarket.presentation.components.ErrorText
import app.carlosribeiro.homemarket.presentation.components.FormTextField
import app.carlosribeiro.homemarket.presentation.components.SubmitButton
import app.carlosribeiro.homemarket.presentation.components.formKeyboard
import app.carlosribeiro.homemarket.presentation.theme.HomeMarketTheme
import app.carlosribeiro.homemarket.presentation.theme.brandColors

/** Widest the content gets on tablets and landscape. */
private val MaxContentWidth = 480.dp

@Composable
fun OnboardingRoute(viewModel: OnboardingViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    OnboardingScreen(state = state, onEvent = viewModel::onEvent)
}

/**
 * iOS `OnboardingView`: the app mark and the welcome, then either the two option cards or the create /
 * join form in a card with a back link.
 */
@Composable
fun OnboardingScreen(state: OnboardingUiState, onEvent: (OnboardingUiEvent) -> Unit, modifier: Modifier = Modifier) {
    val mode = state.mode
    Scaffold(modifier = modifier) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = MaxContentWidth)
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                WelcomeHeader()
                when (mode) {
                    null -> ModeOptions(onSelect = { onEvent(OnboardingUiEvent.SelectMode(it)) })
                    else -> HouseholdForm(mode = mode, state = state, onEvent = onEvent)
                }
            }
        }
    }
}

@Composable
private fun WelcomeHeader() {
    Column(modifier = Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            AppMark(icon = R.drawable.ic_shopping_cart_filled, size = 36.dp)
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        Text(
            text = stringResource(R.string.onboarding_welcome),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { heading() }
        )
        Text(
            text = stringResource(R.string.onboarding_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ModeOptions(onSelect: (OnboardingMode) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OptionCard(
            icon = R.drawable.ic_home_filled,
            title = OnboardingMode.CREATE.titleRes,
            subtitle = R.string.onboarding_create_subtitle,
            onClick = { onSelect(OnboardingMode.CREATE) }
        )
        OptionCard(
            icon = R.drawable.ic_group_filled,
            title = OnboardingMode.JOIN.titleRes,
            subtitle = R.string.onboarding_join_subtitle,
            onClick = { onSelect(OnboardingMode.JOIN) }
        )
    }
}

@Composable
private fun OptionCard(@DrawableRes icon: Int, @StringRes title: Int, @StringRes subtitle: Int, onClick: () -> Unit) {
    BrandCard(onClick = onClick) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(brandColors.tint, MaterialTheme.shapes.medium),
                contentAlignment = Alignment.Center
            ) {
                Icon(painterResource(icon), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    stringResource(subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun HouseholdForm(mode: OnboardingMode, state: OnboardingUiState, onEvent: (OnboardingUiEvent) -> Unit) {
    val isCreate = mode == OnboardingMode.CREATE
    val error = state.error
    val errorMessage = error?.let { stringResource(it.messageRes()) }
    val label = stringResource(if (isCreate) R.string.onboarding_name_label else R.string.onboarding_code_label)
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TextButton(
            onClick = { onEvent(OnboardingUiEvent.Back) },
            enabled = !state.isSubmitting,
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = null, modifier = Modifier.size(18.dp))
            Text(stringResource(R.string.action_back), modifier = Modifier.padding(start = 4.dp))
        }
        BrandCard(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    text = stringResource(
                        if (isCreate) R.string.onboarding_name_hint else R.string.onboarding_code_hint
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FormTextField(
                value = state.value,
                onValueChange = { onEvent(OnboardingUiEvent.ValueChanged(it)) },
                label = label,
                showLabel = false,
                leadingIcon = if (isCreate) R.drawable.ic_home_filled else R.drawable.ic_link,
                placeholder = stringResource(
                    if (isCreate) R.string.onboarding_name_placeholder else R.string.onboarding_code_placeholder
                ),
                error = errorMessage?.takeIf { error.isAboutTheField },
                enabled = !state.isSubmitting,
                keyboardOptions = formKeyboard(
                    imeAction = ImeAction.Done,
                    capitalization = if (isCreate) KeyboardCapitalization.Words else KeyboardCapitalization.None
                ),
                keyboardActions = KeyboardActions(onDone = { onEvent(OnboardingUiEvent.Submit) })
            )
            errorMessage?.takeUnless { error.isAboutTheField }?.let { ErrorText(it) }
            SubmitButton(
                text = stringResource(
                    if (isCreate) R.string.onboarding_create_action else R.string.onboarding_join_action
                ),
                isLoading = state.isSubmitting,
                onClick = { onEvent(OnboardingUiEvent.Submit) },
                enabled = state.value.isNotBlank()
            )
        }
    }
}

@get:StringRes
private val OnboardingMode.titleRes: Int
    get() = when (this) {
        OnboardingMode.CREATE -> R.string.onboarding_create_title
        OnboardingMode.JOIN -> R.string.onboarding_join_title
    }

/** Whether the error is about the household name or invite code typed in the field. */
private val HouseholdError?.isAboutTheField: Boolean
    get() = when (this) {
        HouseholdError.NAME_REQUIRED, HouseholdError.TOKEN_REQUIRED, HouseholdError.TOKEN_NOT_FOUND -> true
        else -> false
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
