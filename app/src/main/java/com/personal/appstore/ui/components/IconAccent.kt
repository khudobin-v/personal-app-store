package com.personal.appstore.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil3.BitmapImage
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Основной цвет иконки приложения.
 *
 * Иконку и так грузит Coil, поэтому берём её из того же кэша и отдаём в Palette.
 * `allowHardware(false)` обязателен: у hardware-битмапа нельзя прочитать
 * пиксели, и Palette на нём падает.
 */
@Composable
fun rememberIconAccent(iconUrl: String?, fallback: Color): Color {
    val context = LocalContext.current
    var accent by remember(iconUrl) { mutableStateOf(fallback) }

    LaunchedEffect(iconUrl) {
        if (iconUrl.isNullOrBlank()) return@LaunchedEffect
        val request = ImageRequest.Builder(context)
            .data(iconUrl)
            .allowHardware(false)
            .build()
        val image = (SingletonImageLoader.get(context).execute(request) as? SuccessResult)?.image
        val bitmap = (image as? BitmapImage)?.bitmap ?: return@LaunchedEffect

        val palette = withContext(Dispatchers.Default) {
            Palette.from(bitmap).maximumColorCount(16).generate()
        }
        val rgb = palette.vibrantSwatch?.rgb
            ?: palette.dominantSwatch?.rgb
            ?: palette.mutedSwatch?.rgb
        if (rgb != null) accent = Color(rgb)
    }

    return accent
}

/** Подложка: половина цвета иконки поверх фона экрана. */
fun Color.asBannerTint(background: Color): Color = copy(alpha = 0.5f).compositeOver(background)

/**
 * Читаемый цвет текста для подложки. Порогом по яркости не обойтись: у средних
 * по светлоте подложек белый и чёрный дают разный контраст, поэтому считаем его
 * по формуле WCAG и берём тот, что выигрывает.
 */
fun Color.readableInk(): Color {
    val dark = Color(0xFF14140F)
    val light = Color(0xFFFBFBF8)
    return if (contrastWith(dark) >= contrastWith(light)) dark else light
}

private fun Color.contrastWith(other: Color): Float {
    val a = luminance() + 0.05f
    val b = other.luminance() + 0.05f
    return if (a > b) a / b else b / a
}
