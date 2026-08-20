package com.personal.appstore.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Та же система оформления, что в веб-панели (см. linear.app-DESIGN.md):
 * канвас #010102, лестница поверхностей, лавандовый акцент и волосяные рамки.
 *
 * Динамические цвета Android намеренно выключены: в системе один акцент, и
 * подмена его обоями сломала бы единство с панелью.
 */

private val Lavender = Color(0xFF5E6AD2)
private val LavenderHover = Color(0xFF828FFF)

// Тёмная — основная.
private val Canvas = Color(0xFF010102)
private val Surface1 = Color(0xFF0F1011)
private val Surface2 = Color(0xFF141516)
private val Hairline = Color(0xFF23252A)
private val Ink = Color(0xFFF7F8F8)
private val InkMuted = Color(0xFFD0D6E0)
private val InkSubtle = Color(0xFF8A8F98)
private val Danger = Color(0xFFE5484D)

// Светлая — та же система, вывернутая.
private val CanvasLight = Color(0xFFFBFBFC)
private val Surface1Light = Color(0xFFFFFFFF)
private val Surface2Light = Color(0xFFF4F5F7)
private val HairlineLight = Color(0xFFE4E6EA)
private val InkLight = Color(0xFF0D0E10)
private val InkMutedLight = Color(0xFF3D424B)
private val InkSubtleLight = Color(0xFF6B7280)
private val DangerLight = Color(0xFFC62A2F)

private val DarkColors = darkColorScheme(
    primary = Lavender,
    onPrimary = Color.White,
    primaryContainer = Surface2,
    onPrimaryContainer = Ink,
    secondary = LavenderHover,
    onSecondary = Color.White,
    secondaryContainer = Surface2,
    onSecondaryContainer = InkMuted,
    background = Canvas,
    onBackground = Ink,
    surface = Canvas,
    onSurface = Ink,
    surfaceVariant = Surface1,
    onSurfaceVariant = InkSubtle,
    surfaceContainer = Surface1,
    surfaceContainerHigh = Surface2,
    outline = Hairline,
    outlineVariant = Hairline,
    error = Danger,
    onError = Color.White,
)

private val LightColors = lightColorScheme(
    primary = Lavender,
    onPrimary = Color.White,
    primaryContainer = Surface2Light,
    onPrimaryContainer = InkLight,
    secondary = Lavender,
    onSecondary = Color.White,
    secondaryContainer = Surface2Light,
    onSecondaryContainer = InkMutedLight,
    background = CanvasLight,
    onBackground = InkLight,
    surface = CanvasLight,
    onSurface = InkLight,
    surfaceVariant = Surface1Light,
    onSurfaceVariant = InkSubtleLight,
    surfaceContainer = Surface1Light,
    surfaceContainerHigh = Surface2Light,
    outline = HairlineLight,
    outlineVariant = HairlineLight,
    error = DangerLight,
    onError = Color.White,
)

/**
 * Шкала из спецификации: отрицательный трекинг на крупном, ноль на мелком.
 * Собственный шрифт Linear не распространяется, поэтому системный гротеск —
 * ровно та замена, которую спецификация и предлагает на устройствах Apple/Android.
 */
private val StoreTypography = Typography(
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 29.sp,
        letterSpacing = (-0.6).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 25.sp,
        letterSpacing = (-0.4).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.2).sp,
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.05).sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp,
        letterSpacing = 0.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 17.sp,
        letterSpacing = 0.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 17.sp,
        letterSpacing = 0.4.sp, // eyebrow: единственная положительная разрядка
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        letterSpacing = 0.sp,
    ),
)

/** Скругления: 8 у кнопок, 12 у карточек, 16 у крупных панелей. Никаких «таблеток». */
private val StoreShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(16.dp),
)

@Composable
fun PersonalAppStoreTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = StoreTypography,
        shapes = StoreShapes,
        content = content,
    )
}
