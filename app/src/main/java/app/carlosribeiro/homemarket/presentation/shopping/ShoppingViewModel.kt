package app.carlosribeiro.homemarket.presentation.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.carlosribeiro.homemarket.domain.model.AdminError
import app.carlosribeiro.homemarket.domain.model.AdminResult
import app.carlosribeiro.homemarket.domain.model.AppUser
import app.carlosribeiro.homemarket.domain.model.ItemStatus
import app.carlosribeiro.homemarket.domain.model.ListItem
import app.carlosribeiro.homemarket.domain.model.ListStatus
import app.carlosribeiro.homemarket.domain.model.UserRole
import app.carlosribeiro.homemarket.domain.model.WeekList
import app.carlosribeiro.homemarket.domain.model.WeeklyList
import app.carlosribeiro.homemarket.domain.usecase.ObserveCurrentUserUseCase
import app.carlosribeiro.homemarket.domain.usecase.ObserveWeeklyListUseCase
import app.carlosribeiro.homemarket.domain.usecase.ResolveNotFoundUseCase
import app.carlosribeiro.homemarket.domain.usecase.ShoppingActions
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ShoppingUiState(
    val user: AppUser? = null,
    val currentList: WeekList? = null,
    val items: List<ListItem> = emptyList(),
    val isClosing: Boolean = false,
    val error: AdminError? = null
) {
    /** iOS full-screen takeover: the whole app shows shopping while the list status is `shopping`. */
    val isShopping: Boolean get() = currentList?.status == ListStatus.SHOPPING
    val isAdmin: Boolean get() = user?.role == UserRole.ADMIN

    val toGet: List<ListItem> get() = items.filter { it.isPendingPurchase }
    val notFound: List<ListItem> get() = items.filter { it.status == ItemStatus.NOT_FOUND }
    val picked: List<ListItem> get() = items.filter { it.status == ItemStatus.PURCHASED }

    /** The first of the user's own not-found items that still needs their decision (web `NotFoundModal`). */
    val notFoundDecision: ListItem? get() = user?.uid?.let { uid ->
        items.firstOrNull { it.awaitsNotFoundDecisionBy(uid) }
    }

    /** iOS progress: purchased items over every item of the list. */
    val progress: Float get() = if (items.isEmpty()) 0f else picked.size.toFloat() / items.size
}

sealed interface ShoppingUiEvent {
    data class TogglePurchased(val item: ListItem) : ShoppingUiEvent

    data class NotFound(val item: ListItem) : ShoppingUiEvent

    data object CloseList : ShoppingUiEvent

    data object Abandon : ShoppingUiEvent

    data object DismissError : ShoppingUiEvent

    data class ResolveNotFound(val item: ListItem) : ShoppingUiEvent
}

@HiltViewModel
class ShoppingViewModel @Inject constructor(
    observeCurrentUser: ObserveCurrentUserUseCase,
    observeWeeklyList: ObserveWeeklyListUseCase,
    private val actions: ShoppingActions,
    private val resolveNotFound: ResolveNotFoundUseCase
) : ViewModel() {

    private val action = MutableStateFlow(ActionState())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val content = observeCurrentUser()
        .distinctUntilChanged()
        .flatMapLatest { user ->
            val householdId = user?.householdId
            if (householdId == null) {
                flowOf(user to WeeklyList(null, emptyList(), emptyList()))
            } else {
                observeWeeklyList(householdId).map { user to it }
            }
        }

    val state: StateFlow<ShoppingUiState> = combine(content, action) { (user, weekly), action ->
        ShoppingUiState(
            user = user,
            currentList = weekly.currentList,
            items = weekly.items,
            isClosing = action.isClosing,
            error = action.error
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ShoppingUiState())

    fun onEvent(event: ShoppingUiEvent) {
        val list = state.value.currentList
        when (event) {
            is ShoppingUiEvent.TogglePurchased -> launchAction { actions.togglePurchased(event.item) }
            is ShoppingUiEvent.NotFound -> launchAction { actions.markNotFound(event.item) }
            ShoppingUiEvent.CloseList -> list?.let(::close)
            ShoppingUiEvent.Abandon -> list?.let { launchAction { actions.abandon(it) } }
            ShoppingUiEvent.DismissError -> action.update { it.copy(error = null) }
            is ShoppingUiEvent.ResolveNotFound -> viewModelScope.launch { resolveNotFound(event.item) }
        }
    }

    private fun close(list: WeekList) {
        if (action.value.isClosing) return
        action.update { it.copy(isClosing = true) }
        viewModelScope.launch {
            val result = actions.closeList(list)
            action.update { it.copy(isClosing = false, error = (result as? AdminResult.Failure)?.error) }
        }
    }

    private fun launchAction(call: suspend () -> AdminResult) {
        viewModelScope.launch {
            val result = call()
            if (result is AdminResult.Failure) action.update { it.copy(error = result.error) }
        }
    }

    private data class ActionState(val isClosing: Boolean = false, val error: AdminError? = null)

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
