package app.carlosribeiro.homemarket.presentation.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    val maxCount = stats.maxOf { it.itemCount }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        stats.forEach { stat -> PersonRow(stat = stat, maxCount = maxCount) }
    }
}

@Composable
private fun PersonRow(stat: PersonStat, maxCount: Int) {
    var expanded by rememberSaveable(stat.uid) { mutableStateOf(false) }
    val name = stat.name.ifEmpty { stringResource(R.string.admin_no_name) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.admin_show_items)) {
                expanded = !expanded
            }
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = pluralStringResource(R.plurals.admin_person_items, stat.itemCount, name, stat.itemCount),
            style = MaterialTheme.typography.bodyMedium
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(stat.itemCount.toFloat() / maxCount)
                        .height(20.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
            Text(
                text = stat.itemCount.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(28.dp)
            )
        }
        if (expanded) {
            stat.itemNames.forEach { itemName ->
                Text(
                    text = itemName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
        }
    }
}
