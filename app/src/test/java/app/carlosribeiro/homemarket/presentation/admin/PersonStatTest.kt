package app.carlosribeiro.homemarket.presentation.admin

import app.carlosribeiro.homemarket.domain.model.ApprovalStatus
import app.carlosribeiro.homemarket.domain.model.ItemStatus
import app.carlosribeiro.homemarket.domain.model.ListItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonStatTest {

    private fun item(name: String, uid: String, author: String) = ListItem(
        name, "l1", "h1", name, 1, "", false, uid, author, ItemStatus.PENDING, ApprovalStatus.NOT_REQUIRED, false, null,
        null
    )

    @Test
    fun groupsByUidAndSortsByItemCount() {
        val stats = personStats(
            listOf(
                item("Leite", "u1", "Maria"),
                item("Pão", "u2", "João"),
                item("Café", "u2", "João"),
                item("Ovos", "u3", "Maria")
            )
        )

        assertEquals(listOf("u2", "u1", "u3"), stats.map { it.uid })
        assertEquals(PersonStat("u2", "João", listOf("Pão", "Café")), stats.first())
        assertEquals(2, stats.first().itemCount)
    }

    @Test
    fun noItems_noBars() {
        assertTrue(personStats(emptyList()).isEmpty())
    }
}
