package app.carlosribeiro.homemarket.presentation.auth

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.carlosribeiro.homemarket.R
import app.carlosribeiro.homemarket.domain.model.AuthError
import app.carlosribeiro.homemarket.domain.validation.ValidationError

/** An error shown under an auth form: either input validation or a failed Firebase call. */
sealed interface AuthFormError {
    data class Validation(val error: ValidationError) : AuthFormError

    data class Auth(val error: AuthError) : AuthFormError
}

/** The form field an error belongs to, so it can be shown on that field instead of under the form. */
enum class AuthField {
    NAME,
    EMAIL,
    PASSWORD,
    CONFIRM_PASSWORD
}

/** The field to flag for this error, or null when it concerns the whole form (shown as a general error). */
val AuthFormError.targetField: AuthField?
    get() = when (this) {
        is AuthFormError.Validation -> error.toField()
        is AuthFormError.Auth -> error.toField()
    }

/** An error's message, routed either to the field it belongs to or to the general error under the form. */
data class ResolvedAuthError(val errorField: AuthField?, val message: String?) {
    /** The message to show on [target], or null when the error belongs elsewhere. */
    fun on(target: AuthField): String? = message.takeIf { errorField == target }

    /** The message to show under the form when no single field is to blame. */
    val general: String? get() = message.takeIf { errorField == null }
}

@Composable
fun AuthFormError?.resolve(): ResolvedAuthError =
    ResolvedAuthError(errorField = this?.targetField, message = this?.let { stringResource(it.messageRes()) })

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
    ValidationError.PHONE_INCOMPLETE -> R.string.account_error_phone_incomplete
}

private fun ValidationError.toField(): AuthField? = when (this) {
    ValidationError.NAME_REQUIRED -> AuthField.NAME
    ValidationError.EMAIL_REQUIRED, ValidationError.EMAIL_INVALID -> AuthField.EMAIL
    ValidationError.PASSWORD_REQUIRED, ValidationError.PASSWORD_TOO_SHORT -> AuthField.PASSWORD
    ValidationError.PASSWORDS_DO_NOT_MATCH -> AuthField.CONFIRM_PASSWORD
    ValidationError.PHONE_INCOMPLETE -> null
}

private fun AuthError.toField(): AuthField? = when (this) {
    AuthError.EMAIL_ALREADY_IN_USE, AuthError.INVALID_EMAIL -> AuthField.EMAIL
    AuthError.WEAK_PASSWORD -> AuthField.PASSWORD
    AuthError.INVALID_CREDENTIALS, AuthError.NETWORK, AuthError.GOOGLE_UNAVAILABLE, AuthError.UNKNOWN -> null
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
