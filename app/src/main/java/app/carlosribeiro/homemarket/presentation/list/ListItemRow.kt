package app.carlosribeiro.homemarket.presentation.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.carlosribeiro.homemarket.R
import app.carlosribeiro.homemarket.domain.model.ApprovalStatus
import app.carlosribeiro.homemarket.domain.model.ItemStatus
import app.carlosribeiro.homemarket.domain.model.ListItem
import app.carlosribeiro.homemarket.presentation.components.BrandCard
import app.carlosribeiro.homemarket.presentation.components.StatusLabel
import app.carlosribeiro.homemarket.presentation.components.StatusTone
import app.carlosribeiro.homemarket.presentation.theme.brandColors
import coil3.compose.AsyncImage

/**
 * iOS `ItemRow` in its card: the thumbnail, the name with the urgent badge, "qty · author" (and the
 * notes), and the status badge trailing. The whole card opens the item.
 */
@Composable
fun ListItemRow(item: ListItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    BrandCard(modifier = modifier, onClick = onClick, contentPadding = PaddingValues(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            ItemThumbnail(item)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (item.isUrgentToBuy) {
                        StatusLabel(stringResource(R.string.item_badge_urgent), StatusTone.WARNING)
                    }
                }
                Text(
                    text = stringResource(R.string.item_quantity_author, item.quantity, item.addedByName),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (item.notes.isNotBlank()) {
                    Text(
                        text = item.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            ItemStatusLabel(item)
        }
    }
}

/** The item photo, or its first letter on the light green tint. */
@Composable
private fun ItemThumbnail(item: ListItem) {
    Box(
        modifier = Modifier
            .size(ThumbnailSize)
            .clip(MaterialTheme.shapes.small)
            .background(brandColors.tint),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = item.name.take(1).uppercase(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
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

/** iOS thumbnail size. */
private val ThumbnailSize = 44.dp
