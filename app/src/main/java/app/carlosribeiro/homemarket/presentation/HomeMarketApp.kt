package app.carlosribeiro.homemarket.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.carlosribeiro.homemarket.presentation.navigation.SignedInNavHost
import app.carlosribeiro.homemarket.presentation.navigation.SignedOutNavHost
import app.carlosribeiro.homemarket.presentation.session.SessionState
import app.carlosribeiro.homemarket.presentation.session.SessionViewModel

/** App root: shows the sign-in flow or the signed-in app depending on the Firebase session. */
@Composable
fun HomeMarketApp(viewModel: SessionViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Surface(modifier = Modifier.fillMaxSize()) {
        when (val session = state) {
            SessionState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            SessionState.SignedOut -> SignedOutNavHost()

            is SessionState.SignedIn -> SignedInNavHost(user = session.user, onSignOut = viewModel::onSignOut)
        }
    }
}
