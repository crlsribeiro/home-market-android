package app.carlosribeiro.homemarket.presentation.list

import app.carlosribeiro.homemarket.domain.model.AdminResult
import app.carlosribeiro.homemarket.domain.model.AppUser
import app.carlosribeiro.homemarket.domain.model.ApprovalStatus
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
import app.carlosribeiro.homemarket.testing.MainDispatcherRule
import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.time.Instant
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val admin = AppUser("u1", "Maria", "maria@example.com", null, "h1", UserRole.ADMIN)
    private val list = WeekList("l1", "h1", Instant.EPOCH, Instant.EPOCH, ListStatus.OPEN, null)
    private val observeCurrentUser = mockk<ObserveCurrentUserUseCase> { every { this@mockk() } returns flowOf(admin) }
    private val observeWeeklyList = mockk<ObserveWeeklyListUseCase>()
    private val createWeekList = mockk<CreateWeekListUseCase>()
    private val removeItem = mockk<RemoveItemUseCase>()
    private val expireStaleList = mockk<ExpireStaleListUseCase> { every { isStale(any()) } returns false }

    private fun viewModel() =
        ListViewModel(observeCurrentUser, observeWeeklyList, createWeekList, removeItem, expireStaleList)

    private fun item(id: String, status: ItemStatus, urgent: Boolean = false, approval: ApprovalStatus) = ListItem(
        id, "l1", "h1", "Item $id", 1, "", urgent, "u1", "Maria", status, approval, false, null, null
    )

    @Test
    fun startsLoading_untilTheListArrives() = runTest {
        every { observeWeeklyList("h1") } returns MutableSharedFlow()

        viewModel().state.test {
            assertTrue(awaitItem().isLoading)
        }
    }

    @Test
    fun noCurrentList_showsTheEmptyState() = runTest {
        every { observeWeeklyList("h1") } returns flowOf(WeeklyList(null, emptyList(), emptyList()))

        viewModel().state.test {
            val state = expectMostRecentItem().takeIf { !it.isLoading } ?: awaitItem()
            assertFalse(state.isLoading)
            assertNull(state.currentList)
            assertTrue(state.isAdmin)
        }
    }

    @Test
    fun content_countsItemsLikeTheIosSummaryCards() = runTest {
        val items = listOf(
            item("1", ItemStatus.PENDING, urgent = true, approval = ApprovalStatus.NOT_REQUIRED),
            item("2", ItemStatus.PURCHASED, urgent = true, approval = ApprovalStatus.NOT_REQUIRED),
            item("3", ItemStatus.PENDING, approval = ApprovalStatus.PENDING),
            item("4", ItemStatus.NOT_FOUND, approval = ApprovalStatus.NOT_REQUIRED)
        )
        every { observeWeeklyList("h1") } returns flowOf(WeeklyList(list, items, emptyList()))

        viewModel().state.test {
            val state = expectMostRecentItem().takeIf { !it.isLoading } ?: awaitItem()
            assertEquals(list, state.currentList)
            assertEquals(4, state.items.size)
            assertEquals(1, state.purchasedCount)
            assertEquals(1, state.pendingCount)
            assertEquals(1, state.urgentCount)
        }
    }

    @Test
    fun createListFailure_isShownAndCanBeDismissed() = runTest {
        every { observeWeeklyList("h1") } returns flowOf(WeeklyList(null, emptyList(), emptyList()))
        coEvery { createWeekList() } returns ListResult.Failure(ListError.CLOSED_THIS_WEEK)
        val viewModel = viewModel()

        viewModel.state.test {
            viewModel.onEvent(ListUiEvent.CreateList)
            val failed = expectMostRecentItem()
            assertEquals(ListError.CLOSED_THIS_WEEK, failed.error)
            assertFalse(failed.isCreatingList)

            viewModel.onEvent(ListUiEvent.DismissError)
            assertNull(awaitItem().error)
        }
    }

    @Test
    fun removeItem_deletesItWithoutConfirmation() = runTest {
        every { observeWeeklyList("h1") } returns flowOf(WeeklyList(list, emptyList(), emptyList()))
        coEvery { removeItem("i1") } returns ItemResult.Success

        viewModel().onEvent(ListUiEvent.RemoveItem("i1"))

        coVerify { removeItem("i1") }
    }

    @Test
    fun removeItemOffline_showsTheNetworkError() = runTest {
        every { observeWeeklyList("h1") } returns flowOf(WeeklyList(list, emptyList(), emptyList()))
        coEvery { removeItem("i1") } returns ItemResult.Failure(ItemError.NETWORK)
        val viewModel = viewModel()

        viewModel.state.test {
            viewModel.onEvent(ListUiEvent.RemoveItem("i1"))
            assertEquals(ListError.NETWORK, expectMostRecentItem().error)
        }
    }

    @Test
    fun staleCurrentList_isExpiredOnce() = runTest {
        every { observeWeeklyList("h1") } returns flowOf(WeeklyList(list, emptyList(), emptyList()))
        every { expireStaleList.isStale(list) } returns true
        coEvery { expireStaleList(list) } returns AdminResult.Success
        val viewModel = viewModel()

        viewModel.state.test {
            expectMostRecentItem()
            cancelAndIgnoreRemainingEvents()
        }
        viewModel.state.test {
            expectMostRecentItem()
            cancelAndIgnoreRemainingEvents()
        }
        coVerify(exactly = 1) { expireStaleList(list) }
    }
}
