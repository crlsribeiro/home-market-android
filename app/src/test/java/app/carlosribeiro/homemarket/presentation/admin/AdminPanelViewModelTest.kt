package app.carlosribeiro.homemarket.presentation.admin

import app.carlosribeiro.homemarket.domain.model.AdminError
import app.carlosribeiro.homemarket.domain.model.AdminResult
import app.carlosribeiro.homemarket.domain.model.AppUser
import app.carlosribeiro.homemarket.domain.model.ApprovalStatus
import app.carlosribeiro.homemarket.domain.model.Household
import app.carlosribeiro.homemarket.domain.model.HouseholdError
import app.carlosribeiro.homemarket.domain.model.HouseholdResult
import app.carlosribeiro.homemarket.domain.model.ItemStatus
import app.carlosribeiro.homemarket.domain.model.ListItem
import app.carlosribeiro.homemarket.domain.model.ListStatus
import app.carlosribeiro.homemarket.domain.model.UserRole
import app.carlosribeiro.homemarket.domain.model.WeekList
import app.carlosribeiro.homemarket.domain.model.WeeklyList
import app.carlosribeiro.homemarket.domain.usecase.ApproveItemUseCase
import app.carlosribeiro.homemarket.domain.usecase.ListAdminActions
import app.carlosribeiro.homemarket.domain.usecase.LockListUseCase
import app.carlosribeiro.homemarket.domain.usecase.ObserveCurrentUserUseCase
import app.carlosribeiro.homemarket.domain.usecase.ObserveHouseholdUseCase
import app.carlosribeiro.homemarket.domain.usecase.ObserveMembersUseCase
import app.carlosribeiro.homemarket.domain.usecase.ObserveWeeklyListUseCase
import app.carlosribeiro.homemarket.domain.usecase.RegenerateInviteTokenUseCase
import app.carlosribeiro.homemarket.domain.usecase.RejectItemUseCase
import app.carlosribeiro.homemarket.domain.usecase.ReopenListUseCase
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
import org.junit.Rule
import org.junit.Test

class AdminPanelViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val admin = AppUser("u1", "Maria", "maria@example.com", null, "h1", UserRole.ADMIN)
    private val household = Household("h1", "Casa Silva", "u1", "ABC12345", listOf("u1"))
    private val observeCurrentUser = mockk<ObserveCurrentUserUseCase> { every { this@mockk() } returns flowOf(admin) }
    private val observeHousehold =
        mockk<ObserveHouseholdUseCase> { every { this@mockk("h1") } returns flowOf(household) }
    private val observeMembers =
        mockk<ObserveMembersUseCase> { every { this@mockk("h1") } returns flowOf(listOf(admin)) }
    private val regenerate = mockk<RegenerateInviteTokenUseCase>()
    private val pendingItem = ListItem(
        "i1", "l1", "h1", "Leite", 1, "", false, "u2", "João",
        ItemStatus.PENDING, ApprovalStatus.PENDING, false, null, null
    )
    private val okItem = pendingItem.copy(id = "i2", approvalStatus = ApprovalStatus.NOT_REQUIRED)
    private val list = WeekList("l1", "h1", Instant.EPOCH, Instant.EPOCH, ListStatus.OPEN, null)
    private val observeWeeklyList = mockk<ObserveWeeklyListUseCase> {
        every { this@mockk("h1") } returns flowOf(WeeklyList(list, listOf(pendingItem, okItem), emptyList()))
    }
    private val lockList = mockk<LockListUseCase>()
    private val reopenList = mockk<ReopenListUseCase>()
    private val approveItem = mockk<ApproveItemUseCase>()
    private val rejectItem = mockk<RejectItemUseCase>()

    private fun viewModel() = AdminPanelViewModel(
        observeCurrentUser,
        observeHousehold,
        observeMembers,
        observeWeeklyList,
        regenerate,
        ListAdminActions(lockList, reopenList, approveItem, rejectItem)
    )

    @Test
    fun showsTheHouseholdAndItsMembers() = runTest {
        viewModel().state.test {
            val loaded = expectMostRecentItem().takeIf { it.household != null } ?: awaitItem()
            assertEquals(household, loaded.household)
            assertEquals(listOf(admin), loaded.members)
        }
    }

    @Test
    fun regenerateFailure_isShown() = runTest {
        coEvery { regenerate() } returns HouseholdResult.Failure(HouseholdError.NETWORK)
        val viewModel = viewModel()

        viewModel.state.test {
            viewModel.onEvent(AdminPanelUiEvent.RegenerateInviteToken)
            val last = expectMostRecentItem()
            assertEquals(HouseholdError.NETWORK, last.error)
            assertFalse(last.isRegenerating)
        }
    }

    @Test
    fun showsTheCurrentListAndOnlyItemsAwaitingApproval() = runTest {
        viewModel().state.test {
            val loaded = expectMostRecentItem().takeIf { it.currentList != null } ?: awaitItem()
            assertEquals(list, loaded.currentList)
            assertEquals(listOf(pendingItem), loaded.pendingApprovals)
        }
    }

    @Test
    fun lockApproveAndReject_callTheUseCases() = runTest {
        coEvery { lockList(list) } returns AdminResult.Success
        coEvery { approveItem(pendingItem) } returns AdminResult.Success
        coEvery { rejectItem(pendingItem) } returns AdminResult.Success
        val viewModel = viewModel()

        viewModel.state.test {
            expectMostRecentItem()
            viewModel.onEvent(AdminPanelUiEvent.LockList)
            viewModel.onEvent(AdminPanelUiEvent.Approve(pendingItem))
            viewModel.onEvent(AdminPanelUiEvent.Reject(pendingItem))
            cancelAndIgnoreRemainingEvents()
        }
        coVerify { lockList(list) }
        coVerify { approveItem(pendingItem) }
        coVerify { rejectItem(pendingItem) }
    }

    @Test
    fun adminFailure_isShownAndCanBeDismissed() = runTest {
        coEvery { reopenList(list) } returns AdminResult.Failure(AdminError.INVALID_STATUS)
        val viewModel = viewModel()

        viewModel.state.test {
            expectMostRecentItem()
            viewModel.onEvent(AdminPanelUiEvent.ReopenList)
            assertEquals(AdminError.INVALID_STATUS, expectMostRecentItem().adminError)
            viewModel.onEvent(AdminPanelUiEvent.DismissAdminError)
            assertEquals(null, awaitItem().adminError)
        }
    }
}
