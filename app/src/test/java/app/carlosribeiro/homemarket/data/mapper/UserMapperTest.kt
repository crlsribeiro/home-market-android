package app.carlosribeiro.homemarket.data.mapper

import app.carlosribeiro.homemarket.domain.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UserMapperTest {

    @Test
    fun mapsAllFields() {
        val user = UserMapper.fromDocument(
            uid = "u1",
            data = mapOf(
                "displayName" to "Maria Silva",
                "email" to "maria@example.com",
                "photoURL" to "https://example.com/a.jpg",
                "householdId" to "h1",
                "role" to "admin"
            ),
            authDisplayName = null,
            authEmail = null
        )

        assertEquals("u1", user.uid)
        assertEquals("Maria Silva", user.displayName)
        assertEquals("maria@example.com", user.email)
        assertEquals("https://example.com/a.jpg", user.photoUrl)
        assertEquals("h1", user.householdId)
        assertEquals(UserRole.ADMIN, user.role)
    }

    @Test
    fun missingDocument_fallsBackToAuthValues() {
        val user = UserMapper.fromDocument("u1", emptyMap(), authDisplayName = "Maria", authEmail = "m@example.com")

        assertEquals("Maria", user.displayName)
        assertEquals("m@example.com", user.email)
        assertNull(user.householdId)
        assertEquals(UserRole.MEMBER, user.role)
    }

    @Test
    fun noNameAnywhere_usesWebDefault() {
        val user = UserMapper.fromDocument("u1", emptyMap(), authDisplayName = null, authEmail = null)

        assertEquals("Usuário", user.displayName)
    }

    @Test
    fun unknownRole_isMember() {
        val user = UserMapper.fromDocument("u1", mapOf("role" to "owner"), null, null)

        assertEquals(UserRole.MEMBER, user.role)
    }

    @Test
    fun blankHouseholdId_isNull() {
        val user = UserMapper.fromDocument("u1", mapOf("householdId" to ""), null, null)

        assertNull(user.householdId)
    }
}
