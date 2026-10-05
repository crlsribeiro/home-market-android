package app.carlosribeiro.homemarket.presentation.item

import androidx.lifecycle.SavedStateHandle
import app.carlosribeiro.homemarket.domain.model.ApprovalStatus
import app.carlosribeiro.homemarket.domain.model.ItemDetail
import app.carlosribeiro.homemarket.domain.model.ItemError
import app.carlosribeiro.homemarket.domain.model.ItemResult
import app.carlosribeiro.homemarket.domain.model.ItemStatus
import app.carlosribeiro.homemarket.domain.model.ListItem
import app.carlosribeiro.homemarket.domain.usecase.ObserveItemDetailUseCase
import app.carlosribeiro.homemarket.domain.usecase.RemoveItemUseCase
import app.carlosribeiro.homemarket.domain.usecase.ReplaceItemPhotoUseCase
import app.carlosribeiro.homemarket.domain.usecase.UpdateItemNotesUseCase
import app.carlosribeiro.homemarket.domain.util.PhotoCompressor
import app.carlosribeiro.homemarket.testing.MainDispatcherRule
import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ItemDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val item = ListItem(
        "i1", "l1", "h1", "Leite", 1, "Integral", false, "u1", "Maria",
        ItemStatus.PENDING, ApprovalStatus.NOT_REQUIRED, false, null, null
    )
    private val detail = MutableStateFlow<ItemDetail?>(ItemDetail(item, null))
    private val observe = mockk<ObserveItemDetailUseCase> { every { this@mockk("i1") } returns detail }
    private val updateNotes = mockk<UpdateItemNotesUseCase>()
    private val removeItem = mockk<RemoveItemUseCase>()
    private val replacePhoto = mockk<ReplaceItemPhotoUseCase>()
    private val photoBytes = byteArrayOf(7)
    private val compressor = PhotoCompressor { uri -> if (uri == "content://ok") photoBytes else null }

    private fun viewModel() = ItemDetailViewModel(
        SavedStateHandle(mapOf(ItemDetailViewModel.ITEM_ID_KEY to "i1")),
        observe,
        updateNotes,
        removeItem,
        replacePhoto,
        compressor
    )

    @Test
    fun editNotes_startsFromTheCurrentNotesAndSaves() = runTest {
        coEvery { updateNotes("i1", "Desnatado") } returns ItemResult.Success
        val viewModel = viewModel()

        viewModel.state.test {
            viewModel.onEvent(ItemDetailUiEvent.EditNotes)
            assertEquals("Integral", expectMostRecentItem().draftNotes)

            viewModel.onEvent(ItemDetailUiEvent.DraftNotesChanged("Desnatado"))
            viewModel.onEvent(ItemDetailUiEvent.SaveNotes)
            assertFalse(expectMostRecentItem().isEditingNotes)
        }
        coVerify { updateNotes("i1", "Desnatado") }
    }

    @Test
    fun remove_needsConfirmationAndClosesWhenTheItemIsGone() = runTest {
        coEvery { removeItem("i1") } coAnswers {
            detail.value = null
            ItemResult.Success
        }
        val viewModel = viewModel()

        viewModel.state.test {
            viewModel.onEvent(ItemDetailUiEvent.AskRemove)
            assertTrue(expectMostRecentItem().isConfirmingRemove)

            viewModel.onEvent(ItemDetailUiEvent.ConfirmRemove)
            val last = expectMostRecentItem()
            assertTrue(last.isGone)
            assertFalse(last.isConfirmingRemove)
        }
    }

    @Test
    fun cancelRemove_keepsTheItem() = runTest {
        val viewModel = viewModel()

        viewModel.state.test {
            viewModel.onEvent(ItemDetailUiEvent.AskRemove)
            viewModel.onEvent(ItemDetailUiEvent.CancelRemove)
            assertFalse(expectMostRecentItem().isConfirmingRemove)
        }
        coVerify(exactly = 0) { removeItem(any()) }
    }

    @Test
    fun replacePhoto_uploadsTheCompressedPhoto() = runTest {
        coEvery { replacePhoto(item, photoBytes) } returns ItemResult.Success
        val viewModel = viewModel()

        viewModel.state.test {
            viewModel.onEvent(ItemDetailUiEvent.PhotoPicked("content://ok"))
            val last = expectMostRecentItem()
            assertFalse(last.isUploadingPhoto)
            assertNull(last.error)
        }
        coVerify { replacePhoto(item, photoBytes) }
    }

    @Test
    fun unreadablePhoto_showsThePhotoError() = runTest {
        val viewModel = viewModel()

        viewModel.state.test {
            viewModel.onEvent(ItemDetailUiEvent.PhotoPicked("content://broken"))
            val last = expectMostRecentItem()
            assertEquals(ItemError.PHOTO_UPLOAD, last.error)
            assertNull(last.pendingPhotoUri)
        }
    }
}
