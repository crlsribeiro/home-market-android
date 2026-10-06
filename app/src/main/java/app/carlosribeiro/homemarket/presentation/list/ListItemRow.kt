@file:OptIn(ExperimentalMaterial3Api::class)

package app.carlosribeiro.homemarket.presentation.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.carlosribeiro.homemarket.R
import app.carlosribeiro.homemarket.domain.model.ApprovalStatus
import app.carlosribeiro.homemarket.domain.model.ItemStatus
import app.carlosribeiro.homemarket.domain.model.ListItem
import app.carlosribeiro.homemarket.presentation.components.StatusLabel
import app.carlosribeiro.homemarket.presentation.components.StatusTone
import coil3.compose.AsyncImage

/**
 * iOS `ItemRow` as a Material 3 list item: thumbnail, name, quantity · author (and notes), and the
 * urgent and status labels trailing. The whole row opens the item.
 */
@Composable
fun ListItemRow(item: ListItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ListItem(
        headlineContent = { Text(text = item.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Column {
                Text(text = stringResource(R.string.item_quantity_author, item.quantity, item.addedByName))
                if (item.notes.isNotBlank()) {
                    Text(text = item.notes, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        },
        leadingContent = { ItemThumbnail(item) },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (item.isUrgentToBuy) {
                    StatusLabel(stringResource(R.string.item_badge_urgent), StatusTone.WARNING)
                }
                ItemStatusLabel(item)
            }
        },
        modifier = modifier.clickable(onClick = onClick)
    )
}

@Composable
private fun ItemThumbnail(item: ListItem) {
    Box(
        modifier = Modifier
            .size(ThumbnailSize)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = item.name.take(1).uppercase(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        item.photoUrl?.let { url ->
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(ThumbnailSize)
            )
        }
    }
}

@Composable
private fun ItemStatusLabel(item: ListItem) {
    val (label, tone) = when {
        item.status == ItemStatus.PURCHASED -> R.string.item_badge_purchased to StatusTone.SUCCESS
        item.status == ItemStatus.NOT_FOUND -> R.string.item_badge_not_found to StatusTone.ERROR
        item.approvalStatus == ApprovalStatus.PENDING -> R.string.item_badge_awaiting_approval to StatusTone.WARNING
        else -> R.string.item_badge_pending to StatusTone.WARNING
    }
    StatusLabel(stringResource(label), tone)
}

/**
 * Kept for callers that still pass a color; prefer [StatusLabel]. The color picks the tone: error for
 * urgent / not found, primary for done, tertiary for pending, anything else neutral.
 */
@Composable
fun StatusBadge(text: String, color: Color, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val tone = when (color) {
        colors.error -> StatusTone.ERROR
        colors.primary -> StatusTone.SUCCESS
        colors.tertiary -> StatusTone.WARNING
        else -> StatusTone.NEUTRAL
    }
    StatusLabel(text = text, tone = tone, modifier = modifier)
}

/** Material 3 list item image size. */
private val ThumbnailSize = 56.dp
