package app.carlosribeiro.homemarket.presentation.auth

import app.carlosribeiro.homemarket.domain.model.AuthError
import app.carlosribeiro.homemarket.domain.model.AuthResult
import app.carlosribeiro.homemarket.domain.model.PhoneCountry
import app.carlosribeiro.homemarket.domain.model.Registration
import app.carlosribeiro.homemarket.domain.usecase.RegisterUseCase
import app.carlosribeiro.homemarket.domain.util.PhotoCompressor
import app.carlosribeiro.homemarket.domain.validation.ValidationError
import app.carlosribeiro.homemarket.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class RegisterViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val register = mockk<RegisterUseCase>()
    private val photo = byteArrayOf(5)
    private val compressor = PhotoCompressor { uri -> if (uri == "content://ok") photo else null }

    private fun filledViewModel(confirm: String = "12345678") = RegisterViewModel(register, compressor).apply {
        onEvent(RegisterUiEvent.FirstNameChanged("Maria"))
        onEvent(RegisterUiEvent.LastNameChanged("Silva"))
        onEvent(RegisterUiEvent.EmailChanged("maria@example.com"))
        onEvent(RegisterUiEvent.PasswordChanged("12345678"))
        onEvent(RegisterUiEvent.ConfirmPasswordChanged(confirm))
    }

    @Test
    fun submit_passwordsDoNotMatch_showsValidationError() = runTest {
        val viewModel = filledViewModel(confirm = "different")

        viewModel.onEvent(RegisterUiEvent.Submit)

        assertEquals(AuthFormError.Validation(ValidationError.PASSWORDS_DO_NOT_MATCH), viewModel.state.value.error)
        coVerify(exactly = 0) { register(any(), any()) }
    }

    @Test
    fun submit_validForm_registersUser() = runTest {
        coEvery { register(any(), any()) } returns AuthResult.Success
        val viewModel = filledViewModel()

        viewModel.onEvent(RegisterUiEvent.Submit)

        coVerify { register(Registration("Maria", "Silva", "maria@example.com", "12345678"), null) }
        assertNull(viewModel.state.value.error)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun submit_emailInUse_showsAuthError() = runTest {
        coEvery { register(any(), any()) } returns AuthResult.Failure(AuthError.EMAIL_ALREADY_IN_USE)
        val viewModel = filledViewModel()

        viewModel.onEvent(RegisterUiEvent.Submit)

        assertEquals(AuthFormError.Auth(AuthError.EMAIL_ALREADY_IN_USE), viewModel.state.value.error)
    }

    @Test
    fun submit_withPhoneAndPhoto_sendsBoth() = runTest {
        coEvery { register(any(), any()) } returns AuthResult.Success
        val viewModel = filledViewModel()
        viewModel.onEvent(RegisterUiEvent.CountryChanged(PhoneCountry.UNITED_STATES))
        viewModel.onEvent(RegisterUiEvent.PhoneChanged("4155550100"))
        viewModel.onEvent(RegisterUiEvent.PhotoPicked("content://ok"))

        viewModel.onEvent(RegisterUiEvent.Submit)

        coVerify {
            register(
                Registration("Maria", "Silva", "maria@example.com", "12345678", "(415) 555-0100", "+1"),
                photo
            )
        }
    }

    @Test
    fun submit_incompletePhone_showsValidationError() = runTest {
        val viewModel = filledViewModel()
        viewModel.onEvent(RegisterUiEvent.PhoneChanged("1198"))

        viewModel.onEvent(RegisterUiEvent.Submit)

        assertEquals(AuthFormError.Validation(ValidationError.PHONE_INCOMPLETE), viewModel.state.value.error)
        coVerify(exactly = 0) { register(any(), any()) }
    }
}
