package app.carlosribeiro.homemarket.domain.model

data class Registration(
    val firstName: String,
    val lastName: String,
    val email: String,
    val password: String,
    val phone: String = "",
    val phoneCountryCode: String = PhoneCountry.BRAZIL.dialCode
)
