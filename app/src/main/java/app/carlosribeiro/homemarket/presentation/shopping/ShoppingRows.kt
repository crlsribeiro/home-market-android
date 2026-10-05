package app.carlosribeiro.homemarket.presentation.shopping

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import app.carlosribeiro.homemarket.R
import app.carlosribeiro.homemarket.domain.model.ListItem
import app.carlosribeiro.homemarket.presentation.list.StatusBadge
import app.carlosribeiro.homemarket.presentation.list.WeekLabelFormatter
import kotlin.math.roundToInt

/** Week, "x of y items", percentage and progress bar. Shared by the admin and the waiting screen. */
@Composable
fun ProgressCard(state: ShoppingUiState, modifier: Modifier = Modifier) {
    val locale = LocalConfiguration.current.locales[0]
    OutlinedCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            state.currentList?.let { list ->
                Text(
                    text = stringResource(
                        R.string.list_week,
                        WeekLabelFormatter.format(list.weekStart, list.weekEnd, locale)
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.shopping_progress, state.picked.size, state.items.size),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = stringResource(R.string.shopping_percent, (state.progress * PERCENT).roundToInt()),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun SectionTitle(@StringRes title: Int) {
    Text(
        text = stringResource(title),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp)
    )
}

/** iOS `ActiveItemRow`: name, urgent badge, quantity and author, notes, then "Got it" / "Not available". */
@Composable
fun ToGetRow(item: ListItem, onGotIt: () -> Unit, onNotAvailable: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text = item.name, style = MaterialTheme.typography.titleMedium)
                if (item.urgent) {
                    StatusBadge(
                        stringResource(R.string.item_badge_urgent),
                        MaterialTheme.colorScheme.error
                    )
                }
            }
            QuantityAuthor(item)
            if (item.notes.isNotEmpty()) {
                Text(
                    text = item.notes,
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onGotIt, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.shopping_got_it))
                }
                Button(
                    onClick = onNotAvailable,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.shopping_not_available))
                }
            }
        }
    }
}

@Composable
fun NotFoundRow(item: ListItem) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = item.name, style = MaterialTheme.typography.titleSmall)
            QuantityAuthor(item)
            Text(
                text = stringResource(R.string.shopping_notification_sent),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

/** Picked items, struck through; "Undo" puts the item back to buy. */
@Composable
fun PickedRow(item: ListItem, onUndo: () -> Unit) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_check_circle),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .alpha(alpha = 0.6f)
            ) {
                Text(text = item.name, textDecoration = TextDecoration.LineThrough)
                Text(
                    text = stringResource(R.string.shopping_quantity, item.quantity),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onUndo) { Text(stringResource(R.string.shopping_undo)) }
        }
    }
}

@Composable
private fun QuantityAuthor(item: ListItem) {
    Text(
        text = stringResource(R.string.item_quantity_author, item.quantity, item.addedByName),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

private const val PERCENT = 100
