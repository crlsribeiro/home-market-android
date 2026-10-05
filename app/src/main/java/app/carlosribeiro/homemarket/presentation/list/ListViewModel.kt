package app.carlosribeiro.homemarket.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.carlosribeiro.homemarket.domain.model.AdminError
import app.carlosribeiro.homemarket.domain.model.AdminResult
import app.carlosribeiro.homemarket.domain.model.AppUser
import app.carlosribeiro.homemarket.domain.model.ItemError
import app.carlosribeiro.homemarket.domain.model.ItemResult
import app.carlosribeiro.homemarket.domain.model.ItemStatus
import app.carlosribeiro.homemarket.domain.model.ListError
import app.carlosribeiro.homemarket.domain.model.ListItem
import app.carlosribeiro.homemarket.domain.model.ListResult
import app.carlosribeiro.homemarket.domain.model.ListStatus
import app.carlosribeiro.homemarket.domain.model.UserRole
import app.carlosribeiro.homemarket.domain.model.WeekList
import app.carlosribeiro.homemarket.domain.model.WeeklyList
import app.carlosribeiro.homemarket.domain.usecase.CreateWeekListUseCase
import app.carlosribeiro.homemarket.domain.usecase.ExpireStaleListUseCase
import app.carlosribeiro.homemarket.domain.usecase.ObserveCurrentUserUseCase
import app.carlosribeiro.homemarket.domain.usecase.ObserveWeeklyListUseCase
import app.carlosribeiro.homemarket.domain.usecase.RemoveItemUseCase
import app.carlosribeiro.homemarket.domain.usecase.StartShoppingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ListUiState(
    val isLoading: Boolean = true,
    val user: AppUser? = null,
    val currentList: WeekList? = null,
    val items: List<ListItem> = emptyList(),
    val nextWeekItems: List<ListItem> = emptyList(),
    val isCreatingList: Boolean = false,
    val error: ListError? = null
) {
    val isAdmin: Boolean get() = user?.role == UserRole.ADMIN
    val purchasedCount: Int get() = items.count { it.status == ItemStatus.PURCHASED }
    val pendingCount: Int get() = items.count { it.isPendingPurchase }
    val urgentCount: Int get() = items.count { it.isUrgentToBuy }

    /** iOS cart button: admin only, list open or locked, and at least one item. */
    val canStartShopping: Boolean
        get() = isAdmin && items.isNotEmpty() &&
            (currentList?.status == ListStatus.OPEN || currentList?.status == ListStatus.LOCKED)
}

sealed interface ListUiEvent {
    data object CreateList : ListUiEvent

    data object DismissError : ListUiEvent

    data class RemoveItem(val itemId: String) : ListUiEvent

    data object StartShopping : ListUiEvent
}

@HiltViewModel
class ListViewModel @Inject constructor(
    observeCurrentUser: ObserveCurrentUserUseCase,
    observeWeeklyList: ObserveWeeklyListUseCase,
    private val createWeekList: CreateWeekListUseCase,
    private val removeItem: RemoveItemUseCase,
    private val expireStaleList: ExpireStaleListUseCase,
    private val startShopping: StartShoppingUseCase
) : ViewModel() {

    private val action = MutableStateFlow(ActionState())

    /** Lists this screen already tried to expire, so each stale list is cut at most once here. */
    private val expiredListIds = mutableSetOf<String>()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val content: Flow<Pair<AppUser?, WeeklyList>> = observeCurrentUser()
        .distinctUntilChanged()
        .flatMapLatest { user ->
            val householdId = user?.householdId
            if (householdId == null) {
                flowOf(user to WeeklyList(null, emptyList(), emptyList()))
            } else {
                observeWeeklyList(householdId).map { user to it }
            }
        }
        .onEach { (_, weekly) -> weekly.currentList?.let(::expireIfStale) }

    val state: StateFlow<ListUiState> = combine(content, action) { (user, weekly), action ->
        ListUiState(
            isLoading = false,
            user = user,
            currentList = weekly.currentList,
            items = weekly.items,
            nextWeekItems = weekly.nextWeekItems,
            isCreatingList = action.isCreatingList,
            error = action.error
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ListUiState())

    fun onEvent(event: ListUiEvent) {
        when (event) {
            ListUiEvent.CreateList -> createList()
            ListUiEvent.DismissError -> action.update { it.copy(error = null) }
            is ListUiEvent.RemoveItem -> remove(event.itemId)
            ListUiEvent.StartShopping -> start()
        }
    }

    private fun createList() {
        if (action.value.isCreatingList) return
        action.value = ActionState(isCreatingList = true)
        viewModelScope.launch {
            val result = createWeekList()
            action.value = ActionState(error = (result as? ListResult.Failure)?.error)
        }
    }

    /** iOS context-menu "Remove" on the current list: no confirmation step. */
    private fun remove(itemId: String) {
        viewModelScope.launch {
            val result = removeItem(itemId)
            if (result is ItemResult.Failure) {
                val error = if (result.error == ItemError.NETWORK) ListError.NETWORK else ListError.UNKNOWN
                action.update { it.copy(error = error) }
            }
        }
    }

    /** The status change switches the whole app to shopping mode; only a failure needs this screen. */
    private fun start() {
        val current = state.value
        val list = current.currentList ?: return
        viewModelScope.launch {
            val result = startShopping(list, current.items)
            if (result is AdminResult.Failure) {
                val error = if (result.error == AdminError.NETWORK) ListError.NETWORK else ListError.NOT_ADMIN
                action.update { it.copy(error = error) }
            }
        }
    }

    private fun expireIfStale(list: WeekList) {
        if (!expireStaleList.isStale(list) || !expiredListIds.add(list.id)) return
        viewModelScope.launch {
            val result = expireStaleList(list)
            if (result is AdminResult.Failure && result.error == AdminError.NETWORK) {
                // Try again the next time the list is shown with a connection.
                expiredListIds.remove(list.id)
            }
        }
    }

    private data class ActionState(val isCreatingList: Boolean = false, val error: ListError? = null)

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
