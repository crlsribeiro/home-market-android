package app.carlosribeiro.homemarket.presentation.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.carlosribeiro.homemarket.presentation.theme.brandColors

/** iOS `BrandBadge`: a small capsule with bold text in the status color on a 15 % tint of it. */
@Composable
fun StatusLabel(text: String, tone: StatusTone, modifier: Modifier = Modifier) {
    val color = tone.color()
    Surface(
        color = color.copy(alpha = Badge.TINT_ALPHA),
        contentColor = color,
        shape = CircleShape,
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun StatusTone.color(): Color = when (this) {
    StatusTone.PRIMARY -> MaterialTheme.colorScheme.primary
    StatusTone.SUCCESS -> brandColors.success
    StatusTone.WARNING -> brandColors.warning
    StatusTone.ERROR -> brandColors.danger
    StatusTone.INFO -> brandColors.info
    StatusTone.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
}

/** Opacity of the badge tint behind the text (iOS `color.opacity(0.15)`). */
private object Badge {
    const val TINT_ALPHA = 0.15f
}
