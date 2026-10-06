package app.carlosribeiro.homemarket.presentation.admin

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem as MaterialListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.carlosribeiro.homemarket.R
import app.carlosribeiro.homemarket.domain.model.AdminError
import app.carlosribeiro.homemarket.domain.model.ListItem
import app.carlosribeiro.homemarket.domain.model.ListStatus
import app.carlosribeiro.homemarket.domain.model.WeekList
import app.carlosribeiro.homemarket.presentation.components.StatusLabel
import app.carlosribeiro.homemarket.presentation.components.StatusTone
import app.carlosribeiro.homemarket.presentation.list.WeekLabelFormatter

/** iOS "List status" card plus the web lock and reopen actions. */
@Composable
fun ListStatusSection(list: WeekList, onEvent: (AdminPanelUiEvent) -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    MaterialListItem(
        headlineContent = { Text(stringResource(R.string.item_detail_week)) },
        trailingContent = {
            Text(
                text = WeekLabelFormatter.format(list.weekStart, list.weekEnd, locale),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    )
    MaterialListItem(
        headlineContent = { Text(stringResource(R.string.admin_list_status)) },
        trailingContent = { StatusLabel(text = stringResource(list.status.labelRes()), tone = list.status.tone()) }
    )
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when (list.status) {
            ListStatus.OPEN -> {
                Text(
                    text = stringResource(R.string.admin_lock_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FilledTonalButton(
                    onClick = { onEvent(AdminPanelUiEvent.LockList) },
                    modifier = Modifier.fillMaxWidth()
                ) {
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
}

/** iOS "Pending approvals": approve keeps the item to buy, reject moves it to next week. */
@Composable
fun PendingApprovalsSection(items: List<ListItem>, onEvent: (AdminPanelUiEvent) -> Unit) {
    if (items.isEmpty()) {
        Text(
            text = stringResource(R.string.admin_no_pending_approvals),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        return
    }
    items.forEach { item ->
        MaterialListItem(
            headlineContent = { Text(item.name) },
            supportingContent = {
                Text(stringResource(R.string.item_quantity_author, item.quantity, item.addedByName))
            },
            trailingContent = {
                Row {
                    IconButton(
                        onClick = { onEvent(AdminPanelUiEvent.Approve(item)) },
                        colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_check),
                            contentDescription = stringResource(R.string.admin_approve_item, item.name)
                        )
                    }
                    IconButton(
                        onClick = { onEvent(AdminPanelUiEvent.Reject(item)) },
                        colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_close),
                            contentDescription = stringResource(R.string.admin_reject_item, item.name)
                        )
                    }
                }
            }
        )
    }
}

@StringRes
private fun ListStatus.labelRes(): Int = when (this) {
    ListStatus.OPEN -> R.string.admin_status_open
    ListStatus.LOCKED -> R.string.admin_status_locked
    ListStatus.SHOPPING -> R.string.admin_status_shopping
    ListStatus.CLOSED -> R.string.admin_status_closed
}

/** iOS: orange while locked (new items wait for approval). */
private fun ListStatus.tone(): StatusTone = when (this) {
    ListStatus.OPEN -> StatusTone.SUCCESS
    ListStatus.LOCKED -> StatusTone.WARNING
    ListStatus.SHOPPING, ListStatus.CLOSED -> StatusTone.NEUTRAL
}

@StringRes
fun AdminError.messageRes(): Int = when (this) {
    AdminError.NOT_ADMIN -> R.string.list_error_not_admin
    AdminError.INVALID_STATUS -> R.string.admin_error_invalid_status
    AdminError.NETWORK -> R.string.auth_error_network
    AdminError.NOT_SIGNED_IN, AdminError.UNKNOWN -> R.string.auth_error_unknown
}
