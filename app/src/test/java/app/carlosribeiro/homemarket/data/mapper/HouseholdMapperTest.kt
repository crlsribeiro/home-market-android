package app.carlosribeiro.homemarket.data.mapper

import app.carlosribeiro.homemarket.domain.model.AppUser
import app.carlosribeiro.homemarket.domain.model.Household
import app.carlosribeiro.homemarket.domain.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Test

class HouseholdMapperTest {

    @Test
    fun documentToDomain_mapsAllFields() {
        val entity = HouseholdMapper.documentToEntity(
            "h1",
            mapOf(
                "name" to "Casa Silva",
                "adminUid" to "u1",
                "inviteToken" to "ABC12345",
                "memberUids" to listOf("u1", "u2")
            )
        )

        assertEquals(
            Household("h1", "Casa Silva", "u1", "ABC12345", listOf("u1", "u2")),
            HouseholdMapper.entityToDomain(entity)
        )
    }

    @Test
    fun documentToEntity_missingFields_useSafeDefaults() {
        val entity = HouseholdMapper.documentToEntity("h1", mapOf("memberUids" to listOf("u1", 42)))

        assertEquals("", entity.name)
        assertEquals(listOf("u1"), entity.memberUids)
    }

    @Test
    fun member_roundTripsThroughTheCache() {
        val user = AppUser("u2", "Ana", "ana@example.com", null, "h1", UserRole.ADMIN)

        assertEquals(user, HouseholdMapper.memberToDomain(HouseholdMapper.memberToEntity("h1", user)))
    }
}
