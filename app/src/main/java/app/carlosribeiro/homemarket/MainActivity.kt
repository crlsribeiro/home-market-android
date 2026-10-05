package app.carlosribeiro.homemarket

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.carlosribeiro.homemarket.presentation.HomeMarketApp
import app.carlosribeiro.homemarket.presentation.theme.HomeMarketTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HomeMarketTheme {
                HomeMarketApp()
            }
        }
    }
}
