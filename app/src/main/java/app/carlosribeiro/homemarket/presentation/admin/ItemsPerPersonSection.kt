@file:OptIn(ExperimentalMaterial3Api::class)

package app.carlosribeiro.homemarket.presentation.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.carlosribeiro.homemarket.R
import app.carlosribeiro.homemarket.presentation.components.BrandCard
import app.carlosribeiro.homemarket.presentation.theme.brandColors

/**
 * iOS "Items per person (this week)" card: a bar per person with the count, and each person expands to
 * show the item names (the iOS disclosure groups).
 */
@Composable
fun ItemsPerPersonSection(stats: List<PersonStat>) {
    BrandCard {
        if (stats.isEmpty()) {
            Text(
                text = stringResource(R.string.admin_no_items_this_week),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            val maxCount = stats.maxOf { it.itemCount }
            stats.forEach { stat -> PersonRow(stat = stat, maxCount = maxCount) }
        }
    }
}

@Composable
private fun PersonRow(stat: PersonStat, maxCount: Int) {
    var expanded by rememberSaveable(stat.uid) { mutableStateOf(false) }
    val name = stat.name.ifEmpty { stringResource(R.string.admin_no_name) }
    val clickLabel = stringResource(if (expanded) R.string.admin_hide_items else R.string.admin_show_items)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(role = Role.Button, onClickLabel = clickLabel) { expanded = !expanded },
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = pluralStringResource(R.plurals.admin_person_items, stat.itemCount, name, stat.itemCount),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Icon(
                painterResource(if (expanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
        LinearProgressIndicator(
            progress = { stat.itemCount.toFloat() / maxCount },
            drawStopIndicator = {},
            gapSize = 0.dp,
            trackColor = brandColors.tint,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
        )
        if (expanded) {
            stat.itemNames.forEach { itemName ->
                Text(
                    text = itemName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}
