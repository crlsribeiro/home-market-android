package app.carlosribeiro.homemarket.presentation.history

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem as MaterialListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.carlosribeiro.homemarket.R
import app.carlosribeiro.homemarket.domain.model.PurchaseDetail
import app.carlosribeiro.homemarket.domain.model.PurchaseItem
import app.carlosribeiro.homemarket.domain.util.Money
import app.carlosribeiro.homemarket.presentation.admin.messageRes
import app.carlosribeiro.homemarket.presentation.components.DetailTopAppBar
import app.carlosribeiro.homemarket.presentation.components.PhotoSourceButtons
import app.carlosribeiro.homemarket.presentation.components.SectionHeader
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
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            DetailTopAppBar(
                title = state.detail?.purchase?.weekText(locale).orEmpty(),
                onBack = onBack,
                scrollBehavior = scrollBehavior
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
            PurchaseDetailContent(
                detail = detail,
                isUploadingReceipt = state.isUploadingReceipt,
                locale = locale,
                onEvent = onEvent,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
private fun PurchaseDetailContent(
    detail: PurchaseDetail,
    isUploadingReceipt: Boolean,
    locale: Locale,
    onEvent: (PurchaseDetailUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 16.dp)
    ) {
        TotalCard(detail = detail, locale = locale)
        SectionHeader(stringResource(R.string.history_items))
        ItemsSection(items = detail.items, locale = locale, onEdit = { onEvent(PurchaseDetailUiEvent.EditItem(it)) })
        ReceiptCard(
            isProcessed = detail.purchase.receiptProcessed,
            isUploading = isUploadingReceipt,
            onPicked = { onEvent(PurchaseDetailUiEvent.ReceiptPicked(it)) }
        )
    }
}

/** The purchase total as the screen's hero, with the store below. */
@Composable
private fun TotalCard(detail: PurchaseDetail, locale: Locale) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.history_total), style = MaterialTheme.typography.labelLarge)
            Text(MoneyFormatter.format(detail.purchase.total, locale), style = MaterialTheme.typography.displaySmall)
            Text(
                detail.purchase.storeName ?: stringResource(R.string.history_unknown_store),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun ItemsSection(items: List<PurchaseItem>, locale: Locale, onEdit: (PurchaseItem) -> Unit) {
    if (items.isEmpty()) {
        Text(
            stringResource(R.string.history_no_items),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
    items.forEachIndexed { index, item ->
        if (index > 0) HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        PurchaseItemRow(item = item, locale = locale, onEdit = { onEdit(item) })
    }
}

@Composable
private fun PurchaseItemRow(item: PurchaseItem, locale: Locale, onEdit: () -> Unit) {
    MaterialListItem(
        headlineContent = { Text(item.name) },
        supportingContent = {
            Text(
                stringResource(
                    R.string.history_item_quantity_price,
                    item.quantity,
                    MoneyFormatter.format(item.unitPrice, locale)
                )
            )
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    MoneyFormatter.format(item.totalPrice, locale),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onEdit) {
                    Icon(
                        painterResource(R.drawable.ic_edit),
                        contentDescription = stringResource(R.string.history_edit_item)
                    )
                }
            }
        }
    )
}

@Composable
private fun ReceiptCard(isProcessed: Boolean, isUploading: Boolean, onPicked: (String) -> Unit) {
    OutlinedCard(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painterResource(R.drawable.ic_receipt_long),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 16.dp)
            )
            Text(
                stringResource(if (isProcessed) R.string.history_receipt_reupload else R.string.history_receipt_upload),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            if (isUploading) CircularProgressIndicator(Modifier.size(24.dp))
        }
        PhotoSourceButtons(
            onPhoto = onPicked,
            enabled = !isUploading,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
        )
    }
}

@Composable
private fun EditPurchaseItemDialog(draft: PurchaseItemDraft, onEvent: (PurchaseDetailUiEvent) -> Unit) {
    val priceInvalid = draft.price.isNotBlank() && Money.parsePrice(draft.price) == null
    val save: () -> Unit = { if (draft.canSave) onEvent(PurchaseDetailUiEvent.SaveEdit) }
    AlertDialog(
        onDismissRequest = { onEvent(PurchaseDetailUiEvent.CancelEdit) },
        title = { Text(stringResource(R.string.history_edit_item)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = draft.name,
                    onValueChange = { onEvent(PurchaseDetailUiEvent.DraftNameChanged(it)) },
                    label = { Text(stringResource(R.string.history_item_name)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = draft.price,
                    onValueChange = { onEvent(PurchaseDetailUiEvent.DraftPriceChanged(it)) },
                    label = { Text(stringResource(R.string.history_unit_price)) },
                    singleLine = true,
                    isError = priceInvalid,
                    supportingText = if (priceInvalid) {
                        { Text(stringResource(R.string.history_invalid_price)) }
                    } else {
                        null
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { save() }),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = save, enabled = draft.canSave) {
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
    HomeMarketTheme(dynamicColor = false) {
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
