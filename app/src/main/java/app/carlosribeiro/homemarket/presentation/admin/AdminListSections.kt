@file:OptIn(ExperimentalMaterial3Api::class)

package app.carlosribeiro.homemarket.presentation.admin

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.carlosribeiro.homemarket.R
import app.carlosribeiro.homemarket.domain.model.AdminError
import app.carlosribeiro.homemarket.domain.model.ListItem
import app.carlosribeiro.homemarket.domain.model.ListStatus
import app.carlosribeiro.homemarket.domain.model.WeekList
import app.carlosribeiro.homemarket.presentation.components.BrandButton
import app.carlosribeiro.homemarket.presentation.components.BrandButtonStyle
import app.carlosribeiro.homemarket.presentation.components.BrandCard
import app.carlosribeiro.homemarket.presentation.list.WeekLabelFormatter

/** iOS "List status" card (week, status, how to start shopping) plus the web lock and reopen actions. */
@Composable
fun ListStatusSection(list: WeekList, onEvent: (AdminPanelUiEvent) -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    LabeledRow(
        label = stringResource(R.string.item_detail_week),
        value = WeekLabelFormatter.format(list.weekStart, list.weekEnd, locale)
    )
    LabeledRow(label = stringResource(R.string.admin_list_status), value = stringResource(list.status.labelRes()))
    Text(
        text = stringResource(R.string.admin_start_shopping_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    when (list.status) {
        ListStatus.OPEN -> {
            Text(
                text = stringResource(R.string.admin_lock_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            BrandButton(
                text = stringResource(R.string.admin_lock_list),
                onClick = { onEvent(AdminPanelUiEvent.LockList) },
                style = BrandButtonStyle.TINT,
                icon = R.drawable.ic_lock
            )
        }

        ListStatus.LOCKED -> BrandButton(
            text = stringResource(R.string.admin_reopen_list),
            onClick = { onEvent(AdminPanelUiEvent.ReopenList) },
            style = BrandButtonStyle.OUTLINE
        )

        ListStatus.SHOPPING, ListStatus.CLOSED -> Unit
    }
}

/** iOS "Pending approvals": a card per item with Approve and Reject (reject moves it to next week). */
@Composable
fun PendingApprovalsSection(items: List<ListItem>, onEvent: (AdminPanelUiEvent) -> Unit) {
    if (items.isEmpty()) {
        BrandCard {
            Text(
                text = stringResource(R.string.admin_no_pending_approvals),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }
    items.forEach { item ->
        val approveLabel = stringResource(R.string.admin_approve_item, item.name)
        val rejectLabel = stringResource(R.string.admin_reject_item, item.name)
        BrandCard(contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 8.dp, bottom = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = item.name, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = stringResource(R.string.item_quantity_author, item.quantity, item.addedByName),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(
                    onClick = { onEvent(AdminPanelUiEvent.Approve(item)) },
                    modifier = Modifier.semantics { contentDescription = approveLabel }
                ) {
                    Text(stringResource(R.string.admin_approve))
                }
                TextButton(
                    onClick = { onEvent(AdminPanelUiEvent.Reject(item)) },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.semantics { contentDescription = rejectLabel }
                ) {
                    Text(stringResource(R.string.admin_reject))
                }
            }
        }
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
