package app.carlosribeiro.homemarket.presentation.onboarding

import app.carlosribeiro.homemarket.domain.model.AppUser
import app.carlosribeiro.homemarket.domain.model.HouseholdError
import app.carlosribeiro.homemarket.domain.model.HouseholdResult
import app.carlosribeiro.homemarket.domain.model.UserRole
import app.carlosribeiro.homemarket.domain.usecase.CreateHouseholdUseCase
import app.carlosribeiro.homemarket.domain.usecase.JoinHouseholdUseCase
import app.carlosribeiro.homemarket.domain.usecase.ObserveCurrentUserUseCase
import app.carlosribeiro.homemarket.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class OnboardingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val user = AppUser("u1", "Maria", "maria@example.com", null, null, UserRole.MEMBER)
    private val observeCurrentUser = mockk<ObserveCurrentUserUseCase> { every { this@mockk() } returns flowOf(user) }
    private val createHousehold = mockk<CreateHouseholdUseCase>()
    private val joinHousehold = mockk<JoinHouseholdUseCase>()

    private fun viewModel() = OnboardingViewModel(observeCurrentUser, createHousehold, joinHousehold)

    @Test
    fun create_callsTheUseCaseWithTheSignedInUser() = runTest {
        coEvery { createHousehold(any(), any()) } returns HouseholdResult.Success("h1")
        val viewModel = viewModel()
        viewModel.onEvent(OnboardingUiEvent.SelectMode(OnboardingMode.CREATE))
        viewModel.onEvent(OnboardingUiEvent.ValueChanged("Casa Silva"))

        viewModel.onEvent(OnboardingUiEvent.Submit)

        coVerify { createHousehold("Casa Silva", "u1") }
        assertNull(viewModel.state.value.error)
        assertFalse(viewModel.state.value.isSubmitting)
    }

    @Test
    fun join_withUnknownCode_showsTheError() = runTest {
        coEvery { joinHousehold(any(), any()) } returns HouseholdResult.Failure(HouseholdError.TOKEN_NOT_FOUND)
        val viewModel = viewModel()
        viewModel.onEvent(OnboardingUiEvent.SelectMode(OnboardingMode.JOIN))
        viewModel.onEvent(OnboardingUiEvent.ValueChanged("WRONG123"))

        viewModel.onEvent(OnboardingUiEvent.Submit)

        assertEquals(HouseholdError.TOKEN_NOT_FOUND, viewModel.state.value.error)
    }

    @Test
    fun back_returnsToTheOptionsAndClearsTheForm() = runTest {
        val viewModel = viewModel()
        viewModel.onEvent(OnboardingUiEvent.SelectMode(OnboardingMode.JOIN))
        viewModel.onEvent(OnboardingUiEvent.ValueChanged("ABC"))

        viewModel.onEvent(OnboardingUiEvent.Back)

        assertEquals(OnboardingUiState(), viewModel.state.value)
    }

    @Test
    fun submit_withoutAMode_doesNothing() = runTest {
        val viewModel = viewModel()

        viewModel.onEvent(OnboardingUiEvent.Submit)

        coVerify(exactly = 0) { createHousehold(any(), any()) }
        coVerify(exactly = 0) { joinHousehold(any(), any()) }
    }
}
