package app.carlosribeiro.homemarket.domain.model

sealed interface ListResult {
    data class Success(val list: WeekList) : ListResult

    data class Failure(val error: ListError) : ListResult
}

enum class ListError {
    NOT_SIGNED_IN,
    NOT_ADMIN,

    /** This week's list was closed early; a new list can only be created next Monday. */
    CLOSED_THIS_WEEK,
    NETWORK,
    UNKNOWN
}
