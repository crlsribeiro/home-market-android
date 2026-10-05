package app.carlosribeiro.homemarket.presentation.auth

import androidx.annotation.StringRes
import app.carlosribeiro.homemarket.R
import app.carlosribeiro.homemarket.domain.model.AuthError
import app.carlosribeiro.homemarket.domain.validation.ValidationError

/** An error shown under an auth form: either input validation or a failed Firebase call. */
sealed interface AuthFormError {
    data class Validation(val error: ValidationError) : AuthFormError

    data class Auth(val error: AuthError) : AuthFormError
}

@StringRes
fun AuthFormError.messageRes(): Int = when (this) {
    is AuthFormError.Validation -> error.messageRes()
    is AuthFormError.Auth -> error.messageRes()
}

@StringRes
private fun ValidationError.messageRes(): Int = when (this) {
    ValidationError.NAME_REQUIRED -> R.string.auth_error_name_required
    ValidationError.EMAIL_REQUIRED -> R.string.auth_error_email_required
    ValidationError.EMAIL_INVALID -> R.string.auth_error_email_invalid
    ValidationError.PASSWORD_REQUIRED -> R.string.auth_error_password_required
    ValidationError.PASSWORD_TOO_SHORT -> R.string.auth_error_password_too_short
    ValidationError.PASSWORDS_DO_NOT_MATCH -> R.string.auth_error_passwords_do_not_match
}

@StringRes
private fun AuthError.messageRes(): Int = when (this) {
    AuthError.INVALID_CREDENTIALS -> R.string.auth_error_invalid_credentials
    AuthError.EMAIL_ALREADY_IN_USE -> R.string.auth_error_email_in_use
    AuthError.WEAK_PASSWORD -> R.string.auth_error_weak_password
    AuthError.INVALID_EMAIL -> R.string.auth_error_email_invalid
    AuthError.NETWORK -> R.string.auth_error_network
    AuthError.GOOGLE_UNAVAILABLE -> R.string.auth_error_google_unavailable
    AuthError.UNKNOWN -> R.string.auth_error_unknown
}
