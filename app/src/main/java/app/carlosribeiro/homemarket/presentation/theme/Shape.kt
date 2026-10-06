package app.carlosribeiro.homemarket.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * iOS corner radii: 12 for fields and buttons (`Brand.controlCornerRadius`), 16 for cards (`brandCard`).
 */
val Shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

/** Height every field and button of a form shares (`Brand.controlHeight`). */
val ControlHeight = 54.dp
