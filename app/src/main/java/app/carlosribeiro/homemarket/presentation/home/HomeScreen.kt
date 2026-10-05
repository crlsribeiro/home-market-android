package app.carlosribeiro.homemarket.presentation.home

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.carlosribeiro.homemarket.R
import app.carlosribeiro.homemarket.domain.model.AppUser
import app.carlosribeiro.homemarket.domain.model.Household
import app.carlosribeiro.homemarket.domain.model.UserRole
import app.carlosribeiro.homemarket.presentation.theme.HomeMarketTheme

@Composable
fun HomeRoute(user: AppUser, onSignOut: () -> Unit, viewModel: HomeViewModel = hiltViewModel()) {
    val household by viewModel.household.collectAsStateWithLifecycle()
    HomeScreen(user = user, household = household, onSignOut = onSignOut)
}

/** Signed-in start screen. The weekly list (M3) replaces its body. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(user: AppUser, household: Household?, onSignOut: () -> Unit, modifier: Modifier = Modifier) {
    var showSignOutDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(household?.name ?: stringResource(R.string.app_name)) },
                actions = {
                    TextButton(onClick = { showSignOutDialog = true }) {
                        Text(stringResource(R.string.auth_sign_out))
                    }
                }
            )
        }
    ) { innerPadding ->
        HomeContent(
            user = user,
            household = household,
            onSignOutClick = { showSignOutDialog = true },
            modifier = Modifier.padding(innerPadding)
        )
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

@Composable
private fun HomeContent(
    user: AppUser,
    household: Household?,
    onSignOutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.home_greeting, user.displayName.substringBefore(' ')),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.home_signed_in_as, user.email),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        household?.let {
            Text(
                text = pluralStringResource(
                    R.plurals.home_household_members,
                    it.memberUids.size,
                    it.name,
                    it.memberUids.size
                ),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
        Text(
            text = stringResource(R.string.home_next_steps),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp)
        )
        OutlinedButton(onClick = onSignOutClick, modifier = Modifier.padding(top = 24.dp)) {
            Text(stringResource(R.string.auth_sign_out))
        }
    }
}

@Composable
private fun SignOutDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.auth_sign_out_title)) },
        text = { Text(stringResource(R.string.auth_sign_out_message)) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.auth_sign_out)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HomeScreenPreview() {
    HomeMarketTheme(dynamicColor = false) {
        HomeScreen(
            user = AppUser(
                uid = "preview",
                displayName = "Maria Silva",
                email = "maria@example.com",
                photoUrl = null,
                householdId = null,
                role = UserRole.MEMBER
            ),
            household = Household("h1", "Casa Silva", "preview", "ABC12345", listOf("preview", "u2")),
            onSignOut = {}
        )
    }
}
