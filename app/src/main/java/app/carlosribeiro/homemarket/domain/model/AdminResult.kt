package app.carlosribeiro.homemarket.domain.model

/** Outcome of an admin action on the list or its items. */
sealed interface AdminResult {
    data object Success : AdminResult

    data class Failure(val error: AdminError) : AdminResult
}

enum class AdminError {
    NOT_SIGNED_IN,

    /** A member called an admin action. */
    NOT_ADMIN,

    /** The list or item is not in a status that allows this action (for example, locking a locked list). */
    INVALID_STATUS,
    NETWORK,
    UNKNOWN
}
