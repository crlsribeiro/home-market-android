package app.carlosribeiro.homemarket.presentation.shopping

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.carlosribeiro.homemarket.R
import app.carlosribeiro.homemarket.domain.model.ApprovalStatus
import app.carlosribeiro.homemarket.domain.model.ItemStatus
import app.carlosribeiro.homemarket.domain.model.ListItem
import app.carlosribeiro.homemarket.domain.model.ListStatus
import app.carlosribeiro.homemarket.domain.model.WeekList
import app.carlosribeiro.homemarket.presentation.theme.HomeMarketTheme
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShoppingModeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val item = ListItem(
        "i1", "l1", "h1", "Leite", 1, "", false, "u2", "João",
        ItemStatus.PENDING, ApprovalStatus.NOT_REQUIRED, false, null, null
    )
    private val state = ShoppingUiState(
        currentList = WeekList("l1", "h1", Instant.EPOCH, Instant.EPOCH, ListStatus.SHOPPING, null),
        items = listOf(item)
    )

    @Test
    fun gotIt_marksTheItemPurchased() {
        val events = mutableListOf<ShoppingUiEvent>()
        composeRule.setContent { HomeMarketTheme { ShoppingModeScreen(state = state, onEvent = { events += it }) } }

        composeRule.onNodeWithText(context.getString(R.string.shopping_got_it)).performClick()

        assertEquals(listOf(ShoppingUiEvent.TogglePurchased(item)), events)
    }

    @Test
    fun abandon_asksForConfirmationFirst() {
        val events = mutableListOf<ShoppingUiEvent>()
        composeRule.setContent { HomeMarketTheme { ShoppingModeScreen(state = state, onEvent = { events += it }) } }

        composeRule.onNodeWithText(context.getString(R.string.shopping_abandon)).performClick()
        assertEquals(emptyList<ShoppingUiEvent>(), events)
        composeRule.onNodeWithText(context.getString(R.string.shopping_keep_shopping)).performClick()
        assertEquals(emptyList<ShoppingUiEvent>(), events)
    }
}
