package com.personal.appstore.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.personal.appstore.R
import com.personal.appstore.ui.theme.StoreGreen
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Приветственный экран: коллаж дроидов, крупный заголовок и слайдер
 * «К приложениям!». Показывается при первом запуске, а в режиме разработчика —
 * при каждом.
 */
@Composable
fun OnboardingScreen(onStart: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            DroidCollage(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(CollageHeightFraction),
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
                    fontWeight = FontWeight.Bold,
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
                SlideToStart(onStart = onStart, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

/**
 * Какую долю экрана занимает коллаж. Картинка горизонтальная: чем выше полоса,
 * тем сильнее Crop увеличивает фигурки и тем меньше их влезает. 0.45 — та
 * плотность, что на референсе.
 */
private const val CollageHeightFraction = 0.45f

/** Наклон коллажа — как в референсе. */
private const val CollageRotation = 15f

/**
 * Во сколько раз увеличить повёрнутую картинку, чтобы её углы не заехали в
 * кадр: прямоугольник W × H, повёрнутый на θ, накрывает исходный, если его
 * масштабировать в `max((W·cosθ + H·sinθ)/W, (W·sinθ + H·cosθ)/H)` раз.
 */
private fun coverScale(width: Float, height: Float, degrees: Float): Float {
    val rad = degrees * PI.toFloat() / 180f
    val c = abs(cos(rad))
    val s = abs(sin(rad))
    return max((width * c + height * s) / width, (width * s + height * c) / height)
}

/**
 * Коллаж — готовая картинка `res/drawable-nodpi/onboarding_collage.webp`,
 * наклонённая на [CollageRotation]. Занимает всё место над заголовком:
 * картинка горизонтальная, поэтому по бокам её подрезает край экрана — как в
 * референсе. Низ гасим градиентом в фон, чтобы заголовок не спорил с картинкой.
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
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val cover = coverScale(size.width, size.height, CollageRotation)
                    rotationZ = CollageRotation
                    scaleX = cover
                    scaleY = cover
                },
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.78f to Color.Transparent,
                        0.98f to background,
                    ),
                ),
        )
    }
}

private val PillHeight: Dp = 72.dp
private val KnobInset: Dp = 8.dp

/** Доля пути, после которой отпускание засчитывается как «дотянул». */
private const val SlideThreshold = 0.85f

/**
 * Зелёная пилюля со свайпом до конца — как приём вызова на старых iPhone:
 * кружок с шевроном тянется вправо, надпись по пути гаснет, у края
 * срабатывает переход. Отпустили раньше — кружок уезжает обратно.
 */
@Composable
private fun SlideToStart(onStart: () -> Unit, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val knobSize = PillHeight - KnobInset * 2
    val offset = remember { Animatable(0f) }
    var travel by remember { mutableFloatStateOf(0f) }
    // Второй раз не срабатываем: докатывание анимируется уже после onStart.
    var finished by remember { mutableStateOf(false) }
    val progress = if (travel > 0f) (offset.value / travel).coerceIn(0f, 1f) else 0f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(PillHeight)
            .clip(RoundedCornerShape(percent = 50))
            .background(StoreGreen)
            .semantics {
                contentDescription = "К приложениям: проведите кружок вправо"
                onClick(label = "Перейти к приложениям") {
                    onStart()
                    true
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = "К приложениям!",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = PillHeight, end = 24.dp)
                .alpha(1f - progress),
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(KnobInset)
                .onSizeChanged { size ->
                    travel = (size.width - with(density) { knobSize.toPx() }).coerceAtLeast(0f)
                },
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset { IntOffset(offset.value.roundToInt(), 0) }
                    .size(knobSize)
                    .clip(CircleShape)
                    .background(Color.White)
                    .draggable(
                        orientation = Orientation.Horizontal,
                        state = rememberDraggableState { delta ->
                            scope.launch {
                                offset.snapTo((offset.value + delta).coerceIn(0f, travel))
                            }
                        },
                        onDragStopped = {
                            if (finished) return@draggable
                            if (travel > 0f && offset.value >= travel * SlideThreshold) {
                                finished = true
                                // Докатывание и переход — в scope экрана, а не в корутине
                                // жеста: `enabled = !finished` тут же снимает draggable,
                                // его корутина отменяется, и onStart() не доходит —
                                // кружок замирает у края, а экран не меняется.
                                scope.launch {
                                    offset.animateTo(travel, tween(durationMillis = 120))
                                    onStart()
                                }
                            } else {
                                offset.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                )
                            }
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Chevrons()
            }
        }
    }
}

@Composable
private fun Chevrons() {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier = Modifier.size(26.dp, 14.dp)) {
        val step = size.width / 3f
        val arm = size.height / 2f
        repeat(3) { i ->
            val x = step * i + step * 0.15f
            drawLine(
                color = color,
                start = Offset(x, 0f),
                end = Offset(x + arm, size.height / 2f),
                strokeWidth = size.height * 0.13f,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = color,
                start = Offset(x + arm, size.height / 2f),
                end = Offset(x, size.height),
                strokeWidth = size.height * 0.13f,
                cap = StrokeCap.Round,
            )
        }
    }
}
