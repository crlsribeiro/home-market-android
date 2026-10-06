package app.carlosribeiro.homemarket.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The iOS design system colors (`DesignSystem.swift`) that Material 3 has no role for: the white card
 * with its thin border, the light green tint behind secondary actions and placeholders, the gray of a
 * read-only field, and the status colors of the badges (green purchased, orange pending and urgent, red
 * not found, blue shopping). The status colors are darker than the iOS system colors so the text keeps
 * a 4.5:1 contrast on its 15 % tinted badge.
 */
@Immutable
data class BrandColors(
    val card: Color,
    val cardBorder: Color,
    val tint: Color,
    val tintBorder: Color,
    val readOnlyField: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val info: Color
)

val LightBrandColors = BrandColors(
    card = Color(0xFFFFFFFF),
    cardBorder = Color(0xFFE5E5EA),
    tint = Color(0xFFF0FDF4),
    tintBorder = Color(0x332E7D52),
    readOnlyField = Color(0xFFF2F2F7),
    success = Color(0xFF1B7F3B),
    warning = Color(0xFFB35300),
    danger = Color(0xFFC62828),
    info = Color(0xFF1565C0)
)

val DarkBrandColors = BrandColors(
    card = Color(0xFF1A1F1C),
    cardBorder = Color(0xFF2E3430),
    tint = Color(0xFF17301F),
    tintBorder = Color(0x5593D5A9),
    readOnlyField = Color(0xFF222824),
    success = Color(0xFF7BD89A),
    warning = Color(0xFFFFB870),
    danger = Color(0xFFFF8A80),
    info = Color(0xFF90CAF9)
)

val LocalBrandColors = staticCompositionLocalOf { LightBrandColors }

/** The brand colors of the current theme. */
val brandColors: BrandColors
    @Composable
    @ReadOnlyComposable
    get() = LocalBrandColors.current
