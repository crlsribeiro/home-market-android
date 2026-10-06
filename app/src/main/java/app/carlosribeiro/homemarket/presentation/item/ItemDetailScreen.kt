@file:OptIn(ExperimentalMaterial3Api::class)

package app.carlosribeiro.homemarket.presentation.item

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
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
import app.carlosribeiro.homemarket.presentation.components.BrandCard
import app.carlosribeiro.homemarket.presentation.components.BrandFieldLabel
import app.carlosribeiro.homemarket.presentation.components.DetailTopAppBar
import app.carlosribeiro.homemarket.presentation.components.PhotoSourceMenu
import app.carlosribeiro.homemarket.presentation.history.MoneyFormatter
import app.carlosribeiro.homemarket.presentation.list.WeekLabelFormatter
import app.carlosribeiro.homemarket.presentation.list.messageRes
import app.carlosribeiro.homemarket.presentation.theme.HomeMarketTheme
import app.carlosribeiro.homemarket.presentation.theme.brandColors
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
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            DetailTopAppBar(
                title = state.detail?.item?.name.orEmpty(),
                onBack = onBack,
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        val detail = state.detail
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
        if (detail == null) {
            Box(contentModifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            ItemDetailContent(
                detail = detail,
                state = state,
                onEvent = onEvent,
                now = now,
                modifier = contentModifier
            )
        }
    }
}

/**
 * iOS `ItemDetailView`: one card with the photo, the name and notes, and the detail rows, then the
 * two-step remove below it.
 */
@Composable
private fun ItemDetailContent(
    detail: ItemDetail,
    state: ItemDetailUiState,
    onEvent: (ItemDetailUiEvent) -> Unit,
    now: Instant,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BrandCard(contentPadding = PaddingValues(0.dp), verticalArrangement = Arrangement.Top) {
            PhotoHeader(
                item = detail.item,
                pendingPhotoUri = state.pendingPhotoUri,
                isUploading = state.isUploadingPhoto,
                onPhoto = { onEvent(ItemDetailUiEvent.PhotoPicked(it)) }
            )
            CardDivider()
            NotesSection(item = detail.item, state = state, onEvent = onEvent)
            DetailRows(detail = detail, now = now)
        }
        if (detail.canRemove) {
            RemoveSection(isConfirming = state.isConfirmingRemove, onEvent = onEvent)
        }
    }
}

/** The photo on the light tint (or a faded cart); tapping it offers the camera or the gallery. */
@Composable
private fun PhotoHeader(item: ListItem, pendingPhotoUri: String?, isUploading: Boolean, onPhoto: (String) -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    val changePhoto = stringResource(R.string.item_detail_change_photo)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(PhotoHeaderHeight)
            .background(brandColors.tint)
            .clickable(enabled = !isUploading, onClickLabel = changePhoto, role = Role.Button) { showMenu = true },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painterResource(R.drawable.ic_shopping_cart_filled),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
            modifier = Modifier.size(44.dp)
        )
        (pendingPhotoUri ?: item.photoUrl)?.let { model ->
            AsyncImage(
                model = model,
                contentDescription = stringResource(R.string.add_item_photo_preview),
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(10.dp)
        ) {
            Box(Modifier.padding(10.dp), contentAlignment = Alignment.Center) {
                if (isUploading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(
                        painterResource(R.drawable.ic_photo_camera),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        Box(Modifier.align(Alignment.BottomEnd)) {
            PhotoSourceMenu(expanded = showMenu, onDismiss = { showMenu = false }, onPhoto = onPhoto)
        }
    }
}

@Composable
private fun DetailRows(detail: ItemDetail, now: Instant) {
    val locale = LocalConfiguration.current.locales[0]
    CardDivider()
    DetailRow(stringResource(R.string.item_detail_quantity), detail.item.quantity.toString())
    CardDivider()
    DetailRow(stringResource(R.string.item_detail_requested_by), detail.item.addedByName)
    CardDivider()
    DetailRow(stringResource(R.string.item_detail_added), timeAgoText(TimeAgo.between(detail.item.createdAt, now)))
    detail.list?.let { list ->
        CardDivider()
        DetailRow(
            stringResource(R.string.item_detail_week),
            stringResource(R.string.list_week, WeekLabelFormatter.format(list.weekStart, list.weekEnd, locale))
        )
    }
    detail.price?.let { price ->
        CardDivider()
        PriceSection(price = price, locale = locale)
    }
}

/** Admin only, as on iOS: the unit price and the green total, or a hint until a receipt has this item. */
@Composable
private fun PriceSection(price: ItemPrice, locale: Locale) {
    val unitPrice = price.unitPrice
    val total = price.total
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        BrandFieldLabel(stringResource(R.string.item_detail_price))
        if (unitPrice == null || total == null) {
            Text(
                text = stringResource(R.string.item_detail_price_unavailable),
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.outline
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(
                        R.string.item_detail_price_per_unit,
                        MoneyFormatter.format(unitPrice, locale)
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = MoneyFormatter.format(total, locale),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = brandColors.success
                )
            }
        }
    }
}

/** A label with its value trailing (iOS `detailRow`). */
@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun CardDivider() {
    HorizontalDivider(color = brandColors.cardBorder)
}

private val PhotoHeaderHeight = 200.dp

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
