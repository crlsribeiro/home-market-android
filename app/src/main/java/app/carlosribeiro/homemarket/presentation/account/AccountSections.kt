@file:OptIn(ExperimentalMaterial3Api::class)

package app.carlosribeiro.homemarket.presentation.account

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.carlosribeiro.homemarket.R
import app.carlosribeiro.homemarket.domain.model.ProfileError
import app.carlosribeiro.homemarket.presentation.components.ErrorText
import app.carlosribeiro.homemarket.presentation.components.FormTextField
import app.carlosribeiro.homemarket.presentation.components.PhoneField
import app.carlosribeiro.homemarket.presentation.components.PhotoSourceMenu
import app.carlosribeiro.homemarket.presentation.components.ReadOnlyField
import app.carlosribeiro.homemarket.presentation.components.SubmitButton
import app.carlosribeiro.homemarket.presentation.components.formKeyboard
import app.carlosribeiro.homemarket.presentation.theme.brandColors
import coil3.compose.AsyncImage

/** The profile photo; tapping it offers the camera or the gallery, as on iOS. A camera badge shows it is tappable. */
@Composable
fun AvatarPicker(state: AccountUiState, onPhotoPicked: (String) -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    val name = state.user?.displayName.orEmpty()
    val description = stringResource(R.string.account_change_photo)
    Box {
        Box(
            modifier = Modifier
                .size(AvatarSize)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.85f))
                .clickable(onClickLabel = description, role = Role.Button) { showMenu = true },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.split(' ').filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1) }.uppercase()
                    .ifEmpty { "?" },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
            (state.pendingPhotoUri ?: state.user?.photoUrl)?.let { model ->
                AsyncImage(
                    model = model,
                    contentDescription = description,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(AvatarSize)
                )
            }
            if (state.isUploadingPhoto) CircularProgressIndicator()
        }
        CameraBadge(modifier = Modifier.align(Alignment.BottomEnd))
        PhotoSourceMenu(expanded = showMenu, onDismiss = { showMenu = false }, onPhoto = onPhotoPicked)
    }
}

@Composable
private fun CameraBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(BadgeSize)
            .border(2.dp, Color.White, CircleShape)
            .padding(2.dp)
            .background(MaterialTheme.colorScheme.primary, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painterResource(R.drawable.ic_photo_camera),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(BadgeIconSize)
        )
    }
}

/** Read-only names, email and phone (with the iOS country picker and mask), then "Save changes". */
@Composable
fun ProfileForm(state: AccountUiState, onEvent: (AccountUiEvent) -> Unit) {
    val (firstName, lastName) = state.user?.resolvedName ?: ("" to "")
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ReadOnlyField(value = firstName, label = stringResource(R.string.auth_first_name), icon = R.drawable.ic_person)
        ReadOnlyField(value = lastName, label = stringResource(R.string.auth_last_name), icon = R.drawable.ic_person)
        FormTextField(
            value = state.email,
            onValueChange = { onEvent(AccountUiEvent.EmailChanged(it)) },
            label = stringResource(R.string.auth_email),
            leadingIcon = R.drawable.ic_mail,
            placeholder = stringResource(R.string.auth_email_placeholder),
            keyboardOptions = formKeyboard(type = KeyboardType.Email)
        )
        PhoneField(
            phone = state.phone,
            country = state.country,
            onPhoneChange = { onEvent(AccountUiEvent.PhoneChanged(it)) },
            onCountryChange = { onEvent(AccountUiEvent.CountryChanged(it)) }
        )
        state.error?.let { ErrorText(stringResource(it.messageRes())) }
        state.message?.let { message ->
            Text(
                text = message.text(),
                style = MaterialTheme.typography.bodyMedium,
                color = brandColors.success
            )
        }
        SubmitButton(
            text = stringResource(R.string.account_save),
            isLoading = state.isSaving,
            onClick = { onEvent(AccountUiEvent.Save) }
        )
    }
}

@Composable
private fun AccountMessage.text(): String = when (this) {
    AccountMessage.Saved -> stringResource(R.string.account_saved)
    is AccountMessage.CheckNewEmail -> stringResource(R.string.account_check_new_email, email)
    AccountMessage.PhotoUpdated -> stringResource(R.string.account_photo_updated)
}

@StringRes
fun ProfileError.messageRes(): Int = when (this) {
    ProfileError.PHONE_INCOMPLETE -> R.string.account_error_phone_incomplete
    ProfileError.EMAIL_INVALID -> R.string.auth_error_email_invalid
    ProfileError.EMAIL_IN_USE -> R.string.auth_error_email_in_use
    ProfileError.REQUIRES_RECENT_LOGIN -> R.string.account_error_recent_login
    ProfileError.NETWORK -> R.string.auth_error_network
    ProfileError.NOT_SIGNED_IN, ProfileError.UNKNOWN -> R.string.auth_error_unknown
}

private val AvatarSize = 72.dp
private val BadgeSize = 24.dp
private val BadgeIconSize = 12.dp
