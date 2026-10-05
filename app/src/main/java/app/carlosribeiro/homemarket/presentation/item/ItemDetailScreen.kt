package app.carlosribeiro.homemarket.presentation.item

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.carlosribeiro.homemarket.R
import app.carlosribeiro.homemarket.domain.model.ApprovalStatus
import app.carlosribeiro.homemarket.domain.model.ItemDetail
import app.carlosribeiro.homemarket.domain.model.ItemPrice
import app.carlosribeiro.homemarket.domain.model.ItemStatus
import app.carlosribeiro.homemarket.domain.model.ListItem
import app.carlosribeiro.homemarket.domain.model.ListStatus
import app.carlosribeiro.homemarket.domain.model.WeekList
import app.carlosribeiro.homemarket.presentation.components.rememberPhotoPicker
import app.carlosribeiro.homemarket.presentation.history.MoneyFormatter
import app.carlosribeiro.homemarket.presentation.list.WeekLabelFormatter
import app.carlosribeiro.homemarket.presentation.list.messageRes
import app.carlosribeiro.homemarket.presentation.theme.HomeMarketTheme
import coil3.compose.AsyncImage
import java.time.Duration
import java.time.Instant
import java.util.Locale

@Composable
fun ItemDetailRoute(onBack: () -> Unit, viewModel: ItemDetailViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.isGone) {
        if (state.isGone) onBack()
    }
    ItemDetailScreen(state = state, onEvent = viewModel::onEvent, onBack = onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailScreen(
    state: ItemDetailUiState,
    onEvent: (ItemDetailUiEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    now: Instant = Instant.now()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val errorMessage = state.error?.let { stringResource(it.messageRes()) }
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
            onEvent(ItemDetailUiEvent.DismissError)
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(state.detail?.item?.name.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
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
                OutlinedCard(Modifier.fillMaxWidth()) {
                    PhotoSection(
                        item = detail.item,
                        pendingPhotoUri = state.pendingPhotoUri,
                        isUploading = state.isUploadingPhoto,
                        onPhotoPicked = { onEvent(ItemDetailUiEvent.PhotoPicked(it)) }
                    )
                    HorizontalDivider()
                    NotesSection(item = detail.item, state = state, onEvent = onEvent)
                    HorizontalDivider()
                    DetailRows(detail = detail, now = now)
                }
                if (detail.canRemove) {
                    RemoveSection(isConfirming = state.isConfirmingRemove, onEvent = onEvent)
                }
            }
        }
    }
}

@Composable
private fun PhotoSection(
    item: ListItem,
    pendingPhotoUri: String?,
    isUploading: Boolean,
    onPhotoPicked: (String) -> Unit
) {
    val photoPicker = rememberPhotoPicker(onPhotoPicked)
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = item.name.take(1).uppercase(),
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            (pendingPhotoUri ?: item.photoUrl)?.let { model ->
                AsyncImage(
                    model = model,
                    contentDescription = stringResource(R.string.add_item_photo_preview),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }
            if (isUploading) {
                CircularProgressIndicator(modifier = Modifier.size(40.dp))
            }
        }
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(onClick = photoPicker.takePhoto, enabled = !isUploading, modifier = Modifier.weight(1f)) {
                Icon(painterResource(R.drawable.ic_photo_camera), contentDescription = null)
                Text(stringResource(R.string.add_item_camera), modifier = Modifier.padding(start = 8.dp))
            }
            OutlinedButton(
                onClick = photoPicker.pickFromGallery,
                enabled = !isUploading,
                modifier = Modifier.weight(1f)
            ) {
                Icon(painterResource(R.drawable.ic_photo_library), contentDescription = null)
                Text(stringResource(R.string.add_item_gallery), modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun DetailRows(detail: ItemDetail, now: Instant) {
    val locale = LocalConfiguration.current.locales[0]
    DetailRow(stringResource(R.string.item_detail_quantity), detail.item.quantity.toString())
    HorizontalDivider()
    DetailRow(stringResource(R.string.item_detail_requested_by), detail.item.addedByName)
    HorizontalDivider()
    DetailRow(stringResource(R.string.item_detail_added), timeAgoText(TimeAgo.between(detail.item.createdAt, now)))
    detail.list?.let { list ->
        HorizontalDivider()
        DetailRow(
            stringResource(R.string.item_detail_week),
            stringResource(R.string.list_week, WeekLabelFormatter.format(list.weekStart, list.weekEnd, locale))
        )
    }
    detail.price?.let { price ->
        HorizontalDivider()
        PriceRows(price = price, locale = locale)
    }
}

/** Admin only: the receipt price, or a hint until a receipt with this item is uploaded. */
@Composable
private fun PriceRows(price: ItemPrice, locale: Locale) {
    val unitPrice = price.unitPrice
    val total = price.total
    if (unitPrice == null || total == null) {
        DetailRow(stringResource(R.string.item_detail_price), stringResource(R.string.item_detail_price_unavailable))
    } else {
        DetailRow(
            stringResource(R.string.item_detail_price),
            stringResource(R.string.item_detail_price_per_unit, MoneyFormatter.format(unitPrice, locale))
        )
        HorizontalDivider()
        DetailRow(stringResource(R.string.item_detail_price_total), MoneyFormatter.format(total, locale))
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun timeAgoText(timeAgo: TimeAgo): String = when (timeAgo) {
    TimeAgo.Unknown -> "—"
    TimeAgo.Now -> stringResource(R.string.time_ago_now)
    is TimeAgo.Minutes -> stringResource(R.string.time_ago_minutes, timeAgo.value)
    is TimeAgo.Hours -> stringResource(R.string.time_ago_hours, timeAgo.value)
    is TimeAgo.Days -> stringResource(R.string.time_ago_days, timeAgo.value)
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ItemDetailPreview() {
    val now = Instant.parse("2026-10-01T15:00:00Z")
    HomeMarketTheme(dynamicColor = false) {
        ItemDetailScreen(
            state = ItemDetailUiState(
                isLoading = false,
                detail = ItemDetail(
                    item = ListItem(
                        "i1", "l1", "h1", "Azeite", 2, "Extra virgem", true, "u1", "Ana",
                        ItemStatus.PENDING, ApprovalStatus.NOT_REQUIRED, false, null, now.minus(Duration.ofHours(2))
                    ),
                    list = WeekList(
                        "l1",
                        "h1",
                        Instant.parse("2026-09-28T03:00:00Z"),
                        Instant.parse("2026-10-05T02:59:59Z"),
                        ListStatus.OPEN,
                        null
                    ),
                    price = ItemPrice(unitPrice = 8.99, quantity = 2)
                )
            ),
            onEvent = {},
            onBack = {},
            now = now
        )
    }
}
