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
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personal.appstore.R

/**
 * Оформление по присланному референсу: светлый пастельный фон, крупные мягкие
 * карточки, тёмная «пилюля» главного действия и очень крупные заголовки.
 *
 * Каждому приложению достаётся своя пастельная подложка — как плитки коллекции
 * в референсе. Цвет выводится из packageName, чтобы не зависеть от порядка в
 * витрине и не меняться между запусками.
 */

private val Ink = Color(0xFF1B1B1F)
private val InkSoft = Color(0xFF6B6B75)
private val Canvas = Color(0xFFFBF8F7)
private val Coral = Color(0xFFF2A9A0)
private val CoralDeep = Color(0xFFE0897E)

private val CanvasDark = Color(0xFF141316)
private val SurfaceDark = Color(0xFF1E1D21)
private val InkDark = Color(0xFFF6F4F3)
private val InkSoftDark = Color(0xFFA3A0A8)

/** Зелёный акцент онбординга — фирменный цвет дроида, один и тот же в обеих темах. */
val StoreGreen = Color(0xFF7CB342)

/** Пастельные подложки карточек: тон выбирается по packageName. */
val CardTints = listOf(
    Color(0xFFF7D9D4),
    Color(0xFFDDE9CF),
    Color(0xFFF6E3C5),
    Color(0xFFD6E4F0),
    Color(0xFFE8DAF2),
    Color(0xFFCFE9E4),
)

fun tintFor(id: String): Color = CardTints[(id.hashCode().let { if (it == Int.MIN_VALUE) 0 else kotlin.math.abs(it) }) % CardTints.size]

private val LightColors = lightColorScheme(
    primary = Ink,
    onPrimary = Color.White,
    primaryContainer = Coral,
    onPrimaryContainer = Ink,
    secondary = Coral,
    onSecondary = Ink,
    secondaryContainer = Color(0xFFF7D9D4),
    onSecondaryContainer = Ink,
    background = Canvas,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFF1ECEA),
    onSurfaceVariant = InkSoft,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFF4EFED),
    outline = Color(0xFFE6DFDC),
    outlineVariant = Color(0xFFEFE9E6),
    error = Color(0xFFB3352F),
    onError = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = InkDark,
    onPrimary = Ink,
    primaryContainer = CoralDeep,
    onPrimaryContainer = Ink,
    secondary = Coral,
    onSecondary = Ink,
    secondaryContainer = Color(0xFF3A2F2E),
    onSecondaryContainer = InkDark,
    background = CanvasDark,
    onBackground = InkDark,
    surface = SurfaceDark,
    onSurface = InkDark,
    surfaceVariant = Color(0xFF272529),
    onSurfaceVariant = InkSoftDark,
    surfaceContainer = SurfaceDark,
    surfaceContainerHigh = Color(0xFF272529),
    outline = Color(0xFF333136),
    outlineVariant = Color(0xFF2A282D),
    error = Color(0xFFE98B85),
    onError = Ink,
)

/**
 * Golos Text — вариативный шрифт (ось wght, Regular…Black), лежит одним файлом
 * в `res/font`. Compose сам инстанцирует нужное начертание: у `Font` по
 * умолчанию `variationSettings = Settings(weight, style)`, а API 26 (наш minSdk)
 * вариативные шрифты уже понимает.
 */
val GolosText = FontFamily(
    Font(R.font.golos_text, FontWeight.Normal),
    Font(R.font.golos_text, FontWeight.Medium),
    Font(R.font.golos_text, FontWeight.SemiBold),
    Font(R.font.golos_text, FontWeight.Bold),
)

/** Проставляет семейство всем стилям, включая те, что мы не переопределяли. */
private fun Typography.withFontFamily(family: FontFamily): Typography = copy(
    displayLarge = displayLarge.copy(fontFamily = family),
    displayMedium = displayMedium.copy(fontFamily = family),
    displaySmall = displaySmall.copy(fontFamily = family),
    headlineLarge = headlineLarge.copy(fontFamily = family),
    headlineMedium = headlineMedium.copy(fontFamily = family),
    headlineSmall = headlineSmall.copy(fontFamily = family),
    titleLarge = titleLarge.copy(fontFamily = family),
    titleMedium = titleMedium.copy(fontFamily = family),
    titleSmall = titleSmall.copy(fontFamily = family),
    bodyLarge = bodyLarge.copy(fontFamily = family),
    bodyMedium = bodyMedium.copy(fontFamily = family),
    bodySmall = bodySmall.copy(fontFamily = family),
    labelLarge = labelLarge.copy(fontFamily = family),
    labelMedium = labelMedium.copy(fontFamily = family),
    labelSmall = labelSmall.copy(fontFamily = family),
)

/** Крупные заголовки с плотным трекингом — как в референсе. */
private val StoreTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = GolosText,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 38.sp,
        letterSpacing = (-1.0).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = GolosText,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.8).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = GolosText,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 27.sp,
        letterSpacing = (-0.5).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = GolosText,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 23.sp,
        letterSpacing = (-0.3).sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = GolosText,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = GolosText,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = GolosText,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = GolosText,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 19.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = GolosText,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 17.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = GolosText,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
).withFontFamily(GolosText)

/** Мягкая геометрия: карточки 24, кнопки-пилюли отдельно в компонентах. */
private val StoreShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
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
