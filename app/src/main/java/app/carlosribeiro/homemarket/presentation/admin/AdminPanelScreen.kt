@file:OptIn(ExperimentalMaterial3Api::class)

package app.carlosribeiro.homemarket.presentation.admin

import android.content.ClipData
import android.content.Intent
import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.carlosribeiro.homemarket.R
import app.carlosribeiro.homemarket.domain.model.AppUser
import app.carlosribeiro.homemarket.domain.model.Household
import app.carlosribeiro.homemarket.domain.model.UserRole
import app.carlosribeiro.homemarket.presentation.components.BrandButton
import app.carlosribeiro.homemarket.presentation.components.BrandButtonStyle
import app.carlosribeiro.homemarket.presentation.components.BrandCard
import app.carlosribeiro.homemarket.presentation.components.DetailTopAppBar
import app.carlosribeiro.homemarket.presentation.components.ErrorText
import app.carlosribeiro.homemarket.presentation.components.SectionHeader
import app.carlosribeiro.homemarket.presentation.components.StatusLabel
import app.carlosribeiro.homemarket.presentation.components.StatusTone
import app.carlosribeiro.homemarket.presentation.components.TabTopAppBar
import app.carlosribeiro.homemarket.presentation.theme.HomeMarketTheme
import app.carlosribeiro.homemarket.presentation.theme.brandColors
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AdminSection(stringResource(R.string.admin_household)) {
                BrandCard {
                    HouseholdSection(name = state.household?.name.orEmpty(), members = state.members)
                    InviteCode(state = state, onEvent = onEvent, onCopyCode = onCopyCode, onShareCode = onShareCode)
                }
            }
            state.currentList?.let { list ->
                AdminSection(stringResource(R.string.admin_list_status_title)) {
                    BrandCard { ListStatusSection(list = list, onEvent = onEvent) }
                }
            }
            AdminSection(stringResource(R.string.admin_pending_approvals)) {
                PendingApprovalsSection(items = state.pendingApprovals, onEvent = onEvent)
            }
            AdminSection(stringResource(R.string.admin_items_per_person)) {
                ItemsPerPersonSection(stats = personStats(state.items))
            }
        }
    }
}

/** iOS section: the caption, then its cards 10 dp apart. */
@Composable
fun AdminSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(title)
        content()
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
    LabeledRow(label = stringResource(R.string.admin_household_name), value = name)
    members.forEach { member ->
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = member.displayName.ifEmpty { stringResource(R.string.admin_no_name) },
                style = MaterialTheme.typography.bodyLarge
            )
            if (member.role == UserRole.ADMIN) {
                StatusLabel(text = stringResource(R.string.admin_badge_label), tone = StatusTone.PRIMARY)
            }
        }
    }
    if (members.size == 1) {
        Text(
            stringResource(R.string.admin_only_member),
            style = MaterialTheme.typography.bodySmall,
            color = brandColors.warning
        )
    }
}

/** A label with its value trailing (iOS `LabeledContent`). */
@Composable
fun LabeledRow(label: String, value: String) {
    Row(
        modifier = Modifier.semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** The code in the brand color with copy and share, then "Generate new invite code" (tint button). */
@Composable
private fun InviteCode(
    state: AdminPanelUiState,
    onEvent: (AdminPanelUiEvent) -> Unit,
    onCopyCode: (String) -> Unit,
    onShareCode: () -> Unit
) {
    var showRegenerateDialog by rememberSaveable { mutableStateOf(false) }
    var copied by rememberSaveable(state.household?.inviteToken) { mutableStateOf(false) }
    val code = state.household?.inviteToken.orEmpty()
    if (code.isNotEmpty()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SelectionContainer(Modifier.weight(1f)) {
                Text(
                    text = code,
                    style = MaterialTheme.typography.bodyLarge,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = {
                onCopyCode(code)
                copied = true
            }) {
                Icon(
                    painterResource(if (copied) R.drawable.ic_check else R.drawable.ic_content_copy),
                    contentDescription = stringResource(R.string.admin_copy_code),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onShareCode) {
                Icon(
                    painterResource(R.drawable.ic_share),
                    contentDescription = stringResource(R.string.admin_share_code),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
    BrandButton(
        text = stringResource(R.string.admin_new_code),
        onClick = { showRegenerateDialog = true },
        style = BrandButtonStyle.TINT,
        icon = R.drawable.ic_link,
        isLoading = state.isRegenerating
    )
    state.error?.let { ErrorText(stringResource(R.string.auth_error_unknown)) }
    if (showRegenerateDialog) {
        RegenerateCodeDialog(
            onConfirm = {
                showRegenerateDialog = false
                copied = false
                onEvent(AdminPanelUiEvent.RegenerateInviteToken)
            },
            onDismiss = { showRegenerateDialog = false }
        )
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
