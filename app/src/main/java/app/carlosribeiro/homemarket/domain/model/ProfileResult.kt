package app.carlosribeiro.homemarket.domain.model

sealed interface ProfileResult {
    /** [emailVerificationSentTo] is set when a new login email is waiting for its confirmation link. */
    data class Success(val emailVerificationSentTo: String? = null) : ProfileResult

    data class Failure(val error: ProfileError) : ProfileResult
}

enum class ProfileError {
    NOT_SIGNED_IN,
    PHONE_INCOMPLETE,
    EMAIL_INVALID,
    EMAIL_IN_USE,

    /** Firebase asks the user to sign in again before changing the email or deleting the account. */
    REQUIRES_RECENT_LOGIN,
    NETWORK,
    UNKNOWN
}
