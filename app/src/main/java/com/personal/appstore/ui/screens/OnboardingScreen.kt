package com.personal.appstore.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.personal.appstore.R
import com.personal.appstore.ui.theme.StoreGreen

/**
 * Приветственный экран: коллаж дроидов-персонажей, крупный заголовок и одна
 * зелёная кнопка-пилюля. Показывается при первом запуске, а в режиме
 * разработчика — при каждом.
 */
@Composable
fun OnboardingScreen(onStart: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            DroidCollage(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(CollageAspect / CollageZoom),
            )
            Spacer(modifier = Modifier.weight(1f))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Устанавливайте свои приложения",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "Все разработанные вами приложения в одном месте",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                StartButton(onClick = onStart, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

/** Пропорции присланного коллажа (5608 × 4488). */
private const val CollageAspect = 5608f / 4488f

/**
 * Насколько картинка шире экрана. Коллаж горизонтальный, экран вертикальный:
 * без запаса фигурки выходят мелкими, с запасом крайние уходят за край —
 * как в референсе. 1.45 — компромисс: видны все 20, обрезаны только края.
 */
private const val CollageZoom = 1.45f

/**
 * Коллаж — готовая картинка `res/drawable-nodpi/onboarding_collage.webp`.
 * Область берём по пропорциям картинки с запасом [CollageZoom]: фигурки
 * получаются крупными, а крайние подрезаются краем экрана. Низ гасим
 * градиентом в фон, чтобы заголовок не спорил с картинкой.
 */
@Composable
private fun DroidCollage(modifier: Modifier = Modifier) {
    val background = MaterialTheme.colorScheme.background
    Box(modifier = modifier.clipToBounds()) {
        Image(
            painter = painterResource(R.drawable.onboarding_collage),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.62f to Color.Transparent,
                        0.98f to background,
                    ),
                ),
        )
    }
}

/** Зелёная пилюля с белым кружком-шевроном справа. */
@Composable
private fun StartButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val pillHeight: Dp = 72.dp
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(pillHeight)
            .clip(RoundedCornerShape(percent = 50))
            .background(StoreGreen)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "К приложениям" },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "К приложениям!",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            modifier = Modifier.padding(end = 56.dp),
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(8.dp)
                .size(pillHeight - 16.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            Chevrons()
        }
    }
}

@Composable
private fun Chevrons() {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    androidx.compose.foundation.Canvas(modifier = Modifier.size(26.dp, 14.dp)) {
        val w = size.width
        val h = size.height
        val step = w / 3f
        val arm = h / 2f
        repeat(3) { i ->
            val x = step * i + step * 0.15f
            drawLine(
                color = color,
                start = Offset(x, 0f),
                end = Offset(x + arm, h / 2f),
                strokeWidth = h * 0.13f,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = color,
                start = Offset(x + arm, h / 2f),
                end = Offset(x, h),
                strokeWidth = h * 0.13f,
                cap = StrokeCap.Round,
            )
        }
    }
}
