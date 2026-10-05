package app.carlosribeiro.homemarket.domain.validation

import app.carlosribeiro.homemarket.domain.model.Registration

enum class ValidationError {
    NAME_REQUIRED,
    EMAIL_REQUIRED,
    EMAIL_INVALID,
    PASSWORD_REQUIRED,
    PASSWORD_TOO_SHORT,
    PASSWORDS_DO_NOT_MATCH
}

object AuthValidation {
    const val MIN_PASSWORD_LENGTH = 8

    private val emailRegex = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    fun validateSignIn(email: String, password: String): ValidationError? = when {
        email.isBlank() -> ValidationError.EMAIL_REQUIRED
        !emailRegex.matches(email.trim()) -> ValidationError.EMAIL_INVALID
        password.isEmpty() -> ValidationError.PASSWORD_REQUIRED
        else -> null
    }

    /** Same rules as the iOS registration form. */
    fun validateRegistration(registration: Registration, confirmPassword: String): ValidationError? = when {
        registration.firstName.isBlank() || registration.lastName.isBlank() -> ValidationError.NAME_REQUIRED
        registration.email.isBlank() -> ValidationError.EMAIL_REQUIRED
        !emailRegex.matches(registration.email.trim()) -> ValidationError.EMAIL_INVALID
        registration.password.length < MIN_PASSWORD_LENGTH -> ValidationError.PASSWORD_TOO_SHORT
        registration.password != confirmPassword -> ValidationError.PASSWORDS_DO_NOT_MATCH
        else -> null
    }
}
