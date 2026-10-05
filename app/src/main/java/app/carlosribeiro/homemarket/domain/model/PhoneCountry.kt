package app.carlosribeiro.homemarket.domain.model

/**
 * iOS `PhoneCountry`: a dial code and the local number mask (`#` is a digit). Shared by registration
 * and account settings so both save phone numbers the same way.
 */
enum class PhoneCountry(val isoCode: String, val flag: String, val dialCode: String, val mask: String) {
    BRAZIL("BR", "🇧🇷", "+55", "(##) #####-####"),
    UNITED_STATES("US", "🇺🇸", "+1", "(###) ###-####"),
    PORTUGAL("PT", "🇵🇹", "+351", "### ### ###"),
    ARGENTINA("AR", "🇦🇷", "+54", "## ####-####"),
    SPAIN("ES", "🇪🇸", "+34", "### ## ## ##");

    val placeholder: String get() = mask.replace('#', '9')
    val digitCount: Int get() = mask.count { it == '#' }

    /** Formats the digits of [raw] with the mask, dropping extra digits. */
    fun formatted(raw: String): String {
        val digits = raw.filter { it.isDigit() }.take(digitCount)
        val result = StringBuilder()
        var next = 0
        for (maskChar in mask) {
            if (next == digits.length) break
            if (maskChar == '#') {
                result.append(digits[next])
                next++
            } else {
                result.append(maskChar)
            }
        }
        return result.toString()
    }

    /** iOS rule: a phone number is either empty or complete for the country. */
    fun isValidOrEmpty(phone: String): Boolean {
        val digits = phone.count { it.isDigit() }
        return phone.isBlank() || digits == digitCount
    }

    companion object {
        /** Matches a stored `phoneCountryCode`; falls back to Brazil, the registration default. */
        fun matching(dialCode: String?): PhoneCountry = entries.firstOrNull { it.dialCode == dialCode } ?: BRAZIL
    }
}
