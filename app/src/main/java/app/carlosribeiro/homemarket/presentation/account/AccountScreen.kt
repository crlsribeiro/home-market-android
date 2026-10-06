@file:OptIn(ExperimentalMaterial3Api::class)

package app.carlosribeiro.homemarket.presentation.account

import android.content.Intent
import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem as MaterialListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
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
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.account_title)) }, scrollBehavior = scrollBehavior)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                ProfileHeader(state = state, onEvent = onEvent)
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    ProfileForm(state = state, onEvent = onEvent)
                }
                HorizontalDivider(modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))
                AccountActions(
                    onOpenPrivacyPolicy = onOpenPrivacyPolicy,
                    onSignOut = { showSignOutDialog = true },
                    onDelete = { onEvent(AccountUiEvent.AskDelete) }
                )
            }
        }
    }

    state.delete?.let { delete -> DeleteAccountDialog(state = delete, onEvent = onEvent) }

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

@Composable
private fun ProfileHeader(state: AccountUiState, onEvent: (AccountUiEvent) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
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
    }
}

/** Privacy policy (opens the browser), sign out, then the destructive delete account. */
@Composable
private fun AccountActions(onOpenPrivacyPolicy: () -> Unit, onSignOut: () -> Unit, onDelete: () -> Unit) {
    MaterialListItem(
        modifier = Modifier.clickable(role = Role.Button, onClick = onOpenPrivacyPolicy),
        headlineContent = { Text(stringResource(R.string.account_privacy_policy)) },
        trailingContent = { Icon(painterResource(R.drawable.ic_open_in_new), contentDescription = null) }
    )
    MaterialListItem(
        modifier = Modifier.clickable(role = Role.Button, onClick = onSignOut),
        headlineContent = { Text(stringResource(R.string.auth_sign_out)) },
        leadingContent = { Icon(painterResource(R.drawable.ic_logout), contentDescription = null) }
    )
    OutlinedButton(
        onClick = onDelete,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
        border = ButtonDefaults.outlinedButtonBorder(enabled = true)
            .copy(brush = SolidColor(MaterialTheme.colorScheme.error)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Text(stringResource(R.string.account_delete))
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
