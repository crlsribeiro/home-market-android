@file:OptIn(ExperimentalMaterial3Api::class)

package app.carlosribeiro.homemarket.presentation.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem as MaterialListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.carlosribeiro.homemarket.R

/** iOS "Items per person (this week)": a horizontal bar per person, tap to see the item names. */
@Composable
fun ItemsPerPersonSection(stats: List<PersonStat>) {
    if (stats.isEmpty()) {
        Text(
            text = stringResource(R.string.admin_no_items_this_week),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        return
    }
    val maxCount = stats.maxOf { it.itemCount }
    stats.forEach { stat -> PersonRow(stat = stat, maxCount = maxCount) }
}

@Composable
private fun PersonRow(stat: PersonStat, maxCount: Int) {
    var expanded by rememberSaveable(stat.uid) { mutableStateOf(false) }
    val name = stat.name.ifEmpty { stringResource(R.string.admin_no_name) }
    val clickLabel = stringResource(if (expanded) R.string.admin_hide_items else R.string.admin_show_items)
    MaterialListItem(
        modifier = Modifier.clickable(role = Role.Button, onClickLabel = clickLabel) { expanded = !expanded },
        headlineContent = {
            Text(pluralStringResource(R.plurals.admin_person_items, stat.itemCount, name, stat.itemCount))
        },
        supportingContent = {
            LinearProgressIndicator(
                progress = { stat.itemCount.toFloat() / maxCount },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        },
        trailingContent = {
            Icon(
                painterResource(if (expanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more),
                contentDescription = null
            )
        }
    )
    if (expanded) {
        Column(
            modifier = Modifier.padding(start = 32.dp, end = 16.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            stat.itemNames.forEach { itemName ->
                Text(
                    text = itemName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
