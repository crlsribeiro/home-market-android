package app.carlosribeiro.homemarket.presentation.list

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.carlosribeiro.homemarket.R
import app.carlosribeiro.homemarket.domain.model.AppUser
import app.carlosribeiro.homemarket.domain.model.ApprovalStatus
import app.carlosribeiro.homemarket.domain.model.ItemStatus
import app.carlosribeiro.homemarket.domain.model.ListItem
import app.carlosribeiro.homemarket.domain.model.ListStatus
import app.carlosribeiro.homemarket.domain.model.UserRole
import app.carlosribeiro.homemarket.domain.model.WeekList
import app.carlosribeiro.homemarket.presentation.components.SubmitButton
import app.carlosribeiro.homemarket.presentation.theme.HomeMarketTheme
import java.time.Instant

@Composable
fun ListRoute(
    onOpenItem: (String) -> Unit,
    viewModel: ListViewModel = hiltViewModel(),
    addItemViewModel: AddItemViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val addItemState by addItemViewModel.state.collectAsStateWithLifecycle()
    ListScreen(
        state = state,
        onEvent = viewModel::onEvent,
        addItemState = addItemState,
        onAddItemEvent = addItemViewModel::onEvent,
        onOpenItem = onOpenItem
    )
}

/** iOS `MainListView`: the household's current weekly list. */
@Composable
fun ListScreen(
    state: ListUiState,
    onEvent: (ListUiEvent) -> Unit,
    modifier: Modifier = Modifier,
    addItemState: AddItemUiState = AddItemUiState(),
    onAddItemEvent: (AddItemUiEvent) -> Unit = {},
    onOpenItem: (String) -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val errorMessage = state.error?.let { stringResource(it.messageRes()) }
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
            onEvent(ListUiEvent.DismissError)
        }
    }
    val addItemError = addItemState.error?.let { stringResource(it.messageRes()) }
    LaunchedEffect(addItemError) {
        if (addItemError != null) {
            snackbarHostState.showSnackbar(addItemError)
            onAddItemEvent(AddItemUiEvent.DismissError)
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (!state.isLoading) {
                ExtendedFloatingActionButton(
                    onClick = { onAddItemEvent(AddItemUiEvent.Open) },
                    icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
                    text = { Text(stringResource(R.string.add_item_title)) }
                )
            }
        }
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
        when {
            state.isLoading -> Box(contentModifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            state.currentList == null -> NoActiveList(
                isAdmin = state.isAdmin,
                isCreating = state.isCreatingList,
                onCreate = { onEvent(ListUiEvent.CreateList) },
                modifier = contentModifier
            )

            else -> ListContent(
                state = state,
                list = state.currentList,
                onEvent = onEvent,
                onOpenItem = onOpenItem,
                modifier = contentModifier
            )
        }
    }

    if (addItemState.isOpen) {
        AddItemSheet(state = addItemState, onEvent = onAddItemEvent)
    }
}

@Composable
private fun ListContent(
    state: ListUiState,
    list: WeekList,
    onEvent: (ListUiEvent) -> Unit,
    onOpenItem: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = FabClearance),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { ListHeader(user = state.user, list = list) }
        item { SummaryCards(state) }
        if (state.canStartShopping) {
            item {
                FilledTonalButton(
                    onClick = { onEvent(ListUiEvent.StartShopping) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(painterResource(R.drawable.ic_shopping_cart), contentDescription = null)
                    Text(stringResource(R.string.shopping_start), modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
        item { SectionTitle(stringResource(R.string.list_this_week)) }
        if (state.items.isEmpty()) {
            item {
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.list_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(state.items, key = { it.id }) { item ->
                SwipeToRemove(onRemove = { onEvent(ListUiEvent.RemoveItem(item.id)) }) {
                    ListItemRow(item, onClick = { onOpenItem(item.id) })
                }
            }
        }
        if (state.nextWeekItems.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.list_next_week, state.nextWeekItems.size)) }
            items(state.nextWeekItems, key = { "next-${it.id}" }) {
                ListItemRow(it, onClick = { onOpenItem(it.id) })
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun NoActiveList(isAdmin: Boolean, isCreating: Boolean, onCreate: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_shopping_cart),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp)
        )
        Text(text = stringResource(R.string.list_no_active_title), style = MaterialTheme.typography.titleLarge)
        Text(
            text = stringResource(if (isAdmin) R.string.list_no_active_admin else R.string.list_no_active_member),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (isAdmin) {
            SubmitButton(
                text = stringResource(R.string.list_create),
                isLoading = isCreating,
                onClick = onCreate
            )
        }
    }
}

/** Keeps the last item visible above the floating action button. */
private val FabClearance = 88.dp

private val previewUser = AppUser("u1", "Maria Silva", "maria@example.com", null, "h1", UserRole.ADMIN)

private fun previewItem(id: String, name: String, status: ItemStatus, urgent: Boolean = false) = ListItem(
    id = id,
    listId = "h1_2026-09-28",
    householdId = "h1",
    name = name,
    quantity = 2,
    notes = "",
    urgent = urgent,
    addedByUid = "u1",
    addedByName = "Maria",
    status = status,
    approvalStatus = ApprovalStatus.NOT_REQUIRED,
    notFoundResolved = false,
    photoUrl = null,
    createdAt = null
)

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ListScreenPreview() {
    HomeMarketTheme(dynamicColor = false) {
        ListScreen(
            state = ListUiState(
                isLoading = false,
                user = previewUser,
                currentList = WeekList(
                    id = "h1_2026-09-28",
                    householdId = "h1",
                    weekStart = Instant.parse("2026-09-28T03:00:00Z"),
                    weekEnd = Instant.parse("2026-10-05T02:59:59Z"),
                    status = ListStatus.OPEN,
                    createdAt = null
                ),
                items = listOf(
                    previewItem("1", "Leite", ItemStatus.PENDING, urgent = true),
                    previewItem("2", "Pão", ItemStatus.PURCHASED)
                ),
                nextWeekItems = listOf(previewItem("3", "Café", ItemStatus.ROLLED_OVER))
            ),
            onEvent = {}
        )
    }
}

@Preview(name = "No list, light", showBackground = true)
@Preview(name = "No list, dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun NoActiveListPreview() {
    HomeMarketTheme(dynamicColor = false) {
        ListScreen(state = ListUiState(isLoading = false, user = previewUser), onEvent = {})
    }
}
