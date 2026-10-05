package app.carlosribeiro.homemarket.presentation.history

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.carlosribeiro.homemarket.R
import app.carlosribeiro.homemarket.domain.model.PurchaseDetail
import app.carlosribeiro.homemarket.domain.model.PurchaseItem
import app.carlosribeiro.homemarket.presentation.admin.messageRes
import app.carlosribeiro.homemarket.presentation.theme.HomeMarketTheme
import java.util.Locale

@Composable
fun PurchaseDetailRoute(onBack: () -> Unit, viewModel: PurchaseDetailViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.isGone) {
        if (state.isGone) onBack()
    }
    PurchaseDetailScreen(state = state, onEvent = viewModel::onEvent, onBack = onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseDetailScreen(
    state: PurchaseDetailUiState,
    onEvent: (PurchaseDetailUiEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val locale = LocalConfiguration.current.locales[0]
    val snackbarHostState = remember { SnackbarHostState() }
    val errorMessage = state.error?.let { stringResource(it.messageRes()) }
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
            onEvent(PurchaseDetailUiEvent.DismissError)
        }
    }
    state.draft?.let { EditPurchaseItemDialog(draft = it, onEvent = onEvent) }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(state.detail?.purchase?.weekText(locale).orEmpty()) },
                navigationIcon = { TextButton(onClick = onBack) { Text(stringResource(R.string.action_back)) } }
            )
        }
    ) { innerPadding ->
        val detail = state.detail
        if (detail == null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TotalCard(detail = detail, locale = locale)
                ItemsCard(items = detail.items, locale = locale, onEdit = {
                    onEvent(PurchaseDetailUiEvent.EditItem(it))
                })
            }
        }
    }
}

@Composable
private fun TotalCard(detail: PurchaseDetail, locale: Locale) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.history_total), style = MaterialTheme.typography.titleMedium)
                Text(
                    detail.purchase.storeName ?: stringResource(R.string.history_unknown_store),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                MoneyFormatter.format(detail.purchase.total, locale),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun ItemsCard(items: List<PurchaseItem>, locale: Locale, onEdit: (PurchaseItem) -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.history_items),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp)
        )
        if (items.isEmpty()) {
            Text(
                stringResource(R.string.history_no_items),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            )
        }
        items.forEachIndexed { index, item ->
            if (index > 0) HorizontalDivider()
            PurchaseItemRow(item = item, locale = locale, onEdit = { onEdit(item) })
        }
    }
}

@Composable
private fun PurchaseItemRow(item: PurchaseItem, locale: Locale, onEdit: () -> Unit) {
    Row(
        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(item.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(
                    R.string.history_item_quantity_price,
                    item.quantity,
                    MoneyFormatter.format(item.unitPrice, locale)
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(MoneyFormatter.format(item.totalPrice, locale), style = MaterialTheme.typography.bodyLarge)
        IconButton(onClick = onEdit) {
            Icon(painterResource(R.drawable.ic_edit), contentDescription = stringResource(R.string.history_edit_item))
        }
    }
}

@Composable
private fun EditPurchaseItemDialog(draft: PurchaseItemDraft, onEvent: (PurchaseDetailUiEvent) -> Unit) {
    AlertDialog(
        onDismissRequest = { onEvent(PurchaseDetailUiEvent.CancelEdit) },
        title = { Text(stringResource(R.string.history_edit_item)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = draft.name,
                    onValueChange = { onEvent(PurchaseDetailUiEvent.DraftNameChanged(it)) },
                    label = { Text(stringResource(R.string.history_item_name)) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = draft.price,
                    onValueChange = { onEvent(PurchaseDetailUiEvent.DraftPriceChanged(it)) },
                    label = { Text(stringResource(R.string.history_unit_price)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onEvent(PurchaseDetailUiEvent.SaveEdit) }, enabled = draft.canSave) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = { onEvent(PurchaseDetailUiEvent.CancelEdit) }) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PurchaseDetailPreview() {
    HomeMarketTheme {
        PurchaseDetailScreen(
            state = PurchaseDetailUiState(
                isLoading = false,
                detail = PurchaseDetail(
                    purchase = previewPurchase("p1", total = 7.48, storeName = "H-E-B"),
                    items = listOf(
                        PurchaseItem("a", "p1", "HEB MILK 2%", quantity = 1, unitPrice = 3.49, totalPrice = 3.49),
                        PurchaseItem("b", "p1", "BANANAS", quantity = 1, unitPrice = 3.99, totalPrice = 3.99)
                    )
                )
            ),
            onEvent = {},
            onBack = {}
        )
    }
}
