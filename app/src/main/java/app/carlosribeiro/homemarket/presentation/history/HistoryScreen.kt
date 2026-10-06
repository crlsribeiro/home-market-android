package app.carlosribeiro.homemarket.presentation.history

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem as MaterialListItem
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.carlosribeiro.homemarket.R
import app.carlosribeiro.homemarket.domain.model.Purchase
import app.carlosribeiro.homemarket.presentation.components.EmptyState
import app.carlosribeiro.homemarket.presentation.components.TabTopAppBar
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
                modifier = Modifier.fillMaxSize(),
                contentPadding = innerPadding
            ) {
                itemsIndexed(state.purchases, key = { _, purchase -> purchase.id }) { index, purchase ->
                    if (index > 0) HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    PurchaseRow(purchase = purchase, onClick = { onOpenPurchase(purchase.id) })
                }
            }
        }
    }
}

@Composable
private fun PurchaseRow(purchase: Purchase, onClick: () -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    MaterialListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(purchase.weekText(locale)) },
        supportingContent = { Text(purchase.storeName ?: stringResource(R.string.history_unknown_store)) },
        trailingContent = {
            Text(
                MoneyFormatter.format(purchase.total, locale),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    )
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
