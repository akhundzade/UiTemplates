package com.azersun.uitemplates.demos.error2

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sqrt

private const val DrawMillis = 900
private const val RingMillis = 1700

/** A new ring starts every half ring lifetime, so two are on screen at once. */
private const val RingSpacing = 0.5f

private val FillBrushColors = listOf(Color(0xFFFF7A88), ErrorColors.Red, Color(0xFFE5354B))

@Composable
fun WarningPulseDemo() {
    val draw = remember { Animatable(0f) }
    val fill = remember { Animatable(0f) }
    val bang = remember { Animatable(0f) }
    // Counts ring lifetimes since the triangle filled; 0 means no rings yet.
    val rings = remember { Animatable(0f) }
    var job by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()

    fun restart(play: Boolean) {
        job?.cancel()
        job = scope.launch {
            draw.snapTo(0f)
            fill.snapTo(0f)
            bang.snapTo(0f)
            rings.snapTo(0f)
            if (!play) return@launch
            draw.animateTo(1f, tween(DrawMillis, easing = FastOutSlowInEasing))
            launch {
                delay(120)
                bang.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = 480f))
            }
            fill.animateTo(1f, tween(300))
            while (true) {
                rings.animateTo(rings.value + 1f, tween(RingMillis, easing = LinearEasing))
            }
        }
    }

    ErrorStage(
        3,
        "Warning Pulse",
        "Triangle outline draws in, fills red with \"!\", and sends out pulse rings.",
        onReplay = { restart(play = false) },
        autoplay = { restart(play = true) },
    ) {
        Spacer(
            Modifier
                .size(260.dp)
                .clickable(
                    interactionSource = null,
                    indication = null,
                    onClickLabel = "Replay warning",
                    role = Role.Button,
                    onClick = { restart(play = true) },
                )
                .drawWithCache {
                    val side = size.width * 0.66f
                    val height = side * sqrt(3f) / 2f
                    val top = (size.height - height) / 2f
                    val cx = size.width / 2f
                    val triangle = Path().apply {
                        roundedPolygon(
                            listOf(Offset(cx, top), Offset(cx + side / 2, top + height), Offset(cx - side / 2, top + height)),
                            side * 0.09f,
                        )
                    }
                    val measure = PathMeasure().apply { setPath(triangle, false) }
                    val length = measure.length
                    val trimmed = Path()
                    val centroid = Offset(cx, top + height * 2f / 3f)
                    val bangCenter = Offset(cx, top + height * 0.62f)
                    val strokeWidth = 5.dp.toPx()
                    val outline = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    val fillBrush = Brush.verticalGradient(FillBrushColors, top, top + height)
                    onDrawBehind {
                        val d = draw.value
                        val f = fill.value
                        val b = bang.value
                        val clock = rings.value
                        if (clock > 0f) {
                            repeat(2) { k ->
                                val phase = clock % RingSpacing + k * RingSpacing
                                if (phase <= clock) {
                                    val s = 1f + 0.8f * phase
                                    scale(s, centroid) {
                                        drawPath(triangle, ErrorColors.Red, alpha = (1f - phase) * 0.6f, style = Stroke(strokeWidth * 0.8f / s))
                                    }
                                }
                            }
                        }
                        if (d < 1f) drawPath(triangle, Color.White, alpha = 0.06f, style = outline)
                        if (f > 0f) {
                            drawPath(triangle, ErrorColors.Red, alpha = 0.22f * f, style = Stroke(strokeWidth * 5f, join = StrokeJoin.Round))
                            drawPath(triangle, fillBrush, alpha = f)
                        }
                        if (d > 0f) {
                            trimmed.reset()
                            measure.getSegment(0f, length * d, trimmed, true)
                            drawPath(trimmed, lerp(ErrorColors.Blue, ErrorColors.Red, f), style = outline)
                        }
                        if (b > 0f) {
                            scale(b, bangCenter) {
                                val w = side * 0.075f
                                drawLine(
                                    Color.White,
                                    Offset(cx, top + height * 0.36f),
                                    Offset(cx, top + height * 0.66f),
                                    w,
                                    StrokeCap.Round,
                                )
                                drawCircle(Color.White, w * 0.62f, Offset(cx, top + height * 0.8f))
                            }
                        }
                    }
                },
        )
    }
}

/** Replaces this path with a polygon through [points] whose corners are rounded by [radius]. */
private fun Path.roundedPolygon(points: List<Offset>, radius: Float) {
    reset()
    val n = points.size
    for (step in 0..n) {
        val i = step % n
        val v = points[i]
        val toPrev = points[(i + n - 1) % n] - v
        val toNext = points[(i + 1) % n] - v
        val entry = v + toPrev / toPrev.getDistance() * radius
        val exit = v + toNext / toNext.getDistance() * radius
        if (step == 0) {
            moveTo(exit.x, exit.y)
        } else {
            lineTo(entry.x, entry.y)
            quadraticTo(v.x, v.y, exit.x, exit.y)
        }
    }
    close()
}
