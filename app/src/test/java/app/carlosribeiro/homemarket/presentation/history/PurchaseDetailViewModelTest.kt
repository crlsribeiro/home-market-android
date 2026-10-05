package app.carlosribeiro.homemarket.presentation.history

import androidx.lifecycle.SavedStateHandle
import app.carlosribeiro.homemarket.domain.model.AdminError
import app.carlosribeiro.homemarket.domain.model.AdminResult
import app.carlosribeiro.homemarket.domain.model.Purchase
import app.carlosribeiro.homemarket.domain.model.PurchaseDetail
import app.carlosribeiro.homemarket.domain.model.PurchaseItem
import app.carlosribeiro.homemarket.domain.usecase.EditPurchaseItemUseCase
import app.carlosribeiro.homemarket.domain.usecase.ObservePurchaseDetailUseCase
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

class PurchaseDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val line = PurchaseItem("a", "p1", "MILK", 1, 3.49, 3.49)
    private val purchase = Purchase("p1", "l1", "h1", "28 – 4 OUT", null, null, 3.49, null, true, null, "H-E-B")
    private val detail = MutableStateFlow<PurchaseDetail?>(PurchaseDetail(purchase, listOf(line)))
    private val observe = mockk<ObservePurchaseDetailUseCase> { every { this@mockk("p1") } returns detail }
    private val edit = mockk<EditPurchaseItemUseCase>()

    private fun viewModel() =
        PurchaseDetailViewModel(SavedStateHandle(mapOf(PurchaseDetailViewModel.PURCHASE_ID_KEY to "p1")), observe, edit)

    @Test
    fun editItem_prefillsTheNameAndPriceAndSavesTheParsedPrice() = runTest {
        coEvery { edit("p1", "a", "Milk", 3.75) } returns AdminResult.Success
        val viewModel = viewModel()

        viewModel.state.test {
            assertEquals(detail.value, expectMostRecentItem().detail)
            viewModel.onEvent(PurchaseDetailUiEvent.EditItem(line))
            assertEquals(PurchaseItemDraft(line, "MILK", "3.49"), expectMostRecentItem().draft)
            viewModel.onEvent(PurchaseDetailUiEvent.DraftNameChanged("Milk"))
            viewModel.onEvent(PurchaseDetailUiEvent.DraftPriceChanged("3,75"))
            assertTrue(expectMostRecentItem().draft!!.canSave)
            viewModel.onEvent(PurchaseDetailUiEvent.SaveEdit)
            assertNull(expectMostRecentItem().draft)
        }
        coVerify { edit("p1", "a", "Milk", 3.75) }
    }

    @Test
    fun draft_cannotSaveABlankNameOrAnInvalidPrice() {
        assertFalse(PurchaseItemDraft(line, " ", "1").canSave)
        assertFalse(PurchaseItemDraft(line, "Milk", "abc").canSave)
    }

    @Test
    fun failedSave_showsTheError() = runTest {
        coEvery { edit(any(), any(), any(), any()) } returns AdminResult.Failure(AdminError.NETWORK)
        val viewModel = viewModel()

        viewModel.state.test {
            viewModel.onEvent(PurchaseDetailUiEvent.EditItem(line))
            viewModel.onEvent(PurchaseDetailUiEvent.SaveEdit)
            val state = expectMostRecentItem()
            assertNull(state.draft)
            assertEquals(AdminError.NETWORK, state.error)
        }
    }

    @Test
    fun removedPurchase_isGone() = runTest {
        val viewModel = viewModel()

        viewModel.state.test {
            assertFalse(expectMostRecentItem().isGone)
            detail.value = null
            assertTrue(expectMostRecentItem().isGone)
        }
    }
}
