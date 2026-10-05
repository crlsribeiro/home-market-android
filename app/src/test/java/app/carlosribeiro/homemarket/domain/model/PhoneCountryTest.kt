package app.carlosribeiro.homemarket.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneCountryTest {

    @Test
    fun formatted_appliesTheMaskAsYouType() {
        assertEquals("(11", PhoneCountry.BRAZIL.formatted("11"))
        assertEquals("(11) 98765-4321", PhoneCountry.BRAZIL.formatted("11987654321"))
        assertEquals("(11) 98765-4321", PhoneCountry.BRAZIL.formatted("(11) 98765-43219999"))
        assertEquals("(415) 555-0100", PhoneCountry.UNITED_STATES.formatted("4155550100"))
        assertEquals("912 345 678", PhoneCountry.PORTUGAL.formatted("912345678"))
    }

    @Test
    fun phone_mustBeEmptyOrComplete() {
        assertTrue(PhoneCountry.BRAZIL.isValidOrEmpty(""))
        assertTrue(PhoneCountry.BRAZIL.isValidOrEmpty("(11) 98765-4321"))
        assertFalse(PhoneCountry.BRAZIL.isValidOrEmpty("(11) 9876"))
        assertTrue(PhoneCountry.SPAIN.isValidOrEmpty("612 34 56 78"))
    }

    @Test
    fun matching_fallsBackToBrazil() {
        assertEquals(PhoneCountry.UNITED_STATES, PhoneCountry.matching("+1"))
        assertEquals(PhoneCountry.BRAZIL, PhoneCountry.matching(null))
        assertEquals(PhoneCountry.BRAZIL, PhoneCountry.matching("+999"))
    }

    @Test
    fun placeholderAndDigitCount_comeFromTheMask() {
        assertEquals("(99) 99999-9999", PhoneCountry.BRAZIL.placeholder)
        assertEquals(11, PhoneCountry.BRAZIL.digitCount)
    }

    @Test
    fun resolvedName_splitsTheFullNameWhenThereAreNoSeparateNames() {
        val user = AppUser("u1", "Maria da Silva", "m@example.com", null, null, UserRole.MEMBER)

        assertEquals("Maria" to "da Silva", user.resolvedName)
        assertEquals("Ana" to "Souza", user.copy(firstName = "Ana", lastName = "Souza").resolvedName)
        assertEquals("Ana" to "Paula", user.copy(firstName = "Ana Paula").resolvedName)
    }
}
