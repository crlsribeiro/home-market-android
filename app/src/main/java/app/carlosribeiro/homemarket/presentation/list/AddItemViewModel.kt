package app.carlosribeiro.homemarket.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.carlosribeiro.homemarket.domain.model.ItemError
import app.carlosribeiro.homemarket.domain.model.ItemResult
import app.carlosribeiro.homemarket.domain.usecase.AddItemUseCase
import app.carlosribeiro.homemarket.domain.usecase.ItemInput
import app.carlosribeiro.homemarket.domain.util.PhotoCompressor
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddItemUiState(
    val isOpen: Boolean = false,
    val name: String = "",
    val quantity: Int = AddItemUseCase.MIN_QUANTITY,
    val notes: String = "",
    val urgent: Boolean = false,
    val photoUri: String? = null,
    val error: ItemError? = null
) {
    val canSubmit: Boolean get() = name.isNotBlank()
}

sealed interface AddItemUiEvent {
    data object Open : AddItemUiEvent

    data object Dismiss : AddItemUiEvent

    data class NameChanged(val value: String) : AddItemUiEvent

    data object IncreaseQuantity : AddItemUiEvent

    data object DecreaseQuantity : AddItemUiEvent

    data class NotesChanged(val value: String) : AddItemUiEvent

    data class UrgentChanged(val value: Boolean) : AddItemUiEvent

    data class PhotoPicked(val uri: String) : AddItemUiEvent

    data object RemovePhoto : AddItemUiEvent

    data object Submit : AddItemUiEvent

    data object DismissError : AddItemUiEvent
}

/**
 * The add-item sheet. Like iOS, the sheet closes as soon as the user taps Add and the write goes on
 * in the background, so adding works offline; a failure shows on the list screen.
 */
@HiltViewModel
class AddItemViewModel @Inject constructor(
    private val addItem: AddItemUseCase,
    private val photoCompressor: PhotoCompressor
) : ViewModel() {

    private val mutableState = MutableStateFlow(AddItemUiState())
    val state: StateFlow<AddItemUiState> = mutableState.asStateFlow()

    fun onEvent(event: AddItemUiEvent) {
        when (event) {
            AddItemUiEvent.Open -> mutableState.update { AddItemUiState(isOpen = true, error = it.error) }
            AddItemUiEvent.Dismiss -> mutableState.update { it.copy(isOpen = false) }
            is AddItemUiEvent.NameChanged -> mutableState.update { it.copy(name = event.value) }
            AddItemUiEvent.IncreaseQuantity -> changeQuantity(1)
            AddItemUiEvent.DecreaseQuantity -> changeQuantity(-1)
            is AddItemUiEvent.NotesChanged -> mutableState.update { it.copy(notes = event.value) }
            is AddItemUiEvent.UrgentChanged -> mutableState.update { it.copy(urgent = event.value) }
            is AddItemUiEvent.PhotoPicked -> mutableState.update { it.copy(photoUri = event.uri) }
            AddItemUiEvent.RemovePhoto -> mutableState.update { it.copy(photoUri = null) }
            AddItemUiEvent.Submit -> submit()
            AddItemUiEvent.DismissError -> mutableState.update { it.copy(error = null) }
        }
    }

    private fun changeQuantity(delta: Int) = mutableState.update {
        it.copy(quantity = (it.quantity + delta).coerceIn(AddItemUseCase.MIN_QUANTITY, AddItemUseCase.MAX_QUANTITY))
    }

    private fun submit() {
        val form = mutableState.value
        if (!form.canSubmit) return
        mutableState.value = AddItemUiState(error = form.error)
        viewModelScope.launch {
            val photo = form.photoUri?.let { photoCompressor.compress(it) }
            val result = addItem(ItemInput(form.name, form.quantity, form.notes, form.urgent), photo)
            if (result is ItemResult.Failure) mutableState.update { it.copy(error = result.error) }
        }
    }
}
