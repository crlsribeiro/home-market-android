package app.carlosribeiro.homemarket.presentation.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.carlosribeiro.homemarket.domain.model.AdminError
import app.carlosribeiro.homemarket.domain.model.AdminResult
import app.carlosribeiro.homemarket.domain.model.PurchaseDetail
import app.carlosribeiro.homemarket.domain.model.PurchaseItem
import app.carlosribeiro.homemarket.domain.usecase.EditPurchaseItemUseCase
import app.carlosribeiro.homemarket.domain.usecase.ObservePurchaseDetailUseCase
import app.carlosribeiro.homemarket.domain.usecase.UploadReceiptUseCase
import app.carlosribeiro.homemarket.domain.util.Money
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The line being edited in the iOS `EditPurchaseItemView` sheet. */
data class PurchaseItemDraft(val item: PurchaseItem, val name: String, val price: String) {
    /** iOS: Save is disabled while the name is blank or the price is not a number. */
    val canSave: Boolean get() = name.isNotBlank() && Money.parsePrice(price) != null
}

data class PurchaseDetailUiState(
    val isLoading: Boolean = true,
    val detail: PurchaseDetail? = null,
    /** The purchase no longer exists, or the user is no longer the admin. */
    val isGone: Boolean = false,
    val draft: PurchaseItemDraft? = null,
    val isUploadingReceipt: Boolean = false,
    val error: AdminError? = null
)

sealed interface PurchaseDetailUiEvent {
    data class EditItem(val item: PurchaseItem) : PurchaseDetailUiEvent

    data class DraftNameChanged(val value: String) : PurchaseDetailUiEvent

    data class DraftPriceChanged(val value: String) : PurchaseDetailUiEvent

    data object CancelEdit : PurchaseDetailUiEvent

    data object SaveEdit : PurchaseDetailUiEvent

    data class ReceiptPicked(val uri: String) : PurchaseDetailUiEvent

    data object DismissError : PurchaseDetailUiEvent
}

/** iOS `PurchaseDetailView`: the total, the receipt lines with an edit button, and the receipt upload. */
@HiltViewModel
class PurchaseDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observePurchaseDetail: ObservePurchaseDetailUseCase,
    private val editPurchaseItem: EditPurchaseItemUseCase,
    private val uploadReceipt: UploadReceiptUseCase
) : ViewModel() {

    private val purchaseId: String = checkNotNull(savedStateHandle[PURCHASE_ID_KEY])
    private val ui = MutableStateFlow(PurchaseDetailUiState())
    private var hasLoaded = false

    val state: StateFlow<PurchaseDetailUiState> = combine(
        observePurchaseDetail(purchaseId).onEach { if (it != null) hasLoaded = true },
        ui
    ) { detail, uiState ->
        uiState.copy(isLoading = detail == null && !hasLoaded, detail = detail, isGone = detail == null && hasLoaded)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), PurchaseDetailUiState())

    fun onEvent(event: PurchaseDetailUiEvent) {
        when (event) {
            is PurchaseDetailUiEvent.EditItem -> ui.update {
                it.copy(draft = PurchaseItemDraft(event.item, event.item.name, priceText(event.item.unitPrice)))
            }

            is PurchaseDetailUiEvent.DraftNameChanged -> ui.update {
                it.copy(draft = it.draft?.copy(name = event.value))
            }

            is PurchaseDetailUiEvent.DraftPriceChanged -> ui.update {
                it.copy(draft = it.draft?.copy(price = event.value))
            }

            PurchaseDetailUiEvent.CancelEdit -> ui.update { it.copy(draft = null) }

            PurchaseDetailUiEvent.SaveEdit -> save()

            is PurchaseDetailUiEvent.ReceiptPicked -> upload(event.uri)

            PurchaseDetailUiEvent.DismissError -> ui.update { it.copy(error = null) }
        }
    }

    /** iOS shows a spinner on the button until the lines are replaced. */
    private fun upload(uri: String) {
        if (ui.value.isUploadingReceipt) return
        ui.update { it.copy(isUploadingReceipt = true) }
        viewModelScope.launch {
            val result = uploadReceipt(purchaseId, uri)
            ui.update { it.copy(isUploadingReceipt = false, error = (result as? AdminResult.Failure)?.error) }
        }
    }

    /** Like the other edits, the sheet closes at once and the listeners bring the new total. */
    private fun save() {
        val draft = ui.value.draft?.takeIf { it.canSave } ?: return
        val price = Money.parsePrice(draft.price) ?: return
        ui.update { it.copy(draft = null) }
        viewModelScope.launch {
            val result = editPurchaseItem(purchaseId, draft.item.id, draft.name, price)
            if (result is AdminResult.Failure) ui.update { it.copy(error = result.error) }
        }
    }

    private fun priceText(price: Double): String = "%.2f".format(Locale.ROOT, price)

    companion object {
        /** Name of the `purchaseId` property of the purchase detail destination. */
        const val PURCHASE_ID_KEY = "purchaseId"
        private const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
