package com.azersun.uitemplates.demos.fab2

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.azersun.uitemplates.demos.menus.MenuColors
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val RingGap = 7.dp
private val RingStroke = 3.dp

@Composable
fun ScrollToTopFabDemo() {
    val autoplay = rememberAutoplay()
    val scroll = rememberScrollState()
    val press = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    suspend fun backToTop() = coroutineScope {
        launch {
            press.animateTo(0.86f, tween(90))
            press.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 600f))
        }
        scroll.animateScrollTo(0, tween(700, easing = FastOutSlowInEasing))
    }

    AutoplayEffect(autoplay) {
        scroll.animateScrollTo((scroll.maxValue * 0.45f).roundToInt(), tween(900, easing = FastOutSlowInEasing))
        delay(250)
        scroll.animateScrollTo((scroll.maxValue * 0.9f).roundToInt(), tween(1000, easing = FastOutSlowInEasing))
        delay(700)
        backToTop()
    }

    NotesScene(
        1,
        "Scroll to Top",
        "A ring round the button fills as the list scrolls. Tap it and the list flies back to the top.",
        autoplay,
    ) {
        Column(Modifier.fillMaxSize()) {
            NotesHeader(
                count = Notes.size,
                modifier = Modifier.drawBehind {
                    val shade = (scroll.value / 24.dp.toPx()).coerceIn(0f, 1f)
                    drawLine(
                        MenuColors.CardBorder.copy(alpha = shade),
                        Offset(0f, size.height),
                        Offset(size.width, size.height),
                        1.dp.toPx(),
                    )
                },
            )
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scroll)
                    .padding(bottom = 96.dp),
            ) {
                Notes.forEach { NoteRow(it) }
            }
        }
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(FabInset - RingGap)
                .size(FabSize + RingGap * 2)
                .drawWithCache {
                    val stroke = RingStroke.toPx()
                    val radius = size.minDimension / 2 - stroke / 2
                    val center = Offset(size.width / 2, size.height / 2)
                    val topLeft = Offset(center.x - radius, center.y - radius)
                    val arcSize = Size(radius * 2, radius * 2)
                    val sweep = Brush.sweepGradient(listOf(MenuColors.Cyan, MenuColors.Blue, MenuColors.Violet, MenuColors.Pink), center)
                    val track = Stroke(stroke)
                    val arc = Stroke(stroke, cap = StrokeCap.Round)
                    onDrawBehind {
                        val max = scroll.maxValue
                        val progress = if (max in 1 until Int.MAX_VALUE) scroll.value.toFloat() / max else 0f
                        drawCircle(Color.White.copy(alpha = 0.08f), radius, style = track)
                        if (progress > 0.002f) {
                            // The sweep gradient starts at 3 o'clock; turn it so the ring fills from 12.
                            rotate(-90f) { drawArc(sweep, 0f, 360f * progress, false, topLeft, arcSize, style = arc) }
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Fab(
                Modifier.graphicsLayer {
                    scaleX = press.value
                    scaleY = press.value
                },
                onClick = { scope.launch { backToTop() } },
                onClickLabel = "Scroll to top",
            ) {
                Canvas(Modifier.size(22.dp)) { drawArrowUp(Color.White) }
            }
        }
    }
}

private fun DrawScope.drawArrowUp(color: Color) {
    val w = size.width
    val stroke = w * 0.12f
    drawLine(color, Offset(w * 0.5f, w * 0.86f), Offset(w * 0.5f, w * 0.16f), stroke, StrokeCap.Round)
    drawLine(color, Offset(w * 0.2f, w * 0.44f), Offset(w * 0.5f, w * 0.14f), stroke, StrokeCap.Round)
    drawLine(color, Offset(w * 0.8f, w * 0.44f), Offset(w * 0.5f, w * 0.14f), stroke, StrokeCap.Round)
}
