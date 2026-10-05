package app.carlosribeiro.homemarket.presentation.admin

import android.content.ClipData
import android.content.Intent
import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
import app.carlosribeiro.homemarket.presentation.components.ErrorText
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

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.admin_title)) },
                navigationIcon = {
                    onBack?.let { TextButton(onClick = it) { Text(stringResource(R.string.action_back)) } }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionCard(title = stringResource(R.string.admin_household)) {
                Text(state.household?.name.orEmpty(), style = MaterialTheme.typography.titleLarge)
                MembersList(state.members)
            }
            SectionCard(title = stringResource(R.string.admin_invite_code)) {
                InviteCode(state = state, onEvent = onEvent, onCopyCode = onCopyCode, onShareCode = onShareCode)
            }
            state.currentList?.let { list ->
                SectionCard(title = stringResource(R.string.admin_list_status_title)) {
                    ListStatusSection(list = list, onEvent = onEvent)
                }
            }
            SectionCard(title = stringResource(R.string.admin_pending_approvals)) {
                PendingApprovalsSection(items = state.pendingApprovals, onEvent = onEvent)
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                title.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            content()
        }
    }
}

@Composable
private fun MembersList(members: List<AppUser>) {
    members.forEach { member ->
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(member.displayName, style = MaterialTheme.typography.bodyLarge)
            if (member.role == UserRole.ADMIN) {
                Text(
                    stringResource(R.string.admin_badge),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
    if (members.size == 1) {
        Text(
            stringResource(R.string.admin_only_member),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.tertiary
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
    val code = state.household?.inviteToken.orEmpty()
    Text(
        text = code,
        style = MaterialTheme.typography.headlineSmall,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { onCopyCode(code) }, enabled = code.isNotEmpty()) {
            Text(stringResource(R.string.admin_copy_code))
        }
        OutlinedButton(onClick = onShareCode, enabled = code.isNotEmpty()) {
            Text(stringResource(R.string.admin_share_code))
        }
    }
    FilledTonalButton(
        onClick = { onEvent(AdminPanelUiEvent.RegenerateInviteToken) },
        enabled = !state.isRegenerating
    ) {
        Text(stringResource(R.string.admin_new_code))
    }
    Text(
        stringResource(R.string.admin_new_code_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    state.error?.let { ErrorText(stringResource(R.string.auth_error_unknown)) }
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
            onBack = {},
            onCopyCode = {},
            onShareCode = {}
        )
    }
}
