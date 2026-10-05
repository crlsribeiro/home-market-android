package app.carlosribeiro.homemarket.presentation.list

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.carlosribeiro.homemarket.R
import app.carlosribeiro.homemarket.domain.model.AppUser
import app.carlosribeiro.homemarket.domain.model.ApprovalStatus
import app.carlosribeiro.homemarket.domain.model.ItemStatus
import app.carlosribeiro.homemarket.domain.model.ListItem
import app.carlosribeiro.homemarket.domain.model.ListStatus
import app.carlosribeiro.homemarket.domain.model.UserRole
import app.carlosribeiro.homemarket.domain.model.WeekList
import app.carlosribeiro.homemarket.presentation.theme.HomeMarketTheme
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ListScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun user(role: UserRole) = AppUser("u1", "Maria Silva", "maria@example.com", null, "h1", role)

    @Test
    fun noList_adminCanCreateThisWeeksList() {
        val events = mutableListOf<ListUiEvent>()
        composeRule.setContent {
            HomeMarketTheme {
                ListScreen(state = ListUiState(isLoading = false, user = user(UserRole.ADMIN)), onEvent = {
                    events += it
                })
            }
        }

        composeRule.onNodeWithText(context.getString(R.string.list_no_active_admin)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.list_create)).performClick()
        assertEquals(listOf(ListUiEvent.CreateList), events)
    }

    @Test
    fun noList_memberWaitsForTheAdmin() {
        composeRule.setContent {
            HomeMarketTheme {
                ListScreen(state = ListUiState(isLoading = false, user = user(UserRole.MEMBER)), onEvent = {})
            }
        }

        composeRule.onNodeWithText(context.getString(R.string.list_no_active_member)).assertIsDisplayed()
    }

    @Test
    fun content_showsItemsAndEmptyMessage() {
        val list = WeekList("l1", "h1", Instant.EPOCH, Instant.EPOCH, ListStatus.OPEN, null)
        val item = ListItem(
            "i1", "l1", "h1", "Leite", 2, "", false, "u1", "Maria", ItemStatus.PENDING,
            ApprovalStatus.NOT_REQUIRED, false, null, null
        )
        composeRule.setContent {
            HomeMarketTheme {
                ListScreen(
                    state = ListUiState(
                        isLoading = false,
                        user = user(UserRole.MEMBER),
                        currentList = list,
                        nextWeekItems = listOf(item.copy(id = "i2", status = ItemStatus.ROLLED_OVER))
                    ),
                    onEvent = {}
                )
            }
        }

        composeRule.onNodeWithText(context.getString(R.string.list_empty)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.list_next_week, 1)).assertIsDisplayed()
        composeRule.onNodeWithText("Leite").assertIsDisplayed()
    }
}
