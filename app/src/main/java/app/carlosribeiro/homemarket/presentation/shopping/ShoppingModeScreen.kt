@file:OptIn(ExperimentalMaterial3Api::class)

package app.carlosribeiro.homemarket.presentation.shopping

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import app.carlosribeiro.homemarket.presentation.components.SubmitButton
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
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.shopping_title)) },
                navigationIcon = { AbandonButton(onClick = { showAbandonDialog = true }) },
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = { CloseListBar(isClosing = state.isClosing, onClose = { onEvent(ShoppingUiEvent.CloseList) }) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item { ProgressCard(state, modifier = Modifier.padding(16.dp)) }
            shoppingSections(state, onEvent)
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
                onNotAvailable = { onEvent(ShoppingUiEvent.NotFound(item)) }
            )
        }
    }
    if (state.notFound.isNotEmpty()) {
        item { SectionTitle(R.string.shopping_not_found) }
        items(state.notFound, key = { it.id }) { NotFoundRow(it) }
    }
    if (state.picked.isNotEmpty()) {
        item { SectionTitle(R.string.shopping_picked) }
        items(state.picked, key = { it.id }) { item ->
            PickedRow(item = item, onUndo = { onEvent(ShoppingUiEvent.TogglePurchased(item)) })
        }
    }
    if (state.items.isEmpty()) {
        item {
            Text(
                text = stringResource(R.string.shopping_no_items),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
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

/** "Close list" stays pinned above the navigation bar, reachable however long the list is. */
@Composable
private fun CloseListBar(isClosing: Boolean, onClose: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SubmitButton(text = stringResource(R.string.shopping_close_list), isLoading = isClosing, onClick = onClose)
            Text(
                text = stringResource(R.string.shopping_close_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
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
