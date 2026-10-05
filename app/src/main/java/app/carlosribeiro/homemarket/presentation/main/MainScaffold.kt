package app.carlosribeiro.homemarket.presentation.main

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import app.carlosribeiro.homemarket.domain.model.AppUser
import app.carlosribeiro.homemarket.presentation.account.AccountRoute
import app.carlosribeiro.homemarket.presentation.admin.AdminPanelRoute
import app.carlosribeiro.homemarket.presentation.list.ListRoute

/**
 * Material 3 adaptive navigation: a navigation bar on phones, a navigation rail on wider windows.
 */
@Composable
fun MainScaffold(user: AppUser, onSignOut: () -> Unit, onOpenItem: (String) -> Unit, modifier: Modifier = Modifier) {
    val tabs = MainTab.visibleFor(user)
    var selected by rememberSaveable { mutableStateOf(MainTab.LIST) }
    val current = selected.takeIf { it in tabs } ?: MainTab.LIST

    NavigationSuiteScaffold(
        modifier = modifier,
        navigationSuiteItems = {
            tabs.forEach { tab ->
                item(
                    selected = tab == current,
                    onClick = { selected = tab },
                    icon = { Icon(painterResource(tab.icon), contentDescription = null) },
                    label = { Text(stringResource(tab.label)) }
                )
            }
        }
    ) {
        when (current) {
            MainTab.LIST -> ListRoute(onOpenItem = onOpenItem)
            MainTab.ADMIN -> AdminPanelRoute()
            MainTab.ACCOUNT -> AccountRoute(onSignOut = onSignOut)
        }
    }
}
