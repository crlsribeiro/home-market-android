@file:OptIn(ExperimentalMaterial3Api::class)

package app.carlosribeiro.homemarket.presentation.history

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.carlosribeiro.homemarket.R
import app.carlosribeiro.homemarket.domain.model.Purchase
import app.carlosribeiro.homemarket.presentation.components.BrandCard
import app.carlosribeiro.homemarket.presentation.components.EmptyState
import app.carlosribeiro.homemarket.presentation.components.TabTopAppBar
import app.carlosribeiro.homemarket.presentation.theme.HomeMarketTheme
import app.carlosribeiro.homemarket.presentation.theme.brandColors
import java.time.Instant

@Composable
fun HistoryRoute(onOpenPurchase: (String) -> Unit, viewModel: HistoryViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HistoryScreen(state = state, onOpenPurchase = onOpenPurchase)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(state: HistoryUiState, onOpenPurchase: (String) -> Unit, modifier: Modifier = Modifier) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { TabTopAppBar(title = stringResource(R.string.history_title), scrollBehavior = scrollBehavior) }
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

            // Scrollable so the large title can still collapse, as in the other tabs.
            state.purchases.isEmpty() -> EmptyState(
                icon = R.drawable.ic_receipt_long,
                title = stringResource(R.string.history_empty_title),
                message = stringResource(R.string.history_empty_message),
                modifier = Modifier
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
            )

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.purchases, key = { it.id }) { purchase ->
                    PurchaseRow(purchase = purchase, onClick = { onOpenPurchase(purchase.id) })
                }
            }
        }
    }
}

/** iOS purchase card: the week and the store, the green total and a chevron. */
@Composable
private fun PurchaseRow(purchase: Purchase, onClick: () -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    BrandCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = purchase.weekText(locale),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = purchase.storeName ?: stringResource(R.string.history_unknown_store),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = MoneyFormatter.format(purchase.total, locale),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = brandColors.success
            )
            Icon(
                painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp)
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
    HomeMarketTheme(dynamicColor = false) {
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
    HomeMarketTheme(dynamicColor = false) {
        HistoryScreen(state = HistoryUiState(isLoading = false), onOpenPurchase = {})
    }
}
