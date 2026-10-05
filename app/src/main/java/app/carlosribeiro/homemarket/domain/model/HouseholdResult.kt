package app.carlosribeiro.homemarket.domain.model

sealed interface HouseholdResult {
    data class Success(val householdId: String) : HouseholdResult

    data class Failure(val error: HouseholdError) : HouseholdResult
}

enum class HouseholdError {
    NAME_REQUIRED,
    TOKEN_REQUIRED,
    TOKEN_NOT_FOUND,
    NOT_SIGNED_IN,
    NOT_ADMIN,
    NETWORK,
    UNKNOWN
}
