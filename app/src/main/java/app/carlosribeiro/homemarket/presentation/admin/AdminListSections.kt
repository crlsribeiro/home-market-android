package app.carlosribeiro.homemarket.presentation.admin

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.carlosribeiro.homemarket.R
import app.carlosribeiro.homemarket.domain.model.AdminError
import app.carlosribeiro.homemarket.domain.model.ListItem
import app.carlosribeiro.homemarket.domain.model.ListStatus
import app.carlosribeiro.homemarket.domain.model.WeekList
import app.carlosribeiro.homemarket.presentation.list.WeekLabelFormatter

/** iOS "List status" card plus the web lock and reopen actions. */
@Composable
fun ListStatusSection(list: WeekList, onEvent: (AdminPanelUiEvent) -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    LabeledRow(
        label = stringResource(R.string.item_detail_week),
        value = WeekLabelFormatter.format(list.weekStart, list.weekEnd, locale)
    )
    LabeledRow(label = stringResource(R.string.admin_list_status), value = stringResource(list.status.labelRes()))
    when (list.status) {
        ListStatus.OPEN -> {
            Text(
                text = stringResource(R.string.admin_lock_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FilledTonalButton(onClick = { onEvent(AdminPanelUiEvent.LockList) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.admin_lock_list))
            }
        }

        ListStatus.LOCKED -> OutlinedButton(
            onClick = { onEvent(AdminPanelUiEvent.ReopenList) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.admin_reopen_list))
        }

        ListStatus.SHOPPING, ListStatus.CLOSED -> Unit
    }
}

/** iOS "Pending approvals": approve keeps the item to buy, reject moves it to next week. */
@Composable
fun PendingApprovalsSection(items: List<ListItem>, onEvent: (AdminPanelUiEvent) -> Unit) {
    if (items.isEmpty()) {
        Text(
            text = stringResource(R.string.admin_no_pending_approvals),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items.forEach { item ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = item.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                TextButton(onClick = { onEvent(AdminPanelUiEvent.Approve(item)) }) {
                    Text(stringResource(R.string.admin_approve))
                }
                TextButton(onClick = { onEvent(AdminPanelUiEvent.Reject(item)) }) {
                    Text(stringResource(R.string.admin_reject), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun LabeledRow(label: String, value: String) {
    Row {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

@StringRes
private fun ListStatus.labelRes(): Int = when (this) {
    ListStatus.OPEN -> R.string.admin_status_open
    ListStatus.LOCKED -> R.string.admin_status_locked
    ListStatus.SHOPPING -> R.string.admin_status_shopping
    ListStatus.CLOSED -> R.string.admin_status_closed
}

@StringRes
fun AdminError.messageRes(): Int = when (this) {
    AdminError.NOT_ADMIN -> R.string.list_error_not_admin
    AdminError.INVALID_STATUS -> R.string.admin_error_invalid_status
    AdminError.NETWORK -> R.string.auth_error_network
    AdminError.NOT_SIGNED_IN, AdminError.UNKNOWN -> R.string.auth_error_unknown
}
