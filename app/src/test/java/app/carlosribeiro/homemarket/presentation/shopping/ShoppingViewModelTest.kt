package app.carlosribeiro.homemarket.presentation.shopping

import app.carlosribeiro.homemarket.domain.model.AdminError
import app.carlosribeiro.homemarket.domain.model.AdminResult
import app.carlosribeiro.homemarket.domain.model.AppUser
import app.carlosribeiro.homemarket.domain.model.ApprovalStatus
import app.carlosribeiro.homemarket.domain.model.ItemResult
import app.carlosribeiro.homemarket.domain.model.ItemStatus
import app.carlosribeiro.homemarket.domain.model.ListItem
import app.carlosribeiro.homemarket.domain.model.ListStatus
import app.carlosribeiro.homemarket.domain.model.UserRole
import app.carlosribeiro.homemarket.domain.model.WeekList
import app.carlosribeiro.homemarket.domain.model.WeeklyList
import app.carlosribeiro.homemarket.domain.usecase.AbandonShoppingUseCase
import app.carlosribeiro.homemarket.domain.usecase.CloseShoppingListUseCase
import app.carlosribeiro.homemarket.domain.usecase.MarkNotFoundUseCase
import app.carlosribeiro.homemarket.domain.usecase.ObserveCurrentUserUseCase
import app.carlosribeiro.homemarket.domain.usecase.ObserveWeeklyListUseCase
import app.carlosribeiro.homemarket.domain.usecase.ResolveNotFoundUseCase
import app.carlosribeiro.homemarket.domain.usecase.ShoppingActions
import app.carlosribeiro.homemarket.domain.usecase.TogglePurchasedUseCase
import app.carlosribeiro.homemarket.testing.MainDispatcherRule
import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.time.Instant
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ShoppingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val admin = AppUser("u1", "Maria", "maria@example.com", null, "h1", UserRole.ADMIN)
    private val list = WeekList("l1", "h1", Instant.EPOCH, Instant.EPOCH, ListStatus.SHOPPING, null)

    private fun item(id: String, status: ItemStatus, approval: ApprovalStatus = ApprovalStatus.NOT_REQUIRED) =
        ListItem(id, "l1", "h1", "Item $id", 1, "", false, "u2", "João", status, approval, false, null, null)

    private val items = listOf(
        item("1", ItemStatus.PENDING),
        item("2", ItemStatus.PENDING, ApprovalStatus.PENDING),
        item("3", ItemStatus.PURCHASED),
        item("4", ItemStatus.NOT_FOUND)
    )
    private val observeUser = mockk<ObserveCurrentUserUseCase> { every { this@mockk() } returns flowOf(admin) }
    private val observeWeeklyList = mockk<ObserveWeeklyListUseCase> {
        every { this@mockk("h1") } returns flowOf(WeeklyList(list, items, emptyList()))
    }
    private val abandon = mockk<AbandonShoppingUseCase>()
    private val close = mockk<CloseShoppingListUseCase>()
    private val toggle = mockk<TogglePurchasedUseCase>()
    private val notFound = mockk<MarkNotFoundUseCase>()
    private val resolveNotFound = mockk<ResolveNotFoundUseCase>()

    private fun viewModel() = ShoppingViewModel(
        observeUser,
        observeWeeklyList,
        ShoppingActions(abandon, close, toggle, notFound),
        resolveNotFound
    )

    @Test
    fun groupsItemsLikeIosShoppingMode() = runTest {
        viewModel().state.test {
            val state = expectMostRecentItem().takeIf { it.currentList != null } ?: awaitItem()
            assertTrue(state.isShopping)
            assertTrue(state.isAdmin)
            assertEquals(listOf("1"), state.toGet.map { it.id })
            assertEquals(listOf("4"), state.notFound.map { it.id })
            assertEquals(listOf("3"), state.picked.map { it.id })
            assertEquals(0.25f, state.progress)
        }
    }

    @Test
    fun actions_callTheUseCases() = runTest {
        coEvery { toggle(any()) } returns AdminResult.Success
        coEvery { notFound(any()) } returns AdminResult.Success
        coEvery { abandon(list) } returns AdminResult.Success
        val viewModel = viewModel()

        viewModel.state.test {
            expectMostRecentItem()
            viewModel.onEvent(ShoppingUiEvent.TogglePurchased(items[0]))
            viewModel.onEvent(ShoppingUiEvent.NotFound(items[0]))
            viewModel.onEvent(ShoppingUiEvent.Abandon)
            cancelAndIgnoreRemainingEvents()
        }
        coVerify { toggle(items[0]) }
        coVerify { notFound(items[0]) }
        coVerify { abandon(list) }
    }

    @Test
    fun closeListFailure_stopsLoadingAndShowsTheError() = runTest {
        coEvery { close(list) } returns AdminResult.Failure(AdminError.NETWORK)
        val viewModel = viewModel()

        viewModel.state.test {
            expectMostRecentItem()
            viewModel.onEvent(ShoppingUiEvent.CloseList)
            val last = expectMostRecentItem()
            assertFalse(last.isClosing)
            assertEquals(AdminError.NETWORK, last.error)
        }
    }

    @Test
    fun emptyList_hasNoProgress() {
        assertEquals(0f, ShoppingUiState().progress)
        assertFalse(ShoppingUiState().isShopping)
    }

    @Test
    fun notFoundDecision_isTheUsersOwnUnresolvedNotFoundItem() {
        val mine = item("5", ItemStatus.NOT_FOUND).copy(addedByUid = "u1")
        val state = ShoppingUiState(user = admin, items = items + mine)

        // Item 4 is not found too, but it was added by someone else.
        assertEquals(mine, state.notFoundDecision)
        assertNull(state.copy(items = items).notFoundDecision)
    }

    @Test
    fun resolveNotFound_callsTheUseCase() = runTest {
        val mine = item("5", ItemStatus.NOT_FOUND).copy(addedByUid = "u1")
        coEvery { resolveNotFound(mine) } returns ItemResult.Success

        viewModel().onEvent(ShoppingUiEvent.ResolveNotFound(mine))

        coVerify { resolveNotFound(mine) }
    }
}
