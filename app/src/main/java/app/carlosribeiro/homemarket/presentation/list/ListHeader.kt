package app.carlosribeiro.homemarket.presentation.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.carlosribeiro.homemarket.R
import app.carlosribeiro.homemarket.domain.model.AppUser
import app.carlosribeiro.homemarket.domain.model.ListStatus
import app.carlosribeiro.homemarket.domain.model.WeekList
import app.carlosribeiro.homemarket.presentation.components.StatusLabel
import app.carlosribeiro.homemarket.presentation.components.StatusTone

/** iOS header row: avatar, greeting, list status badge and week. */
@Composable
fun ListHeader(user: AppUser?, list: WeekList) {
    val locale = LocalConfiguration.current.locales[0]
    val name = user?.displayName.orEmpty()
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Avatar(name)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.list_greeting),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$name 👋",
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
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

@Composable
private fun Avatar(name: String) {
    val initials = name.split(' ').filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1) }.uppercase()
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials.ifEmpty { "?" },
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun ListStatusLabel(status: ListStatus) {
    val (label, tone) = when (status) {
        ListStatus.OPEN -> R.string.list_status_open to StatusTone.SUCCESS
        ListStatus.LOCKED -> R.string.list_status_locked to StatusTone.WARNING
        ListStatus.SHOPPING -> R.string.list_status_shopping to StatusTone.NEUTRAL
        ListStatus.CLOSED -> R.string.list_status_closed to StatusTone.NEUTRAL
    }
    StatusLabel(stringResource(label), tone)
}

@Composable
fun SummaryCards(state: ListUiState) {
    val colors = MaterialTheme.colorScheme
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SummaryCard(R.string.list_summary_total, state.items.size, colors.onSurface, Modifier.weight(1f))
        SummaryCard(R.string.list_summary_purchased, state.purchasedCount, colors.primary, Modifier.weight(1f))
        SummaryCard(R.string.list_summary_pending, state.pendingCount, colors.tertiary, Modifier.weight(1f))
        if (state.urgentCount > 0) {
            SummaryCard(R.string.list_summary_urgent, state.urgentCount, colors.error, Modifier.weight(1f))
        }
    }
}

@Composable
private fun SummaryCard(label: Int, value: Int, color: Color, modifier: Modifier = Modifier) {
    OutlinedCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value.toString(), style = MaterialTheme.typography.titleLarge, color = color)
            Text(
                text = stringResource(label),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
