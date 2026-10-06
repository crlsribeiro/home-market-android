package app.carlosribeiro.homemarket.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** The primary form button (iOS `BrandPrimaryButtonStyle`) that shows a spinner while [isLoading]. */
@Composable
fun SubmitButton(
    text: String,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    BrandButton(text = text, onClick = onClick, modifier = modifier, enabled = enabled, isLoading = isLoading)
}
