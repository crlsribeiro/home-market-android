package app.carlosribeiro.homemarket.presentation.admin

import android.content.ClipData
import android.content.Intent
import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem as MaterialListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.carlosribeiro.homemarket.R
import app.carlosribeiro.homemarket.domain.model.AppUser
import app.carlosribeiro.homemarket.domain.model.Household
import app.carlosribeiro.homemarket.domain.model.UserRole
import app.carlosribeiro.homemarket.presentation.components.DetailTopAppBar
import app.carlosribeiro.homemarket.presentation.components.ErrorText
import app.carlosribeiro.homemarket.presentation.components.SectionHeader
import app.carlosribeiro.homemarket.presentation.components.StatusLabel
import app.carlosribeiro.homemarket.presentation.components.StatusTone
import app.carlosribeiro.homemarket.presentation.components.TabTopAppBar
import app.carlosribeiro.homemarket.presentation.theme.HomeMarketTheme
import kotlinx.coroutines.launch

@Composable
fun AdminPanelRoute(onBack: (() -> Unit)? = null, viewModel: AdminPanelViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val shareMessage = state.household?.let { stringResource(R.string.admin_share_message, it.name, it.inviteToken) }
    AdminPanelScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        onCopyCode = { code ->
            scope.launch { clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("invite code", code))) }
        },
        onShareCode = {
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareMessage)
            }
            context.startActivity(Intent.createChooser(send, null))
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    state: AdminPanelUiState,
    onEvent: (AdminPanelUiEvent) -> Unit,
    onBack: (() -> Unit)?,
    onCopyCode: (String) -> Unit,
    onShareCode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val adminErrorMessage = state.adminError?.let { stringResource(it.messageRes()) }
    LaunchedEffect(adminErrorMessage) {
        if (adminErrorMessage != null) {
            snackbarHostState.showSnackbar(adminErrorMessage)
            onEvent(AdminPanelUiEvent.DismissAdminError)
        }
    }
    // As a tab the title is large and collapses; pushed from elsewhere it is a small bar with a back arrow.
    val scrollBehavior = if (onBack == null) {
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    } else {
        TopAppBarDefaults.pinnedScrollBehavior()
    }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { AdminTopBar(onBack = onBack, scrollBehavior = scrollBehavior) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp)
        ) {
            SectionHeader(stringResource(R.string.admin_household))
            HouseholdSection(name = state.household?.name.orEmpty(), members = state.members)
            SectionHeader(stringResource(R.string.admin_invite_code))
            InviteCode(state = state, onEvent = onEvent, onCopyCode = onCopyCode, onShareCode = onShareCode)
            state.currentList?.let { list ->
                SectionHeader(stringResource(R.string.admin_list_status_title))
                ListStatusSection(list = list, onEvent = onEvent)
            }
            SectionHeader(stringResource(R.string.admin_pending_approvals))
            PendingApprovalsSection(items = state.pendingApprovals, onEvent = onEvent)
            SectionHeader(stringResource(R.string.admin_items_per_person))
            ItemsPerPersonSection(stats = personStats(state.items))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminTopBar(onBack: (() -> Unit)?, scrollBehavior: TopAppBarScrollBehavior) {
    val title = stringResource(R.string.admin_title)
    if (onBack == null) {
        TabTopAppBar(title = title, scrollBehavior = scrollBehavior)
    } else {
        DetailTopAppBar(title = title, onBack = onBack, scrollBehavior = scrollBehavior)
    }
}

@Composable
private fun HouseholdSection(name: String, members: List<AppUser>) {
    MaterialListItem(
        headlineContent = { Text(name, style = MaterialTheme.typography.titleMedium) }
    )
    members.forEach { member ->
        MaterialListItem(
            headlineContent = { Text(member.displayName.ifEmpty { stringResource(R.string.admin_no_name) }) },
            leadingContent = { MemberAvatar(member.displayName) },
            trailingContent = {
                if (member.role == UserRole.ADMIN) {
                    StatusLabel(text = stringResource(R.string.admin_badge_label), tone = StatusTone.NEUTRAL)
                }
            }
        )
    }
    if (members.size == 1) {
        Text(
            stringResource(R.string.admin_only_member),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

/** The member's initial in a tonal circle, the Material 3 list avatar. */
@Composable
private fun MemberAvatar(name: String) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name.trim().take(1).uppercase().ifEmpty { "?" },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun InviteCode(
    state: AdminPanelUiState,
    onEvent: (AdminPanelUiEvent) -> Unit,
    onCopyCode: (String) -> Unit,
    onShareCode: () -> Unit
) {
    var showRegenerateDialog by rememberSaveable { mutableStateOf(false) }
    val code = state.household?.inviteToken.orEmpty()
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = code,
            style = MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.primary
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconLabelButton(
                icon = R.drawable.ic_content_copy,
                text = stringResource(R.string.admin_copy_code),
                enabled = code.isNotEmpty(),
                onClick = { onCopyCode(code) }
            )
            IconLabelButton(
                icon = R.drawable.ic_share,
                text = stringResource(R.string.admin_share_code),
                enabled = code.isNotEmpty(),
                onClick = onShareCode
            )
        }
        FilledTonalButton(
            onClick = { showRegenerateDialog = true },
            enabled = !state.isRegenerating,
            contentPadding = ButtonDefaults.ButtonWithIconContentPadding
        ) {
            Icon(
                painterResource(R.drawable.ic_refresh),
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize)
            )
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text(stringResource(R.string.admin_new_code))
        }
        Text(
            stringResource(R.string.admin_new_code_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        state.error?.let { ErrorText(stringResource(R.string.auth_error_unknown)) }
    }
    if (showRegenerateDialog) {
        RegenerateCodeDialog(
            onConfirm = {
                showRegenerateDialog = false
                onEvent(AdminPanelUiEvent.RegenerateInviteToken)
            },
            onDismiss = { showRegenerateDialog = false }
        )
    }
}

@Composable
private fun IconLabelButton(@DrawableRes icon: Int, text: String, enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        contentPadding = ButtonDefaults.ButtonWithIconContentPadding
    ) {
        Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
        Text(text)
    }
}

/** A new code invalidates the old one, so the admin confirms first. */
@Composable
private fun RegenerateCodeDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(painterResource(R.drawable.ic_refresh), contentDescription = null) },
        title = { Text(stringResource(R.string.admin_new_code_confirm_title)) },
        text = { Text(stringResource(R.string.admin_new_code_confirm_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.admin_new_code_confirm)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AdminPanelPreview() {
    HomeMarketTheme(dynamicColor = false) {
        AdminPanelScreen(
            state = AdminPanelUiState(
                household = Household("h1", "Casa Silva", "u1", "aB3dE5fG", listOf("u1", "u2")),
                members = listOf(
                    AppUser("u1", "Maria Silva", "maria@example.com", null, "h1", UserRole.ADMIN),
                    AppUser("u2", "João Silva", "joao@example.com", null, "h1", UserRole.MEMBER)
                )
            ),
            onEvent = {},
            onBack = null,
            onCopyCode = {},
            onShareCode = {}
        )
    }
}
