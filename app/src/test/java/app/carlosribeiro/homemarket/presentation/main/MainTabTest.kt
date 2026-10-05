package app.carlosribeiro.homemarket.presentation.main

import app.carlosribeiro.homemarket.domain.model.AppUser
import app.carlosribeiro.homemarket.domain.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Test

class MainTabTest {

    private fun user(role: UserRole) = AppUser("u1", "Maria", "maria@example.com", null, "h1", role)

    @Test
    fun admin_seesEveryTabInTheIosOrder() {
        assertEquals(
            listOf(MainTab.LIST, MainTab.ADMIN, MainTab.HISTORY, MainTab.ACCOUNT),
            MainTab.visibleFor(user(UserRole.ADMIN))
        )
    }

    @Test
    fun member_seesNeitherAdminNorHistory() {
        assertEquals(listOf(MainTab.LIST, MainTab.ACCOUNT), MainTab.visibleFor(user(UserRole.MEMBER)))
    }
}
