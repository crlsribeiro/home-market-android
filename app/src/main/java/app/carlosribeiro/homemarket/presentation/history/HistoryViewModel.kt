package app.carlosribeiro.homemarket.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.carlosribeiro.homemarket.domain.model.Purchase
import app.carlosribeiro.homemarket.domain.usecase.ObservePurchaseHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class HistoryUiState(val isLoading: Boolean = true, val purchases: List<Purchase> = emptyList())

/** iOS `HistoryView`: the household's purchases, newest first. */
@HiltViewModel
class HistoryViewModel @Inject constructor(observePurchaseHistory: ObservePurchaseHistoryUseCase) : ViewModel() {
    val state: StateFlow<HistoryUiState> = observePurchaseHistory()
        .map { HistoryUiState(isLoading = false, purchases = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), HistoryUiState())

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
