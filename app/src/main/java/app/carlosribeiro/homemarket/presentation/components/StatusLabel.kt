package app.carlosribeiro.homemarket.presentation.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.carlosribeiro.homemarket.presentation.theme.statusColors

/** Tone of a [StatusLabel], mapped to container roles (iOS: green for done, orange for pending/urgent). */
enum class StatusTone { NEUTRAL, SUCCESS, WARNING, ERROR }

/** Small read-only status label (like a M3 badge with text), for item and list states. */
@Composable
fun StatusLabel(text: String, tone: StatusTone, modifier: Modifier = Modifier) {
    val (container, content) = tone.colors()
    Surface(color = container, contentColor = content, shape = RoundedCornerShape(8.dp), modifier = modifier) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun StatusTone.colors(): Pair<Color, Color> = when (this) {
    StatusTone.NEUTRAL -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    StatusTone.SUCCESS -> statusColors.successContainer to statusColors.onSuccessContainer
    StatusTone.WARNING -> statusColors.warningContainer to statusColors.onWarningContainer
    StatusTone.ERROR -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
}
