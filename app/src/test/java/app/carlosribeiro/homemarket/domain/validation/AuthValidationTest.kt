package app.carlosribeiro.homemarket.domain.validation

import app.carlosribeiro.homemarket.domain.model.Registration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthValidationTest {

    private val valid = Registration(
        firstName = "Maria",
        lastName = "Silva",
        email = "maria@example.com",
        password = "12345678"
    )

    @Test
    fun signIn_validInput_hasNoError() {
        assertNull(AuthValidation.validateSignIn("maria@example.com", "secret"))
    }

    @Test
    fun signIn_blankEmail_isRequired() {
        assertEquals(ValidationError.EMAIL_REQUIRED, AuthValidation.validateSignIn("  ", "secret"))
    }

    @Test
    fun signIn_malformedEmail_isInvalid() {
        assertEquals(ValidationError.EMAIL_INVALID, AuthValidation.validateSignIn("maria@", "secret"))
    }

    @Test
    fun signIn_emptyPassword_isRequired() {
        assertEquals(ValidationError.PASSWORD_REQUIRED, AuthValidation.validateSignIn("maria@example.com", ""))
    }

    @Test
    fun registration_validInput_hasNoError() {
        assertNull(AuthValidation.validateRegistration(valid, "12345678"))
    }

    @Test
    fun registration_missingLastName_needsName() {
        assertEquals(
            ValidationError.NAME_REQUIRED,
            AuthValidation.validateRegistration(valid.copy(lastName = " "), "12345678")
        )
    }

    @Test
    fun registration_shortPassword_isTooShort() {
        assertEquals(
            ValidationError.PASSWORD_TOO_SHORT,
            AuthValidation.validateRegistration(valid.copy(password = "1234567"), "1234567")
        )
    }

    @Test
    fun registration_differentConfirmation_doesNotMatch() {
        assertEquals(ValidationError.PASSWORDS_DO_NOT_MATCH, AuthValidation.validateRegistration(valid, "87654321"))
    }

    @Test
    fun registration_invalidEmail_isInvalid() {
        assertEquals(
            ValidationError.EMAIL_INVALID,
            AuthValidation.validateRegistration(valid.copy(email = "maria.example.com"), "12345678")
        )
    }

    @Test
    fun validateEmail_requiresAValidEmail() {
        assertEquals(ValidationError.EMAIL_REQUIRED, AuthValidation.validateEmail(" "))
        assertEquals(ValidationError.EMAIL_INVALID, AuthValidation.validateEmail("maria"))
        assertEquals(null, AuthValidation.validateEmail(" maria@example.com "))
    }

    @Test
    fun registration_phoneMustBeCompleteOrEmpty() {
        val registration = Registration("Maria", "Silva", "maria@example.com", "12345678")

        assertNull(AuthValidation.validateRegistration(registration, "12345678"))
        assertNull(
            AuthValidation.validateRegistration(registration.copy(phone = "(11) 98765-4321"), "12345678")
        )
        assertEquals(
            ValidationError.PHONE_INCOMPLETE,
            AuthValidation.validateRegistration(registration.copy(phone = "(11) 9876"), "12345678")
        )
    }
}
