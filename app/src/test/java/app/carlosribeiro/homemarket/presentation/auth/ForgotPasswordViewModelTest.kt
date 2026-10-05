package app.carlosribeiro.homemarket.presentation.auth

import app.carlosribeiro.homemarket.domain.model.AuthError
import app.carlosribeiro.homemarket.domain.model.AuthResult
import app.carlosribeiro.homemarket.domain.usecase.SendPasswordResetUseCase
import app.carlosribeiro.homemarket.domain.validation.ValidationError
import app.carlosribeiro.homemarket.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ForgotPasswordViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val sendPasswordReset = mockk<SendPasswordResetUseCase>()
    private val viewModel = ForgotPasswordViewModel(sendPasswordReset)

    @Test
    fun open_prefillsTheLoginEmail() {
        viewModel.onEvent(ForgotPasswordUiEvent.Open("maria@example.com"))

        assertTrue(viewModel.state.value.isOpen)
        assertEquals("maria@example.com", viewModel.state.value.email)
    }

    @Test
    fun send_showsTheConfirmation() = runTest {
        coEvery { sendPasswordReset("maria@example.com") } returns AuthResult.Success
        viewModel.onEvent(ForgotPasswordUiEvent.Open("maria@example.com"))

        viewModel.onEvent(ForgotPasswordUiEvent.Send)

        assertTrue(viewModel.state.value.isSent)
        assertFalse(viewModel.state.value.isSending)
    }

    @Test
    fun invalidEmail_isNotSent() = runTest {
        viewModel.onEvent(ForgotPasswordUiEvent.Open("maria"))

        viewModel.onEvent(ForgotPasswordUiEvent.Send)

        assertEquals(AuthFormError.Validation(ValidationError.EMAIL_INVALID), viewModel.state.value.error)
        coVerify(exactly = 0) { sendPasswordReset(any()) }
    }

    @Test
    fun firebaseFailure_isShown() = runTest {
        coEvery { sendPasswordReset(any()) } returns AuthResult.Failure(AuthError.NETWORK)
        viewModel.onEvent(ForgotPasswordUiEvent.Open("maria@example.com"))

        viewModel.onEvent(ForgotPasswordUiEvent.Send)

        assertEquals(AuthFormError.Auth(AuthError.NETWORK), viewModel.state.value.error)
        assertFalse(viewModel.state.value.isSent)
    }

    @Test
    fun dismiss_resetsTheDialog() {
        viewModel.onEvent(ForgotPasswordUiEvent.Open("maria@example.com"))

        viewModel.onEvent(ForgotPasswordUiEvent.Dismiss)

        assertEquals(ForgotPasswordUiState(), viewModel.state.value)
    }
}
