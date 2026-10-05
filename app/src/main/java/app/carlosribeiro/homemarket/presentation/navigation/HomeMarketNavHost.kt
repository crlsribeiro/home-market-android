package app.carlosribeiro.homemarket.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.carlosribeiro.homemarket.domain.model.AppUser
import app.carlosribeiro.homemarket.presentation.auth.LoginRoute
import app.carlosribeiro.homemarket.presentation.auth.RegisterRoute
import app.carlosribeiro.homemarket.presentation.item.ItemDetailRoute
import app.carlosribeiro.homemarket.presentation.main.MainScaffold

/** Screens for a signed-out user. Signing in swaps this graph for [SignedInNavHost]. */
@Composable
fun SignedOutNavHost(modifier: Modifier = Modifier, navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = LoginDestination, modifier = modifier) {
        composable<LoginDestination> {
            LoginRoute(onCreateAccount = { navController.navigate(RegisterDestination) })
        }
        composable<RegisterDestination> {
            RegisterRoute(onBack = { navController.popBackStack() })
        }
    }
}

/** Screens for a signed-in user with a household. Item detail (M3) and shopping mode (M5) join this graph. */
@Composable
fun SignedInNavHost(
    user: AppUser,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(navController = navController, startDestination = MainDestination, modifier = modifier) {
        composable<MainDestination> {
            MainScaffold(
                user = user,
                onSignOut = onSignOut,
                onOpenItem = { itemId -> navController.navigate(ItemDetailDestination(itemId)) }
            )
        }
        composable<ItemDetailDestination> {
            ItemDetailRoute(onBack = { navController.popBackStack() })
        }
    }
}
