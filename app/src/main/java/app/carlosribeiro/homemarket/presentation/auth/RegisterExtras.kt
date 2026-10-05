package app.carlosribeiro.homemarket.presentation.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import app.carlosribeiro.homemarket.R
import app.carlosribeiro.homemarket.presentation.components.rememberPhotoPicker
import coil3.compose.AsyncImage

/** Pages published for the iOS app, linked from the same registration screen. */
private object LegalUrls {
    const val TERMS = "https://crlsribeiro.github.io/home-market-privacy/terms.html"
    const val PRIVACY = "https://crlsribeiro.github.io/home-market-privacy/"
}

/** iOS registration avatar: optional photo from the camera or the gallery. */
@Composable
fun RegisterPhoto(photoUri: String?, enabled: Boolean, onPhotoPicked: (String) -> Unit, onRemove: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    val photoPicker = rememberPhotoPicker(onPhotoPicked)
    val description = stringResource(R.string.register_add_photo)
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable(enabled = enabled, onClickLabel = description, role = Role.Button) { showMenu = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_photo_camera),
                    contentDescription = description,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                photoUri?.let {
                    AsyncImage(
                        model = it,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(80.dp)
                    )
                }
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.add_item_camera)) },
                    onClick = {
                        showMenu = false
                        photoPicker.takePhoto()
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.add_item_gallery)) },
                    onClick = {
                        showMenu = false
                        photoPicker.pickFromGallery()
                    }
                )
            }
        }
        if (photoUri != null) {
            TextButton(onClick = onRemove, enabled = enabled) { Text(stringResource(R.string.add_item_remove_photo)) }
        } else {
            Text(
                text = stringResource(R.string.register_photo_optional),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** iOS: "By creating an account, you agree to our Terms of Use and Privacy Policy", with links. */
@Composable
fun LegalLinks() {
    val linkStyles = TextLinkStyles(
        SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)
    )
    val prefix = stringResource(R.string.register_legal_prefix)
    val terms = stringResource(R.string.register_terms)
    val and = stringResource(R.string.register_legal_and)
    val privacy = stringResource(R.string.account_privacy_policy)
    val text = buildAnnotatedString {
        append("$prefix ")
        withLink(LinkAnnotation.Url(LegalUrls.TERMS, linkStyles)) { append(terms) }
        append(" $and ")
        withLink(LinkAnnotation.Url(LegalUrls.PRIVACY, linkStyles)) { append(privacy) }
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
