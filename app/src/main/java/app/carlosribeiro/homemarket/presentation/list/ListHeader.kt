@file:OptIn(ExperimentalMaterial3Api::class)

package app.carlosribeiro.homemarket.presentation.list

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.carlosribeiro.homemarket.R
import app.carlosribeiro.homemarket.domain.model.AppUser
import app.carlosribeiro.homemarket.domain.model.ListStatus
import app.carlosribeiro.homemarket.domain.model.WeekList
import app.carlosribeiro.homemarket.presentation.components.BrandCard
import app.carlosribeiro.homemarket.presentation.components.BrandFieldLabel
import app.carlosribeiro.homemarket.presentation.components.SectionHeader
import app.carlosribeiro.homemarket.presentation.components.StatusLabel
import app.carlosribeiro.homemarket.presentation.components.StatusTone
import app.carlosribeiro.homemarket.presentation.theme.brandColors

/** The bold "List" title on the same row as the overflow menu, which holds "Sign out". */
@Composable
fun ListTopBar(onSignOut: () -> Unit, scrollBehavior: TopAppBarScrollBehavior) {
    var showMenu by remember { mutableStateOf(false) }
    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.tab_list),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        },
        scrollBehavior = scrollBehavior,
        actions = {
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        painterResource(R.drawable.ic_more_vert),
                        contentDescription = stringResource(R.string.list_more)
                    )
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.auth_sign_out)) },
                        leadingIcon = { Icon(painterResource(R.drawable.ic_logout), contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onSignOut()
                        }
                    )
                }
            }
        }
    )
}

/** Greeting card: the avatar, "Hi, welcome back" and the name, then the list status badge and the week. */
@Composable
fun ListHeader(user: AppUser?, list: WeekList) {
    val locale = LocalConfiguration.current.locales[0]
    val name = user?.displayName.orEmpty()
    BrandCard(color = brandColors.mutedCard) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(name)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.list_greeting),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$name 👋",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ListStatusLabel(list.status)
                Text(
                    text = stringResource(
                        R.string.list_week,
                        WeekLabelFormatter.format(list.weekStart, list.weekEnd, locale)
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun Avatar(name: String) {
    val initials = name.split(' ').filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1) }.uppercase()
    Box(
        modifier = Modifier
            .size(44.dp)
            .border(2.dp, brandColors.card, CircleShape)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials.ifEmpty { "?" },
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer
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

/**
 * "This week" panel: the purchased progress, then white tiles with the total, purchased (green), pending
 * (orange) and, when there are any, urgent (red) counts.
 */
@Composable
fun SummaryCards(state: ListUiState, modifier: Modifier = Modifier) {
    val total = state.items.size
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(PanelShape)
            .background(brandColors.mutedPanel)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(
                painterResource(R.drawable.ic_analytics),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            BrandFieldLabel(stringResource(R.string.list_this_week_short), modifier = Modifier.weight(1f))
            Text(
                text = stringResource(R.string.list_purchased_progress, state.purchasedCount, total),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        LinearProgressIndicator(
            progress = { if (total == 0) 0f else state.purchasedCount.toFloat() / total },
            drawStopIndicator = {},
            gapSize = 0.dp,
            trackColor = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
        )
        StatTiles(state)
    }
}

@Composable
private fun StatTiles(state: ListUiState) {
    val brand = brandColors
    val onSurface = MaterialTheme.colorScheme.onSurface
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatTile(R.string.list_summary_total, state.items.size, onSurface, Modifier.weight(1f))
        StatTile(R.string.list_summary_purchased, state.purchasedCount, brand.success, Modifier.weight(1f))
        StatTile(R.string.list_summary_pending, state.pendingCount, brand.warning, Modifier.weight(1f))
        if (state.urgentCount > 0) {
            StatTile(R.string.list_summary_urgent, state.urgentCount, brand.danger, Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatTile(label: Int, value: Int, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .background(brandColors.card)
            .padding(vertical = 14.dp)
            .semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
        Text(
            text = stringResource(label),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** A section title with an optional count on the right ("1 item"). */
@Composable
fun SectionTitleRow(title: String, count: Int?, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(PaddingValues(end = 4.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SectionHeader(title, modifier = Modifier.weight(1f))
        count?.let {
            Text(
                text = pluralStringResource(R.plurals.list_item_count, it, it),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** The admin's full-width pill "Start shopping" (closes the list for edits and opens shopping mode). */
@Composable
fun StartShoppingButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        shape = CircleShape,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
    ) {
        Icon(painterResource(R.drawable.ic_shopping_cart), contentDescription = null, modifier = Modifier.size(20.dp))
        Text(
            text = stringResource(R.string.shopping_start),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

private val PanelShape = RoundedCornerShape(24.dp)
