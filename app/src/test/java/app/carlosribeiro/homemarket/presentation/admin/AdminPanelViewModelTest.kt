package app.carlosribeiro.homemarket.presentation.admin

import app.carlosribeiro.homemarket.domain.model.AppUser
import app.carlosribeiro.homemarket.domain.model.Household
import app.carlosribeiro.homemarket.domain.model.HouseholdError
import app.carlosribeiro.homemarket.domain.model.HouseholdResult
import app.carlosribeiro.homemarket.domain.model.UserRole
import app.carlosribeiro.homemarket.domain.usecase.ObserveCurrentUserUseCase
import app.carlosribeiro.homemarket.domain.usecase.ObserveHouseholdUseCase
import app.carlosribeiro.homemarket.domain.usecase.ObserveMembersUseCase
import app.carlosribeiro.homemarket.domain.usecase.RegenerateInviteTokenUseCase
import app.carlosribeiro.homemarket.testing.MainDispatcherRule
import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
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

    private fun viewModel() = AdminPanelViewModel(observeCurrentUser, observeHousehold, observeMembers, regenerate)

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
}
