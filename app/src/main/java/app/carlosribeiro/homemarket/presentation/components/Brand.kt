@file:OptIn(ExperimentalMaterial3Api::class)

package app.carlosribeiro.homemarket.presentation.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.carlosribeiro.homemarket.presentation.theme.ControlHeight
import app.carlosribeiro.homemarket.presentation.theme.brandColors

/**
 * iOS `brandCard`: a white card with a 16 dp radius and a thin gray border, the container of every list
 * row and section. With [onClick] the whole card is one touch target; [color] tints it.
 */
@Composable
fun BrandCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    color: Color = brandColors.card,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(12.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = MaterialTheme.shapes.large
    val border = BorderStroke(1.dp, brandColors.cardBorder)
    val body: @Composable () -> Unit = {
        Column(Modifier.padding(contentPadding), verticalArrangement = verticalArrangement, content = content)
    }
    if (onClick == null) {
        Surface(modifier = modifier.fillMaxWidth(), shape = shape, color = color, border = border, content = body)
    } else {
        Surface(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            color = color,
            border = border,
            content = body
        )
    }
}

/**
 * iOS `BrandFieldLabel`: the uppercase, letter-spaced caption above a field or a section. Accessibility
 * services get the text as written, so it isn't read letter by letter.
 */
@Composable
fun BrandFieldLabel(text: String, modifier: Modifier = Modifier, required: Boolean = false) {
    Row(
        modifier = modifier
            .padding(start = 4.dp)
            .clearAndSetSemantics { this.text = AnnotatedString(text) }
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (required) {
            Text(text = " *", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
        }
    }
}

/** A section title in the iOS caption style, announced as a heading. */
@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    BrandFieldLabel(text = text, modifier = modifier.semantics { heading() })
}

/** The iOS button styles of `DesignSystem.swift`. */
enum class BrandButtonStyle { PRIMARY, TINT, OUTLINE, DESTRUCTIVE, DESTRUCTIVE_OUTLINE }

/**
 * Full-width button at the shared 54 dp control height with a 12 dp radius, in one of the iOS styles.
 * Shows a spinner instead of the label while [isLoading].
 */
@Composable
fun BrandButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: BrandButtonStyle = BrandButtonStyle.PRIMARY,
    @DrawableRes icon: Int? = null,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        shape = MaterialTheme.shapes.medium,
        colors = style.colors(),
        border = style.border(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ControlHeight)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = LocalContentColor.current
            )
        } else {
            icon?.let {
                Icon(painterResource(it), contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text = text, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun BrandButtonStyle.colors(): ButtonColors {
    val scheme = MaterialTheme.colorScheme
    val brand = brandColors
    return when (this) {
        BrandButtonStyle.PRIMARY -> ButtonDefaults.buttonColors()

        BrandButtonStyle.TINT -> ButtonDefaults.buttonColors(containerColor = brand.tint, contentColor = scheme.primary)

        BrandButtonStyle.OUTLINE -> ButtonDefaults.buttonColors(
            containerColor = brand.card,
            contentColor = scheme.onSurface
        )

        BrandButtonStyle.DESTRUCTIVE -> ButtonDefaults.buttonColors(
            containerColor = scheme.error,
            contentColor = scheme.onError
        )

        BrandButtonStyle.DESTRUCTIVE_OUTLINE -> ButtonDefaults.buttonColors(
            containerColor = brand.card,
            contentColor = scheme.error
        )
    }
}

@Composable
private fun BrandButtonStyle.border(): BorderStroke? = when (this) {
    BrandButtonStyle.TINT -> BorderStroke(1.dp, brandColors.tintBorder)
    BrandButtonStyle.OUTLINE -> BorderStroke(1.dp, brandColors.cardBorder)
    BrandButtonStyle.DESTRUCTIVE_OUTLINE -> BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
    BrandButtonStyle.PRIMARY, BrandButtonStyle.DESTRUCTIVE -> null
}

/** A read-only value in the field shape, on the iOS gray (first and last name on the account screen). */
@Composable
fun ReadOnlyField(value: String, label: String, @DrawableRes icon: Int, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        BrandFieldLabel(label)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = ControlHeight)
                .background(brandColors.readOnlyField, MaterialTheme.shapes.medium)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(painterResource(icon), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** The square brand-green badge with the white cart (iOS login and onboarding header). */
@Composable
fun AppMark(@DrawableRes icon: Int, size: Dp, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(size),
        shape = if (size > SmallMarkSize) MaterialTheme.shapes.large else MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shadowElevation = if (size > SmallMarkSize) 4.dp else 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(size * Mark.ICON_FRACTION))
        }
    }
}

/** Up to this size the app mark is the small inline badge (onboarding), above it the login hero. */
private val SmallMarkSize = 48.dp

/** The cart takes half of the app mark, as on iOS. */
private object Mark {
    const val ICON_FRACTION = 0.5f
}
