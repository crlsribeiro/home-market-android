@file:OptIn(ExperimentalMaterial3Api::class)

package app.carlosribeiro.homemarket.presentation.shopping

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.carlosribeiro.homemarket.R
import app.carlosribeiro.homemarket.domain.model.ApprovalStatus
import app.carlosribeiro.homemarket.domain.model.ItemStatus
import app.carlosribeiro.homemarket.domain.model.ListItem
import app.carlosribeiro.homemarket.domain.model.ListStatus
import app.carlosribeiro.homemarket.domain.model.WeekList
import app.carlosribeiro.homemarket.presentation.admin.messageRes
import app.carlosribeiro.homemarket.presentation.components.BrandButton
import app.carlosribeiro.homemarket.presentation.components.BrandButtonStyle
import app.carlosribeiro.homemarket.presentation.components.InlineTabTopAppBar
import app.carlosribeiro.homemarket.presentation.theme.HomeMarketTheme
import java.time.Instant

/** iOS `ShoppingModeView`: the admin's full-screen checklist while the list is `shopping`. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingModeScreen(state: ShoppingUiState, onEvent: (ShoppingUiEvent) -> Unit, modifier: Modifier = Modifier) {
    KeepScreenOn()
    var showAbandonDialog by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val errorMessage = state.error?.let { stringResource(it.messageRes()) }
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
            onEvent(ShoppingUiEvent.DismissError)
        }
    }
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            InlineTabTopAppBar(
                title = stringResource(R.string.shopping_title),
                navigationIcon = { AbandonButton(onClick = { showAbandonDialog = true }) }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { ProgressCard(state) }
            shoppingSections(state, onEvent)
            item {
                CloseListSection(
                    isClosing = state.isClosing,
                    onClose = { onEvent(ShoppingUiEvent.CloseList) },
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }

    if (showAbandonDialog) {
        AbandonDialog(
            onConfirm = {
                showAbandonDialog = false
                onEvent(ShoppingUiEvent.Abandon)
            },
            onDismiss = { showAbandonDialog = false }
        )
    }
}

private fun LazyListScope.shoppingSections(state: ShoppingUiState, onEvent: (ShoppingUiEvent) -> Unit) {
    if (state.toGet.isNotEmpty()) {
        item { SectionTitle(R.string.shopping_to_get) }
        items(state.toGet, key = { it.id }) { item ->
            ToGetRow(
                item = item,
                onGotIt = { onEvent(ShoppingUiEvent.TogglePurchased(item)) },
                onNotAvailable = { onEvent(ShoppingUiEvent.NotFound(item)) },
                modifier = Modifier.animateItem()
            )
        }
    }
    if (state.notFound.isNotEmpty()) {
        item { SectionTitle(R.string.shopping_not_found) }
        items(state.notFound, key = { it.id }) { NotFoundRow(it, modifier = Modifier.animateItem()) }
    }
    if (state.picked.isNotEmpty()) {
        item { SectionTitle(R.string.shopping_picked) }
        items(state.picked, key = { it.id }) { item ->
            PickedRow(
                item = item,
                onUndo = { onEvent(ShoppingUiEvent.TogglePurchased(item)) },
                modifier = Modifier.animateItem()
            )
        }
    }
    if (state.items.isEmpty()) {
        item {
            Text(
                text = stringResource(R.string.shopping_no_items),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp)
            )
        }
    }
}

/**
 * Close icon that opens the abandon confirmation. Its label is exposed as the node's text, so screen
 * readers announce "Abandon" and tests can find it by that text.
 */
@Composable
private fun AbandonButton(onClick: () -> Unit) {
    val label = stringResource(R.string.shopping_abandon)
    IconButton(onClick = onClick, modifier = Modifier.semantics { text = AnnotatedString(label) }) {
        Icon(painterResource(R.drawable.ic_close), contentDescription = null)
    }
}

/** iOS: the red "Close list" at the end of the list, with what happens to the pending items. */
@Composable
private fun CloseListSection(isClosing: Boolean, onClose: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BrandButton(
            text = stringResource(R.string.shopping_close_list),
            onClick = onClose,
            style = BrandButtonStyle.DESTRUCTIVE,
            isLoading = isClosing
        )
        Text(
            text = stringResource(R.string.shopping_close_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** The phone stays awake while the admin shops. */
@Composable
private fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}

@Composable
private fun AbandonDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.shopping_abandon_title)) },
        text = { Text(stringResource(R.string.shopping_abandon_message)) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text(stringResource(R.string.shopping_abandon_confirm))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.shopping_keep_shopping)) } }
    )
}

private fun previewItem(id: String, name: String, status: ItemStatus) = ListItem(
    id, "l1", "h1", name, 2, "", id == "1", "u1", "Maria", status, ApprovalStatus.NOT_REQUIRED, false, null, null
)

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ShoppingModePreview() {
    HomeMarketTheme(dynamicColor = false) {
        ShoppingModeScreen(
            state = ShoppingUiState(
                currentList = WeekList(
                    "l1",
                    "h1",
                    Instant.parse("2026-09-28T03:00:00Z"),
                    Instant.parse("2026-10-05T02:59:59Z"),
                    ListStatus.SHOPPING,
                    null
                ),
                items = listOf(
                    previewItem("1", "Leite", ItemStatus.PENDING),
                    previewItem("2", "Pão", ItemStatus.PURCHASED),
                    previewItem("3", "Café", ItemStatus.NOT_FOUND)
                )
            ),
            onEvent = {}
        )
    }
}
