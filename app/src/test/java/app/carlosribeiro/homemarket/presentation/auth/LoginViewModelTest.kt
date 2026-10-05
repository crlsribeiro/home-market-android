package app.carlosribeiro.homemarket.presentation.auth

import app.carlosribeiro.homemarket.domain.model.AuthError
import app.carlosribeiro.homemarket.domain.model.AuthResult
import app.carlosribeiro.homemarket.domain.usecase.SignInUseCase
import app.carlosribeiro.homemarket.domain.validation.ValidationError
import app.carlosribeiro.homemarket.testing.MainDispatcherRule
import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val signIn = mockk<SignInUseCase>()

    private fun viewModel() = LoginViewModel(signIn)

    @Test
    fun submit_withInvalidEmail_showsValidationErrorWithoutCallingFirebase() = runTest {
        val viewModel = viewModel()
        viewModel.onEvent(LoginUiEvent.EmailChanged("maria"))
        viewModel.onEvent(LoginUiEvent.PasswordChanged("secret"))

        viewModel.onEvent(LoginUiEvent.Submit)

        assertEquals(AuthFormError.Validation(ValidationError.EMAIL_INVALID), viewModel.state.value.error)
        coVerify(exactly = 0) { signIn(any(), any()) }
    }

    @Test
    fun submit_showsLoadingThenClearsOnSuccess() = runTest {
        val result = CompletableDeferred<AuthResult>()
        coEvery { signIn(any(), any()) } coAnswers { result.await() }
        val viewModel = viewModel()
        viewModel.onEvent(LoginUiEvent.EmailChanged("maria@example.com"))
        viewModel.onEvent(LoginUiEvent.PasswordChanged("secret"))

        viewModel.state.test {
            assertFalse(awaitItem().isLoading)
            viewModel.onEvent(LoginUiEvent.Submit)
            assertTrue(awaitItem().isLoading)
            result.complete(AuthResult.Success)
            val done = awaitItem()
            assertFalse(done.isLoading)
            assertNull(done.error)
        }
        coVerify { signIn("maria@example.com", "secret") }
    }

    @Test
    fun submit_wrongPassword_showsAuthError() = runTest {
        coEvery { signIn(any(), any()) } returns AuthResult.Failure(AuthError.INVALID_CREDENTIALS)
        val viewModel = viewModel()
        viewModel.onEvent(LoginUiEvent.EmailChanged("maria@example.com"))
        viewModel.onEvent(LoginUiEvent.PasswordChanged("wrong"))

        viewModel.onEvent(LoginUiEvent.Submit)

        assertEquals(AuthFormError.Auth(AuthError.INVALID_CREDENTIALS), viewModel.state.value.error)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun editingAField_clearsTheError() = runTest {
        val viewModel = viewModel()
        viewModel.onEvent(LoginUiEvent.Submit)
        assertEquals(AuthFormError.Validation(ValidationError.EMAIL_REQUIRED), viewModel.state.value.error)

        viewModel.onEvent(LoginUiEvent.EmailChanged("m"))

        assertNull(viewModel.state.value.error)
    }
}
