@file:OptIn(ExperimentalMaterial3Api::class)

package app.carlosribeiro.homemarket.presentation.main

import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.carlosribeiro.homemarket.domain.model.AppUser
import app.carlosribeiro.homemarket.presentation.account.AccountRoute
import app.carlosribeiro.homemarket.presentation.admin.AdminPanelRoute
import app.carlosribeiro.homemarket.presentation.history.HistoryRoute
import app.carlosribeiro.homemarket.presentation.list.ListRoute
import app.carlosribeiro.homemarket.presentation.shopping.NotFoundDialog
import app.carlosribeiro.homemarket.presentation.shopping.ShoppingModeScreen
import app.carlosribeiro.homemarket.presentation.shopping.ShoppingUiEvent
import app.carlosribeiro.homemarket.presentation.shopping.ShoppingViewModel
import app.carlosribeiro.homemarket.presentation.shopping.ShoppingWaitingScreen

/**
 * Material 3 adaptive navigation: a navigation bar on phones, a navigation rail on wider windows.
 */
@Composable
fun MainScaffold(
    user: AppUser,
    onSignOut: () -> Unit,
    onOpenItem: (String) -> Unit,
    onOpenPurchase: (String) -> Unit,
    modifier: Modifier = Modifier,
    shoppingViewModel: ShoppingViewModel = hiltViewModel()
) {
    val shopping by shoppingViewModel.state.collectAsStateWithLifecycle()
    // iOS takeover: while the list is being shopped, the whole app is shopping mode (admin) or the
    // waiting screen (members) instead of the tabs.
    shopping.notFoundDecision?.takeUnless { shopping.isShopping && shopping.isAdmin }?.let { item ->
        NotFoundDialog(item = item, onResolve = { shoppingViewModel.onEvent(ShoppingUiEvent.ResolveNotFound(item)) })
    }
    if (shopping.isShopping) {
        if (shopping.isAdmin) {
            ShoppingModeScreen(state = shopping, onEvent = shoppingViewModel::onEvent, modifier = modifier)
        } else {
            ShoppingWaitingScreen(state = shopping, modifier = modifier)
        }
        return
    }
    val tabs = MainTab.visibleFor(user)
    var selected by rememberSaveable { mutableStateOf(MainTab.LIST) }
    val current = selected.takeIf { it in tabs } ?: MainTab.LIST

    NavigationSuiteScaffold(
        modifier = modifier,
        navigationSuiteItems = {
            tabs.forEach { tab ->
                val isSelected = tab == current
                item(
                    selected = isSelected,
                    onClick = { selected = tab },
                    icon = {
                        Icon(
                            painterResource(if (isSelected) tab.selectedIcon else tab.icon),
                            contentDescription = null
                        )
                    },
                    label = { Text(stringResource(tab.label)) }
                )
            }
        }
    ) {
        when (current) {
            MainTab.LIST -> ListRoute(onOpenItem = onOpenItem, onSignOut = onSignOut)
            MainTab.ADMIN -> AdminPanelRoute()
            MainTab.HISTORY -> HistoryRoute(onOpenPurchase = onOpenPurchase)
            MainTab.ACCOUNT -> AccountRoute()
        }
    }
}
