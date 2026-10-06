package app.carlosribeiro.homemarket.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Status colors Material 3 has no role for. iOS shows purchased items in green and pending, urgent,
 * locked and awaiting-approval states in orange; these are their tonal palettes (tone 90 container,
 * tone 10 content in light; tone 30 and 90 in dark).
 */
@Immutable
data class StatusColors(
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warningContainer: Color,
    val onWarningContainer: Color
)

val LightStatusColors = StatusColors(
    successContainer = Color(0xFF95F7B9),
    onSuccessContainer = Color(0xFF002110),
    warningContainer = Color(0xFFFFDCBF),
    onWarningContainer = Color(0xFF2D1600)
)

val DarkStatusColors = StatusColors(
    successContainer = Color(0xFF00522F),
    onSuccessContainer = Color(0xFF95F7B9),
    warningContainer = Color(0xFF6A3B00),
    onWarningContainer = Color(0xFFFFDCBF)
)

val LocalStatusColors = staticCompositionLocalOf { LightStatusColors }

/** The status colors of the current theme. */
val statusColors: StatusColors
    @Composable
    @ReadOnlyComposable
    get() = LocalStatusColors.current
