package app.carlosribeiro.homemarket.domain.model

sealed interface ItemResult {
    data object Success : ItemResult

    data class Failure(val error: ItemError) : ItemResult
}

enum class ItemError {
    NAME_REQUIRED,
    NOT_SIGNED_IN,

    /** This week's list was closed early; items can be added again next Monday. */
    CLOSED_THIS_WEEK,

    /** The item was saved but its photo could not be uploaded. */
    PHOTO_UPLOAD,
    NETWORK,
    UNKNOWN
}
