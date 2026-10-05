package app.carlosribeiro.homemarket.presentation.account

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.carlosribeiro.homemarket.R
import app.carlosribeiro.homemarket.domain.model.AppUser
import app.carlosribeiro.homemarket.domain.model.Household
import app.carlosribeiro.homemarket.domain.model.UserRole
import app.carlosribeiro.homemarket.presentation.components.SignOutDialog
import app.carlosribeiro.homemarket.presentation.theme.HomeMarketTheme

@Composable
fun AccountRoute(user: AppUser, onSignOut: () -> Unit, viewModel: AccountViewModel = hiltViewModel()) {
    val household by viewModel.household.collectAsStateWithLifecycle()
    AccountScreen(user = user, household = household, onSignOut = onSignOut)
}

/** Account tab: name, email, household and sign-out. Milestone M8 adds the iOS account settings. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(user: AppUser, household: Household?, onSignOut: () -> Unit, modifier: Modifier = Modifier) {
    var showSignOutDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(modifier = modifier, topBar = {
        TopAppBar(title = { Text(stringResource(R.string.account_title)) })
    }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = user.displayName, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = user.email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    household?.let {
                        Text(
                            text = pluralStringResource(
                                R.plurals.account_household_members,
                                it.memberUids.size,
                                it.name,
                                it.memberUids.size
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
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
            user = AppUser("u1", "Maria Silva", "maria@example.com", null, "h1", UserRole.ADMIN),
            household = Household("h1", "Casa Silva", "u1", "ABC12345", listOf("u1", "u2")),
            onSignOut = {}
        )
    }
}
