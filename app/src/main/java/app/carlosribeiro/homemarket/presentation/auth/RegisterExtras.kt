@file:OptIn(ExperimentalMaterial3Api::class)

package app.carlosribeiro.homemarket.presentation.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import app.carlosribeiro.homemarket.R
import app.carlosribeiro.homemarket.presentation.components.PhotoSourceMenu
import coil3.compose.AsyncImage

/** Pages published for the iOS app, linked from the same registration screen. */
private object LegalUrls {
    const val TERMS = "https://crlsribeiro.github.io/home-market-privacy/terms.html"
    const val PRIVACY = "https://crlsribeiro.github.io/home-market-privacy/"
}

/** iOS registration header: brand green behind the status bar, the back button, the title and the photo. */
@Composable
fun RegisterHeader(
    photoUri: String?,
    enabled: Boolean,
    onBack: () -> Unit,
    onPhotoPicked: (String) -> Unit,
    onRemovePhoto: () -> Unit
) {
    val onHeader = MaterialTheme.colorScheme.onPrimary
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(Modifier.fillMaxWidth()) {
            IconButton(
                onClick = onBack,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = onHeader.copy(alpha = 0.2f),
                    contentColor = onHeader
                )
            ) {
                Icon(
                    painterResource(R.drawable.ic_arrow_back),
                    contentDescription = stringResource(R.string.action_back)
                )
            }
        }
        Text(
            text = stringResource(R.string.auth_create_account),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = onHeader,
            modifier = Modifier.semantics { heading() }
        )
        RegisterPhoto(photoUri = photoUri, enabled = enabled, onPhotoPicked = onPhotoPicked)
        if (photoUri != null) {
            TextButton(
                onClick = onRemovePhoto,
                enabled = enabled,
                colors = ButtonDefaults.textButtonColors(contentColor = onHeader)
            ) {
                Text(stringResource(R.string.add_item_remove_photo))
            }
        } else {
            Text(
                text = stringResource(R.string.register_photo_optional),
                style = MaterialTheme.typography.bodySmall,
                color = onHeader.copy(alpha = 0.85f)
            )
        }
    }
}

/** The dashed photo circle with the camera badge; tapping it offers the camera or the gallery. */
@Composable
private fun RegisterPhoto(photoUri: String?, enabled: Boolean, onPhotoPicked: (String) -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    val description = stringResource(R.string.register_add_photo)
    val onHeader = MaterialTheme.colorScheme.onPrimary
    Box {
        Box(
            modifier = Modifier
                .size(PhotoSize)
                .clip(CircleShape)
                .background(onHeader.copy(alpha = 0.1f))
                .drawBehind {
                    drawCircle(
                        color = onHeader.copy(alpha = 0.7f),
                        radius = size.minDimension / 2 - 1.dp.toPx(),
                        style = Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))
                        )
                    )
                }
                .clickable(enabled = enabled, onClickLabel = description, role = Role.Button) { showMenu = true },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    painter = painterResource(R.drawable.ic_photo_camera),
                    contentDescription = description,
                    tint = onHeader.copy(alpha = 0.85f)
                )
                Text(
                    text = stringResource(R.string.register_photo),
                    style = MaterialTheme.typography.labelSmall,
                    color = onHeader.copy(alpha = 0.85f)
                )
            }
            photoUri?.let {
                AsyncImage(
                    model = it,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(PhotoSize)
                )
            }
        }
        CameraBadge(Modifier.align(Alignment.BottomEnd))
        PhotoSourceMenu(expanded = showMenu, onDismiss = { showMenu = false }, onPhoto = onPhotoPicked)
    }
}

@Composable
private fun CameraBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(28.dp)
            .border(2.dp, Color.White, CircleShape)
            .padding(2.dp)
            .background(MaterialTheme.colorScheme.primary, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painterResource(R.drawable.ic_photo_camera),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(14.dp)
        )
    }
}

private val PhotoSize = 96.dp

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
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}
