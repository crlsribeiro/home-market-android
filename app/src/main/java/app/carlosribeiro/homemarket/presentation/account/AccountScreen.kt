package app.carlosribeiro.homemarket.presentation.account

import android.content.Intent
import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.carlosribeiro.homemarket.R
import app.carlosribeiro.homemarket.domain.model.AppUser
import app.carlosribeiro.homemarket.domain.model.Household
import app.carlosribeiro.homemarket.domain.model.UserRole
import app.carlosribeiro.homemarket.presentation.components.SignOutDialog
import app.carlosribeiro.homemarket.presentation.theme.HomeMarketTheme

private object AccountLinks {
    /** Published privacy policy, the same page the iOS app links to. */
    const val PRIVACY_POLICY_URL = "https://crlsribeiro.github.io/home-market-privacy/"
}

@Composable
fun AccountRoute(onSignOut: () -> Unit, viewModel: AccountViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    AccountScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onSignOut = onSignOut,
        onOpenPrivacyPolicy = {
            context.startActivity(Intent(Intent.ACTION_VIEW, AccountLinks.PRIVACY_POLICY_URL.toUri()))
        }
    )
}

/** iOS `AccountSettingsView`, the Account tab for every member. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    state: AccountUiState,
    onEvent: (AccountUiEvent) -> Unit,
    onSignOut: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSignOutDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(modifier = modifier, topBar = {
        TopAppBar(title = { Text(stringResource(R.string.account_title)) })
    }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AvatarPicker(state = state, onPhotoPicked = { onEvent(AccountUiEvent.PhotoPicked(it)) })
            state.household?.let { household ->
                Text(
                    text = pluralStringResource(
                        R.plurals.account_household_members,
                        household.memberUids.size,
                        household.name,
                        household.memberUids.size
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            ProfileForm(state = state, onEvent = onEvent)
            FilledTonalButton(onClick = onOpenPrivacyPolicy, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.account_privacy_policy))
            }
            OutlinedButton(onClick = { showSignOutDialog = true }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.auth_sign_out))
            }
        }
    }

    if (showSignOutDialog) {
        SignOutDialog(
            onConfirm = {
                showSignOutDialog = false
                onSignOut()
            },
            onDismiss = { showSignOutDialog = false }
        )
    }
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AccountScreenPreview() {
    HomeMarketTheme(dynamicColor = false) {
        AccountScreen(
            state = AccountUiState(
                user = AppUser("u1", "Maria Silva", "maria@example.com", null, "h1", UserRole.ADMIN),
                household = Household("h1", "Casa Silva", "u1", "ABC12345", listOf("u1", "u2")),
                email = "maria@example.com",
                phone = "(11) 98765-4321"
            ),
            onEvent = {},
            onSignOut = {},
            onOpenPrivacyPolicy = {}
        )
    }
}
