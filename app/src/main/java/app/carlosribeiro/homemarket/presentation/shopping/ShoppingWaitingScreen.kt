@file:OptIn(ExperimentalMaterial3Api::class)

package app.carlosribeiro.homemarket.presentation.shopping

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem as MaterialListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.carlosribeiro.homemarket.R
import app.carlosribeiro.homemarket.domain.model.AppUser
import app.carlosribeiro.homemarket.domain.model.UserRole
import app.carlosribeiro.homemarket.presentation.theme.HomeMarketTheme

/**
 * iOS `ShoppingWaitingView`: what members see while the admin shops. It also shows the live progress
 * (CODEX_PLAN M5), which updates as the admin picks items up.
 */
@Composable
fun ShoppingWaitingScreen(state: ShoppingUiState, modifier: Modifier = Modifier) {
    Scaffold(modifier = modifier) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Centered when it fits, scrollable when it does not (small phones, large font scales).
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = if (constraints.hasBoundedHeight) maxHeight else 0.dp)
                    .padding(vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                WaitingHeader(firstName = state.user?.displayName.orEmpty().substringBefore(' '))
                ProgressCard(state, modifier = Modifier.padding(horizontal = 16.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    InfoRow(R.drawable.ic_inventory_2, stringResource(R.string.waiting_info_picking))
                    InfoRow(R.drawable.ic_notifications, stringResource(R.string.waiting_info_notified))
                    InfoRow(R.drawable.ic_check_circle, stringResource(R.string.waiting_info_update))
                }
            }
        }
    }
}

@Composable
private fun WaitingHeader(firstName: String) {
    Column(
        modifier = Modifier.padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(112.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_shopping_cart),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(48.dp)
            )
        }
        Text(
            text = stringResource(R.string.waiting_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.waiting_message, firstName),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun InfoRow(@DrawableRes icon: Int, text: String) {
    MaterialListItem(
        headlineContent = { Text(text) },
        leadingContent = {
            Icon(painterResource(icon), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    )
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ShoppingWaitingPreview() {
    HomeMarketTheme(dynamicColor = false) {
        ShoppingWaitingScreen(
            state = ShoppingUiState(user = AppUser("u2", "João Souza", "joao@example.com", null, "h1", UserRole.MEMBER))
        )
    }
}
