@file:OptIn(ExperimentalMaterial3Api::class)

package app.carlosribeiro.homemarket.presentation.list

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.carlosribeiro.homemarket.R
import app.carlosribeiro.homemarket.domain.model.AppUser
import app.carlosribeiro.homemarket.domain.model.ListStatus
import app.carlosribeiro.homemarket.domain.model.WeekList
import app.carlosribeiro.homemarket.presentation.components.InlineTabTopAppBar
import app.carlosribeiro.homemarket.presentation.components.StatusLabel
import app.carlosribeiro.homemarket.presentation.components.StatusTone
import app.carlosribeiro.homemarket.presentation.theme.brandColors

/** iOS header row: avatar, "Hi," and the name, then the list status badge and the week. */
@Composable
fun ListHeader(user: AppUser?, list: WeekList) {
    val locale = LocalConfiguration.current.locales[0]
    val name = user?.displayName.orEmpty()
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Avatar(name)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.list_greeting),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$name 👋",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        ListStatusLabel(list.status)
        Text(
            text = stringResource(R.string.list_week, WeekLabelFormatter.format(list.weekStart, list.weekEnd, locale)),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun Avatar(name: String) {
    val initials = name.split(' ').filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1) }.uppercase()
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials.ifEmpty { "?" },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
private fun ListStatusLabel(status: ListStatus) {
    val (label, tone) = when (status) {
        ListStatus.OPEN -> R.string.list_status_open to StatusTone.PRIMARY
        ListStatus.LOCKED -> R.string.list_status_locked to StatusTone.WARNING
        ListStatus.SHOPPING -> R.string.list_status_shopping to StatusTone.INFO
        ListStatus.CLOSED -> R.string.list_status_closed to StatusTone.NEUTRAL
    }
    StatusLabel(stringResource(label), tone)
}

/** iOS summary cards: total, purchased (green), pending (orange) and, when there are any, urgent (red). */
@Composable
fun SummaryCards(state: ListUiState, modifier: Modifier = Modifier) {
    val brand = brandColors
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SummaryCard(
            R.string.list_summary_total,
            state.items.size,
            MaterialTheme.colorScheme.onSurface,
            Modifier.weight(1f)
        )
        SummaryCard(R.string.list_summary_purchased, state.purchasedCount, brand.success, Modifier.weight(1f))
        SummaryCard(R.string.list_summary_pending, state.pendingCount, brand.warning, Modifier.weight(1f))
        if (state.urgentCount > 0) {
            SummaryCard(R.string.list_summary_urgent, state.urgentCount, brand.danger, Modifier.weight(1f))
        }
    }
}

@Composable
private fun SummaryCard(label: Int, value: Int, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(SummaryShape)
            .background(brandColors.card)
            .border(1.dp, brandColors.cardBorder, SummaryShape)
            .padding(vertical = 10.dp)
            .semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = stringResource(label),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private val SummaryShape = RoundedCornerShape(14.dp)

/** iOS toolbar of the list: sign out on the left, start shopping (admin) and add item on the right. */
@Composable
fun ListTopBar(
    canStartShopping: Boolean,
    canAddItem: Boolean,
    onSignOut: () -> Unit,
    onStartShopping: () -> Unit,
    onAddItem: () -> Unit
) {
    InlineTabTopAppBar(
        title = "",
        navigationIcon = {
            IconButton(onClick = onSignOut) {
                Icon(painterResource(R.drawable.ic_logout), contentDescription = stringResource(R.string.auth_sign_out))
            }
        },
        actions = {
            if (canStartShopping) {
                IconButton(onClick = onStartShopping) {
                    Icon(
                        painterResource(R.drawable.ic_shopping_cart_filled),
                        contentDescription = stringResource(R.string.shopping_start),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            if (canAddItem) {
                IconButton(onClick = onAddItem) {
                    Icon(
                        painterResource(R.drawable.ic_add),
                        contentDescription = stringResource(R.string.add_item_title),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    )
}
