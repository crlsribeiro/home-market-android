package app.carlosribeiro.homemarket.presentation.shopping

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem as MaterialListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import app.carlosribeiro.homemarket.R
import app.carlosribeiro.homemarket.domain.model.ListItem
import app.carlosribeiro.homemarket.presentation.components.SectionHeader
import app.carlosribeiro.homemarket.presentation.components.StatusLabel
import app.carlosribeiro.homemarket.presentation.components.StatusTone
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
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = stringResource(R.string.shopping_percent, (state.progress * PERCENT).roundToInt()),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun SectionTitle(@StringRes title: Int) {
    SectionHeader(stringResource(title))
}

/** iOS `ActiveItemRow`: name, urgent badge, quantity and author, notes, then "Got it" / "Not available". */
@Composable
fun ToGetRow(item: ListItem, onGotIt: () -> Unit, onNotAvailable: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        MaterialListItem(
            headlineContent = { Text(item.name) },
            supportingContent = {
                Column {
                    Text(stringResource(R.string.item_quantity_author, item.quantity, item.addedByName))
                    if (item.notes.isNotEmpty()) {
                        Text(text = item.notes, fontStyle = FontStyle.Italic)
                    }
                }
            },
            trailingContent = if (item.urgent) {
                { StatusLabel(text = stringResource(R.string.item_badge_urgent), tone = StatusTone.ERROR) }
            } else {
                null
            }
        )
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onGotIt,
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text(stringResource(R.string.shopping_got_it))
            }
            OutlinedButton(
                onClick = onNotAvailable,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border = ButtonDefaults.outlinedButtonBorder(enabled = true)
                    .copy(brush = SolidColor(MaterialTheme.colorScheme.error)),
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.shopping_not_available))
            }
        }
    }
}

@Composable
fun NotFoundRow(item: ListItem) {
    MaterialListItem(
        headlineContent = { Text(item.name) },
        supportingContent = {
            Text(stringResource(R.string.item_quantity_author, item.quantity, item.addedByName))
        },
        trailingContent = {
            StatusLabel(text = stringResource(R.string.shopping_notification_sent), tone = StatusTone.WARNING)
        }
    )
}

/** Picked items, struck through; "Undo" puts the item back to buy. */
@Composable
fun PickedRow(item: ListItem, onUndo: () -> Unit) {
    MaterialListItem(
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.ic_check_circle),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        headlineContent = {
            Text(
                text = item.name,
                textDecoration = TextDecoration.LineThrough,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        supportingContent = { Text(stringResource(R.string.shopping_quantity, item.quantity)) },
        trailingContent = { TextButton(onClick = onUndo) { Text(stringResource(R.string.shopping_undo)) } }
    )
}

private const val PERCENT = 100
