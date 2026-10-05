package app.carlosribeiro.homemarket.presentation.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.carlosribeiro.homemarket.R
import app.carlosribeiro.homemarket.domain.model.AppUser
import app.carlosribeiro.homemarket.domain.model.UserRole
import app.carlosribeiro.homemarket.presentation.theme.HomeMarketTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val user = AppUser(
        uid = "uid",
        displayName = "Maria Silva",
        email = "maria@example.com",
        photoUrl = null,
        householdId = null,
        role = UserRole.MEMBER
    )

    @Test
    fun showsGreetingAndEmail() {
        composeRule.setContent { HomeMarketTheme { HomeScreen(user = user, household = null, onSignOut = {}) } }

        composeRule.onNodeWithText(context.getString(R.string.home_greeting, "Maria")).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.home_signed_in_as, user.email)).assertIsDisplayed()
    }

    @Test
    fun signOutAsksForConfirmation() {
        var signedOut = false
        composeRule.setContent {
            HomeMarketTheme {
                HomeScreen(user = user, household = null, onSignOut = {
                    signedOut =
                        true
                })
            }
        }
        val signOut = context.getString(R.string.auth_sign_out)

        composeRule.onAllNodesWithText(signOut).onFirst().performClick()
        composeRule.onNodeWithText(context.getString(R.string.auth_sign_out_title)).assertIsDisplayed()
        composeRule.onAllNodesWithText(signOut).onFirst().performClick()

        assertTrue(signedOut)
    }
}
