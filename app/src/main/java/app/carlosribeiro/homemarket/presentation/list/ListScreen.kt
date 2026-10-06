@file:OptIn(ExperimentalMaterial3Api::class)

package app.carlosribeiro.homemarket.presentation.list

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import app.carlosribeiro.homemarket.presentation.components.EmptyState
import app.carlosribeiro.homemarket.presentation.components.SectionHeader
import app.carlosribeiro.homemarket.presentation.components.SubmitButton
import app.carlosribeiro.homemarket.presentation.components.TabTopAppBar
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
@OptIn(ExperimentalMaterial3Api::class)
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
    ErrorSnackbars(snackbarHostState, state, onEvent, addItemState, onAddItemEvent)
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val listState = rememberLazyListState()
    val fabExpanded by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { TabTopAppBar(title = stringResource(R.string.tab_list), scrollBehavior = scrollBehavior) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (!state.isLoading) {
                val addLabel = stringResource(R.string.add_item_title)
                ExtendedFloatingActionButton(
                    text = { Text(addLabel) },
                    icon = {
                        // Collapsed, the icon alone has to name the action.
                        Icon(
                            painterResource(R.drawable.ic_add),
                            contentDescription = if (fabExpanded) null else addLabel
                        )
                    },
                    onClick = { onAddItemEvent(AddItemUiEvent.Open) },
                    expanded = fabExpanded
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
                listState = listState,
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

/** List and add-item failures both show as a snackbar, then are dismissed. */
@Composable
private fun ErrorSnackbars(
    snackbarHostState: SnackbarHostState,
    state: ListUiState,
    onEvent: (ListUiEvent) -> Unit,
    addItemState: AddItemUiState,
    onAddItemEvent: (AddItemUiEvent) -> Unit
) {
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
}

@Composable
private fun ListContent(
    state: ListUiState,
    list: WeekList,
    listState: LazyListState,
    onEvent: (ListUiEvent) -> Unit,
    onOpenItem: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val horizontal = Modifier.padding(horizontal = 16.dp)
    LazyColumn(
        modifier = modifier,
        state = listState,
        contentPadding = PaddingValues(top = 8.dp, bottom = FabClearance)
    ) {
        item {
            Box(horizontal) { ListHeader(user = state.user, list = list) }
        }
        item {
            Box(horizontal.padding(top = 16.dp)) { SummaryCards(state) }
        }
        if (state.canStartShopping) {
            item {
                FilledTonalButton(
                    onClick = { onEvent(ListUiEvent.StartShopping) },
                    modifier = horizontal
                        .padding(top = 12.dp)
                        .fillMaxWidth()
                ) {
                    Icon(painterResource(R.drawable.ic_shopping_cart), contentDescription = null)
                    Text(stringResource(R.string.shopping_start), modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
        thisWeekSection(state.items, onRemove = { onEvent(ListUiEvent.RemoveItem(it)) }, onOpenItem = onOpenItem)
        nextWeekSection(state.nextWeekItems, onOpenItem = onOpenItem)
    }
}

private fun LazyListScope.thisWeekSection(
    items: List<ListItem>,
    onRemove: (String) -> Unit,
    onOpenItem: (String) -> Unit
) {
    item { SectionHeader(stringResource(R.string.list_this_week)) }
    if (items.isEmpty()) {
        item {
            EmptyState(
                icon = R.drawable.ic_shopping_cart,
                title = stringResource(R.string.list_empty_title),
                message = stringResource(R.string.list_empty)
            )
        }
    } else {
        itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
            Column(Modifier.animateItem()) {
                if (index > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                SwipeToRemove(onRemove = { onRemove(item.id) }) {
                    ListItemRow(item, onClick = { onOpenItem(item.id) })
                }
            }
        }
    }
}

private fun LazyListScope.nextWeekSection(items: List<ListItem>, onOpenItem: (String) -> Unit) {
    if (items.isEmpty()) return
    item { SectionHeader(stringResource(R.string.list_next_week, items.size)) }
    itemsIndexed(items, key = { _, item -> "next-${item.id}" }) { index, item ->
        Column(Modifier.animateItem()) {
            if (index > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            ListItemRow(item, onClick = { onOpenItem(item.id) })
        }
    }
}

@Composable
private fun NoActiveList(isAdmin: Boolean, isCreating: Boolean, onCreate: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        EmptyState(
            icon = R.drawable.ic_shopping_cart,
            title = stringResource(R.string.list_no_active_title),
            message = stringResource(if (isAdmin) R.string.list_no_active_admin else R.string.list_no_active_member)
        ) {
            if (isAdmin) {
                SubmitButton(
                    text = stringResource(R.string.list_create),
                    isLoading = isCreating,
                    onClick = onCreate,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
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
                    previewItem("2", "Pão", ItemStatus.PURCHASED).copy(notes = "Integral")
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
