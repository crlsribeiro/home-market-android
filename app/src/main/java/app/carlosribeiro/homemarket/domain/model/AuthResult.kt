package app.carlosribeiro.homemarket.domain.model

/** Outcome of an authentication call. Errors are values, never exceptions. */
sealed interface AuthResult {
    data object Success : AuthResult

    data class Failure(val error: AuthError) : AuthResult
}

enum class AuthError {
    INVALID_CREDENTIALS,
    EMAIL_ALREADY_IN_USE,
    WEAK_PASSWORD,
    INVALID_EMAIL,
    NETWORK,

    /** Sign in with Google could not get an account from the device (none, or not configured). */
    GOOGLE_UNAVAILABLE,
    UNKNOWN
}
