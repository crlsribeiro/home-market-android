package app.carlosribeiro.homemarket.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.carlosribeiro.homemarket.domain.model.AppUser
import app.carlosribeiro.homemarket.presentation.admin.AdminPanelRoute
import app.carlosribeiro.homemarket.presentation.auth.LoginRoute
import app.carlosribeiro.homemarket.presentation.auth.RegisterRoute
import app.carlosribeiro.homemarket.presentation.home.HomeRoute

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

/** Screens for a signed-in user. */
@Composable
fun SignedInNavHost(
    user: AppUser,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(navController = navController, startDestination = HomeDestination, modifier = modifier) {
        composable<HomeDestination> {
            HomeRoute(
                user = user,
                onSignOut = onSignOut,
                onOpenAdmin = { navController.navigate(AdminPanelDestination) }
            )
        }
        composable<AdminPanelDestination> {
            AdminPanelRoute(onBack = { navController.popBackStack() })
        }
    }
}
