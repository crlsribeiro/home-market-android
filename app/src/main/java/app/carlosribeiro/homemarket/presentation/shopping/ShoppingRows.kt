@file:OptIn(ExperimentalMaterial3Api::class)

package app.carlosribeiro.homemarket.presentation.shopping

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import app.carlosribeiro.homemarket.R
import app.carlosribeiro.homemarket.domain.model.ListItem
import app.carlosribeiro.homemarket.presentation.components.BrandButton
import app.carlosribeiro.homemarket.presentation.components.BrandButtonStyle
import app.carlosribeiro.homemarket.presentation.components.BrandCard
import app.carlosribeiro.homemarket.presentation.components.SectionHeader
import app.carlosribeiro.homemarket.presentation.components.StatusLabel
import app.carlosribeiro.homemarket.presentation.components.StatusTone
import app.carlosribeiro.homemarket.presentation.list.WeekLabelFormatter
import app.carlosribeiro.homemarket.presentation.theme.brandColors
import kotlin.math.roundToInt

/** iOS progress card: the week, "x of y items", the percentage and the bar. Shared with the waiting screen. */
@Composable
fun ProgressCard(state: ShoppingUiState, modifier: Modifier = Modifier) {
    val locale = LocalConfiguration.current.locales[0]
    BrandCard(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        state.currentList?.let { list ->
            Text(
                text = stringResource(
                    R.string.list_week,
                    WeekLabelFormatter.format(list.weekStart, list.weekEnd, locale)
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.shopping_progress, state.picked.size, state.items.size),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = stringResource(R.string.shopping_percent, (state.progress * PERCENT).roundToInt()),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        LinearProgressIndicator(
            progress = { state.progress },
            drawStopIndicator = {},
            gapSize = 0.dp,
            trackColor = brandColors.tint,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun SectionTitle(@StringRes title: Int) {
    SectionHeader(stringResource(title))
}

/** iOS `ActiveItemRow` card: name and urgent badge, quantity and author, notes, then Got it / Not available. */
@Composable
fun ToGetRow(item: ListItem, onGotIt: () -> Unit, onNotAvailable: () -> Unit, modifier: Modifier = Modifier) {
    BrandCard(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text = item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (item.urgent) {
                    StatusLabel(text = stringResource(R.string.item_badge_urgent), tone = StatusTone.WARNING)
                }
            }
            Text(
                text = stringResource(R.string.item_quantity_author, item.quantity, item.addedByName),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (item.notes.isNotEmpty()) {
                Text(
                    text = item.notes,
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BrandButton(
                text = stringResource(R.string.shopping_got_it),
                onClick = onGotIt,
                icon = R.drawable.ic_check,
                modifier = Modifier.weight(1f)
            )
            BrandButton(
                text = stringResource(R.string.shopping_not_available),
                onClick = onNotAvailable,
                style = BrandButtonStyle.DESTRUCTIVE,
                icon = R.drawable.ic_close,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/** iOS not-found card, faded: name, quantity and author, and "notification sent" in orange. */
@Composable
fun NotFoundRow(item: ListItem, modifier: Modifier = Modifier) {
    BrandCard(modifier = modifier.alpha(Fade.NOT_FOUND), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = item.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        Text(
            text = stringResource(R.string.item_quantity_author, item.quantity, item.addedByName),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.shopping_notification_sent),
            style = MaterialTheme.typography.bodySmall,
            color = brandColors.warning
        )
    }
}

/** iOS picked card, faded and struck through; "Undo" puts the item back to buy. */
@Composable
fun PickedRow(item: ListItem, onUndo: () -> Unit, modifier: Modifier = Modifier) {
    BrandCard(
        modifier = modifier,
        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 8.dp, bottom = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .alpha(Fade.PICKED),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_check_circle),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodyLarge,
                        textDecoration = TextDecoration.LineThrough
                    )
                    Text(
                        text = stringResource(R.string.shopping_quantity, item.quantity),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            TextButton(onClick = onUndo) { Text(stringResource(R.string.shopping_undo)) }
        }
    }
}

private const val PERCENT = 100

/** iOS opacities of the not found and picked cards. */
private object Fade {
    const val NOT_FOUND = 0.75f
    const val PICKED = 0.5f
}
