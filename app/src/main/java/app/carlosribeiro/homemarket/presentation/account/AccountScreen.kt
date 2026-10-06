@file:OptIn(ExperimentalMaterial3Api::class)

package app.carlosribeiro.homemarket.presentation.account

import android.content.Intent
import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import app.carlosribeiro.homemarket.presentation.components.BrandButton
import app.carlosribeiro.homemarket.presentation.components.BrandButtonStyle
import app.carlosribeiro.homemarket.presentation.components.InlineTabTopAppBar
import app.carlosribeiro.homemarket.presentation.theme.HomeMarketTheme

private object AccountLinks {
    /** Published privacy policy, the same page the iOS app links to. */
    const val PRIVACY_POLICY_URL = "https://crlsribeiro.github.io/home-market-privacy/"
}

@Composable
fun AccountRoute(viewModel: AccountViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    AccountScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onOpenPrivacyPolicy = {
            context.startActivity(Intent(Intent.ACTION_VIEW, AccountLinks.PRIVACY_POLICY_URL.toUri()))
        }
    )
}

/**
 * iOS `AccountSettingsView`, the Account tab for every member: the photo, the profile form, then the
 * privacy policy and the destructive delete account. Signing out is on the List tab, as on iOS.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    state: AccountUiState,
    onEvent: (AccountUiEvent) -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = { InlineTabTopAppBar(title = stringResource(R.string.account_title)) }
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
                    .padding(horizontal = 20.dp)
                    .padding(top = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AvatarPicker(state = state, onPhotoPicked = { onEvent(AccountUiEvent.PhotoPicked(it)) })
                ProfileForm(state = state, onEvent = onEvent)
                BrandButton(
                    text = stringResource(R.string.account_privacy_policy),
                    onClick = onOpenPrivacyPolicy,
                    style = BrandButtonStyle.TINT
                )
                BrandButton(
                    text = stringResource(R.string.account_delete),
                    onClick = { onEvent(AccountUiEvent.AskDelete) },
                    style = BrandButtonStyle.DESTRUCTIVE
                )
            }
        }
    }

    state.delete?.let { delete -> DeleteAccountDialog(state = delete, onEvent = onEvent) }
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
            onOpenPrivacyPolicy = {}
        )
    }
}
