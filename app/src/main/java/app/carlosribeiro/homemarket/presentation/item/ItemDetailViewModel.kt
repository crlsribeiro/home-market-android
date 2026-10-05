package app.carlosribeiro.homemarket.presentation.item

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.carlosribeiro.homemarket.domain.model.ItemDetail
import app.carlosribeiro.homemarket.domain.model.ItemError
import app.carlosribeiro.homemarket.domain.model.ItemResult
import app.carlosribeiro.homemarket.domain.usecase.ObserveItemDetailUseCase
import app.carlosribeiro.homemarket.domain.usecase.RemoveItemUseCase
import app.carlosribeiro.homemarket.domain.usecase.ReplaceItemPhotoUseCase
import app.carlosribeiro.homemarket.domain.usecase.UpdateItemNotesUseCase
import app.carlosribeiro.homemarket.domain.util.PhotoCompressor
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ItemDetailUiState(
    val isLoading: Boolean = true,
    val detail: ItemDetail? = null,
    /** The item no longer exists (removed here or on another device). */
    val isGone: Boolean = false,
    val isEditingNotes: Boolean = false,
    val draftNotes: String = "",
    val isConfirmingRemove: Boolean = false,
    val pendingPhotoUri: String? = null,
    val isUploadingPhoto: Boolean = false,
    val error: ItemError? = null
)

sealed interface ItemDetailUiEvent {
    data object EditNotes : ItemDetailUiEvent

    data class DraftNotesChanged(val value: String) : ItemDetailUiEvent

    data object CancelNotes : ItemDetailUiEvent

    data object SaveNotes : ItemDetailUiEvent

    data object AskRemove : ItemDetailUiEvent

    data object CancelRemove : ItemDetailUiEvent

    data object ConfirmRemove : ItemDetailUiEvent

    data class PhotoPicked(val uri: String) : ItemDetailUiEvent

    data object DismissError : ItemDetailUiEvent
}

/** iOS `ItemDetailView`: photo, notes, quantity, author, time added, week, and remove. */
@HiltViewModel
class ItemDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeItemDetail: ObserveItemDetailUseCase,
    private val updateNotes: UpdateItemNotesUseCase,
    private val removeItem: RemoveItemUseCase,
    private val replacePhoto: ReplaceItemPhotoUseCase,
    private val photoCompressor: PhotoCompressor
) : ViewModel() {

    private val itemId: String = checkNotNull(savedStateHandle[ITEM_ID_KEY])
    private val ui = MutableStateFlow(ItemDetailUiState())
    private var hasLoaded = false

    val state: StateFlow<ItemDetailUiState> = combine(
        observeItemDetail(itemId).onEach { if (it != null) hasLoaded = true },
        ui
    ) { detail, uiState ->
        uiState.copy(isLoading = detail == null && !hasLoaded, detail = detail, isGone = detail == null && hasLoaded)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ItemDetailUiState())

    fun onEvent(event: ItemDetailUiEvent) {
        when (event) {
            ItemDetailUiEvent.EditNotes -> ui.update {
                it.copy(isEditingNotes = true, draftNotes = state.value.detail?.item?.notes.orEmpty())
            }

            is ItemDetailUiEvent.DraftNotesChanged -> ui.update { it.copy(draftNotes = event.value) }

            ItemDetailUiEvent.CancelNotes -> ui.update { it.copy(isEditingNotes = false) }

            ItemDetailUiEvent.SaveNotes -> saveNotes()

            ItemDetailUiEvent.AskRemove -> ui.update { it.copy(isConfirmingRemove = true) }

            ItemDetailUiEvent.CancelRemove -> ui.update { it.copy(isConfirmingRemove = false) }

            ItemDetailUiEvent.ConfirmRemove -> remove()

            is ItemDetailUiEvent.PhotoPicked -> uploadPhoto(event.uri)

            ItemDetailUiEvent.DismissError -> ui.update { it.copy(error = null) }
        }
    }

    /** Like iOS, the edit closes at once; the cached item shows the new notes as soon as Firestore has them. */
    private fun saveNotes() {
        val notes = ui.value.draftNotes
        ui.update { it.copy(isEditingNotes = false) }
        launchWrite { updateNotes(itemId, notes) }
    }

    private fun remove() {
        ui.update { it.copy(isConfirmingRemove = false) }
        launchWrite { removeItem(itemId) }
    }

    private fun uploadPhoto(uri: String) {
        val item = state.value.detail?.item ?: return
        ui.update { it.copy(pendingPhotoUri = uri, isUploadingPhoto = true) }
        viewModelScope.launch {
            val photo = photoCompressor.compress(uri)
            val result = if (photo == null) ItemResult.Failure(ItemError.PHOTO_UPLOAD) else replacePhoto(item, photo)
            ui.update {
                it.copy(
                    isUploadingPhoto = false,
                    pendingPhotoUri = it.pendingPhotoUri.takeIf { result is ItemResult.Success },
                    error = (result as? ItemResult.Failure)?.error
                )
            }
        }
    }

    private fun launchWrite(write: suspend () -> ItemResult) {
        viewModelScope.launch {
            val result = write()
            if (result is ItemResult.Failure) ui.update { it.copy(error = result.error) }
        }
    }

    companion object {
        /** Name of the `itemId` property of the item detail destination. */
        const val ITEM_ID_KEY = "itemId"
        private const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
