@file:OptIn(ExperimentalMaterial3Api::class)

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
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
import app.carlosribeiro.homemarket.presentation.components.BrandButton
import app.carlosribeiro.homemarket.presentation.components.BrandButtonStyle
import app.carlosribeiro.homemarket.presentation.components.BrandCard
import app.carlosribeiro.homemarket.presentation.components.DetailTopAppBar
import app.carlosribeiro.homemarket.presentation.components.FormTextField
import app.carlosribeiro.homemarket.presentation.components.PhotoSourceMenu
import app.carlosribeiro.homemarket.presentation.components.SectionHeader
import app.carlosribeiro.homemarket.presentation.components.formKeyboard
import app.carlosribeiro.homemarket.presentation.theme.HomeMarketTheme
import app.carlosribeiro.homemarket.presentation.theme.brandColors
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

/** iOS `PurchaseDetailView`: the total card, the items as cards, then the receipt upload button. */
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
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TotalCard(detail = detail, locale = locale)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHeader(stringResource(R.string.history_items))
            ItemsSection(items = detail.items, locale = locale, onEdit = {
                onEvent(PurchaseDetailUiEvent.EditItem(it))
            })
        }
        ReceiptButton(
            isProcessed = detail.purchase.receiptProcessed,
            isUploading = isUploadingReceipt,
            onPicked = { onEvent(PurchaseDetailUiEvent.ReceiptPicked(it)) }
        )
    }
}

/** iOS total card: "Total" and the green amount, with the store below. */
@Composable
private fun TotalCard(detail: PurchaseDetail, locale: Locale) {
    BrandCard(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.semantics(mergeDescendants = true) {},
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.history_total),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = MoneyFormatter.format(detail.purchase.total, locale),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = brandColors.success
            )
        }
        Text(
            text = detail.purchase.storeName ?: stringResource(R.string.history_unknown_store),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ItemsSection(items: List<PurchaseItem>, locale: Locale, onEdit: (PurchaseItem) -> Unit) {
    if (items.isEmpty()) {
        BrandCard {
            Text(
                stringResource(R.string.history_no_items),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    items.forEach { item -> PurchaseItemRow(item = item, locale = locale, onEdit = { onEdit(item) }) }
}

@Composable
private fun PurchaseItemRow(item: PurchaseItem, locale: Locale, onEdit: () -> Unit) {
    BrandCard(contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 4.dp, bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
            Text(
                MoneyFormatter.format(item.totalPrice, locale),
                style = MaterialTheme.typography.bodyLarge,
                color = brandColors.success
            )
            IconButton(onClick = onEdit) {
                Icon(
                    painterResource(R.drawable.ic_edit),
                    contentDescription = stringResource(R.string.history_edit_item),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** iOS: one outline button to upload (or re-upload) the receipt; it offers the camera or the gallery. */
@Composable
private fun ReceiptButton(isProcessed: Boolean, isUploading: Boolean, onPicked: (String) -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    Box {
        BrandButton(
            text = stringResource(
                if (isProcessed) R.string.history_receipt_reupload else R.string.history_receipt_upload
            ),
            onClick = { showMenu = true },
            style = BrandButtonStyle.OUTLINE,
            icon = R.drawable.ic_upload,
            isLoading = isUploading
        )
        PhotoSourceMenu(expanded = showMenu, onDismiss = { showMenu = false }, onPhoto = onPicked)
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
                FormTextField(
                    value = draft.name,
                    onValueChange = { onEvent(PurchaseDetailUiEvent.DraftNameChanged(it)) },
                    label = stringResource(R.string.history_item_name),
                    leadingIcon = R.drawable.ic_shopping_cart,
                    keyboardOptions = formKeyboard(capitalization = KeyboardCapitalization.Sentences)
                )
                FormTextField(
                    value = draft.price,
                    onValueChange = { onEvent(PurchaseDetailUiEvent.DraftPriceChanged(it)) },
                    label = stringResource(R.string.history_unit_price),
                    leadingIcon = R.drawable.ic_receipt_long,
                    error = stringResource(R.string.history_invalid_price).takeIf { priceInvalid },
                    keyboardOptions = formKeyboard(type = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { save() })
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
