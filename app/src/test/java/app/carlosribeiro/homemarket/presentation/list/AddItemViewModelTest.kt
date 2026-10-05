package app.carlosribeiro.homemarket.presentation.list

import app.carlosribeiro.homemarket.domain.model.ItemError
import app.carlosribeiro.homemarket.domain.model.ItemResult
import app.carlosribeiro.homemarket.domain.usecase.AddItemUseCase
import app.carlosribeiro.homemarket.domain.usecase.ItemInput
import app.carlosribeiro.homemarket.domain.util.PhotoCompressor
import app.carlosribeiro.homemarket.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AddItemViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val addItem = mockk<AddItemUseCase>()
    private val photoBytes = byteArrayOf(9)
    private val compressor = PhotoCompressor { uri -> if (uri == "content://photo") photoBytes else null }
    private val viewModel = AddItemViewModel(addItem, compressor)

    @Test
    fun quantityStaysBetween1And99() {
        viewModel.onEvent(AddItemUiEvent.DecreaseQuantity)
        assertEquals(1, viewModel.state.value.quantity)

        repeat(120) { viewModel.onEvent(AddItemUiEvent.IncreaseQuantity) }
        assertEquals(99, viewModel.state.value.quantity)
    }

    @Test
    fun blankName_cannotBeSubmitted() {
        viewModel.onEvent(AddItemUiEvent.Open)
        viewModel.onEvent(AddItemUiEvent.NameChanged("  "))

        assertFalse(viewModel.state.value.canSubmit)
        viewModel.onEvent(AddItemUiEvent.Submit)
        assertTrue(viewModel.state.value.isOpen)
        coVerify(exactly = 0) { addItem(any(), any()) }
    }

    @Test
    fun submit_closesTheSheetResetsTheFormAndSavesWithThePhoto() = runTest {
        coEvery { addItem(any(), any()) } returns ItemResult.Success
        viewModel.onEvent(AddItemUiEvent.Open)
        viewModel.onEvent(AddItemUiEvent.NameChanged("Leite"))
        viewModel.onEvent(AddItemUiEvent.IncreaseQuantity)
        viewModel.onEvent(AddItemUiEvent.NotesChanged("Integral"))
        viewModel.onEvent(AddItemUiEvent.UrgentChanged(true))
        viewModel.onEvent(AddItemUiEvent.PhotoPicked("content://photo"))

        viewModel.onEvent(AddItemUiEvent.Submit)

        assertEquals(AddItemUiState(), viewModel.state.value)
        coVerify { addItem(ItemInput("Leite", 2, "Integral", true), photoBytes) }
    }

    @Test
    fun failure_isShownAfterTheSheetClosed() = runTest {
        coEvery { addItem(any(), any()) } returns ItemResult.Failure(ItemError.CLOSED_THIS_WEEK)
        viewModel.onEvent(AddItemUiEvent.Open)
        viewModel.onEvent(AddItemUiEvent.NameChanged("Leite"))

        viewModel.onEvent(AddItemUiEvent.Submit)

        assertFalse(viewModel.state.value.isOpen)
        assertEquals(ItemError.CLOSED_THIS_WEEK, viewModel.state.value.error)
        viewModel.onEvent(AddItemUiEvent.DismissError)
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun removePhoto_clearsIt() {
        viewModel.onEvent(AddItemUiEvent.PhotoPicked("content://photo"))
        viewModel.onEvent(AddItemUiEvent.RemovePhoto)

        assertNull(viewModel.state.value.photoUri)
    }
}
