package app.carlosribeiro.homemarket.presentation.history

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.carlosribeiro.homemarket.R
import app.carlosribeiro.homemarket.domain.model.Purchase
import app.carlosribeiro.homemarket.presentation.theme.HomeMarketTheme
import java.time.Instant

@Composable
fun HistoryRoute(onOpenPurchase: (String) -> Unit, viewModel: HistoryViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HistoryScreen(state = state, onOpenPurchase = onOpenPurchase)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(state: HistoryUiState, onOpenPurchase: (String) -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.history_title)) }) }
    ) { innerPadding ->
        when {
            state.isLoading -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            state.purchases.isEmpty() -> EmptyHistory(Modifier.padding(innerPadding))

            else -> LazyColumn(
                modifier = Modifier.padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.purchases, key = { it.id }) { purchase ->
                    PurchaseCard(purchase = purchase, onClick = { onOpenPurchase(purchase.id) })
                }
            }
        }
    }
}

@Composable
private fun EmptyHistory(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.history_empty_title), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.history_empty_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun PurchaseCard(purchase: Purchase, onClick: () -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    OutlinedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(purchase.weekText(locale), style = MaterialTheme.typography.titleMedium)
                Text(
                    purchase.storeName ?: stringResource(R.string.history_unknown_store),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                MoneyFormatter.format(purchase.total, locale),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

internal fun previewPurchase(id: String, total: Double, storeName: String?) = Purchase(
    id = id,
    listId = "l-$id",
    householdId = "h1",
    weekLabel = "29 – 5 OUT",
    weekStart = Instant.parse("2026-09-29T03:00:00Z"),
    weekEnd = Instant.parse("2026-10-06T02:59:59Z"),
    total = total,
    receiptUrl = null,
    receiptProcessed = storeName != null,
    createdAt = null,
    storeName = storeName
)

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HistoryPreview() {
    HomeMarketTheme {
        HistoryScreen(
            state = HistoryUiState(
                isLoading = false,
                purchases = listOf(
                    previewPurchase("p1", total = 84.37, storeName = "H-E-B"),
                    previewPurchase("p2", total = 0.0, storeName = null)
                )
            ),
            onOpenPurchase = {}
        )
    }
}

@Preview(name = "Empty light", showBackground = true)
@Preview(name = "Empty dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HistoryEmptyPreview() {
    HomeMarketTheme {
        HistoryScreen(state = HistoryUiState(isLoading = false), onOpenPurchase = {})
    }
}
