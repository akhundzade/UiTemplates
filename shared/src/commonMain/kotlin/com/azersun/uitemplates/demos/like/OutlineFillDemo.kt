package com.azersun.uitemplates.demos.like

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlin.math.PI
import kotlin.math.sin

private const val DrawMillis = 1200
private const val FillMillis = 3000f
private const val AutoHoldMillis = 1800L
private val HeartSide = 170.dp

@Composable
fun OutlineFillDemo() {
    var epoch by remember { mutableIntStateOf(0) }
    val draw = remember { Animatable(0f) }
    var level by remember { mutableFloatStateOf(0f) }
    // The script's hold and a finger's hold are kept apart so cancelling the script never lifts a real finger.
    var touchHold by remember { mutableStateOf(false) }
    var autoHold by remember { mutableStateOf(false) }
    val holding = touchHold || autoHold
    val full by remember { derivedStateOf { level >= 1f } }
    val waving by remember { derivedStateOf { level > 0f && level < 1f } }
    val pulse = remember { Animatable(1f) }

    LaunchedEffect(epoch) {
        draw.snapTo(0f)
        delay(700)
        draw.animateTo(1f, tween(DrawMillis, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(holding) {
        if (!holding) return@LaunchedEffect
        var last = withFrameNanos { it }
        while (level < 1f) {
            withFrameNanos { now ->
                level = (level + (now - last) / 1_000_000f / FillMillis).coerceAtMost(1f)
                last = now
            }
        }
    }
    LaunchedEffect(full) {
        if (!full) {
            pulse.animateTo(1f, tween(150))
            return@LaunchedEffect
        }
        while (true) {
            pulse.animateTo(1.1f, tween(140))
            pulse.animateTo(1f, tween(160))
            pulse.animateTo(1.06f, tween(120))
            pulse.animateTo(1f, tween(220))
            delay(650)
        }
    }
    val wave = if (waving) {
        rememberInfiniteTransition(label = "wave").animateFloat(
            initialValue = 0f,
            targetValue = 2f * PI.toFloat(),
            animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing)),
            label = "wavePhase",
        )
    } else {
        null
    }

    LikeStage(
        5,
        "Outline Fill",
        "The outline draws itself; press and hold to pour in rising liquid, and a full heart beats.",
        onReplay = {
            epoch++
            level = 0f
            autoHold = false
        },
        autoplay = {
            snapshotFlow { draw.value }.first { it >= 1f }
            delay(300)
            autoHold = true
            try {
                delay(AutoHoldMillis)
            } finally {
                autoHold = false
            }
        },
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(
                Modifier
                    .size(HeartSide + 40.dp)
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown()
                            touchHold = true
                            waitForUpOrCancellation()
                            touchHold = false
                        }
                    }
                    .drawWithCache {
                        val side = HeartSide.toPx()
                        val corner = size.center - Offset(side / 2, side / 2)
                        val heart = Path().apply { heart(corner, side) }
                        val bounds = heart.getBounds()
                        val measure = PathMeasure().apply { setPath(heart, false) }
                        val length = measure.length
                        val trimmed = Path()
                        val front = Path()
                        val back = Path()
                        val liquid = Brush.verticalGradient(listOf(LikeColors.Pink, LikeColors.Violet), bounds.top, bounds.bottom)
                        val ink = Brush.linearGradient(listOf(LikeColors.Pink, LikeColors.Violet), corner, corner + Offset(side, side))
                        val stroke = Stroke(4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        val amp = 6.dp.toPx()
                        val step = 4.dp.toPx()
                        val k = 2f * PI.toFloat() / (side * 0.6f)
                        onDrawBehind {
                            val drawn = draw.value
                            val fill = level
                            val phase = wave?.value ?: 0f
                            scale(pulse.value, center) {
                                if (fill > 0f) {
                                    val surface = bounds.bottom + amp - fill * (bounds.height + 2 * amp)
                                    back.wave(bounds.left, bounds.right, bounds.bottom, surface - amp * 0.4f, amp * 0.8f, -phase + 1.7f, k, step)
                                    front.wave(bounds.left, bounds.right, bounds.bottom, surface, amp, phase, k, step)
                                    clipPath(heart) {
                                        drawPath(back, liquid, alpha = 0.45f)
                                        drawPath(front, liquid)
                                    }
                                }
                                if (drawn > 0f) {
                                    trimmed.reset()
                                    measure.getSegment(0f, length * drawn, trimmed, true)
                                    drawPath(trimmed, ink, style = stroke)
                                }
                            }
                        }
                    },
            )
            Text(
                if (full) "Full of love" else "Press and hold to fill",
                color = LikeColors.Outline,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

/** Replaces this path with water below a sine surface at [surface], filled down to [bottom]. */
private fun Path.wave(
    left: Float,
    right: Float,
    bottom: Float,
    surface: Float,
    amp: Float,
    phase: Float,
    k: Float,
    step: Float,
) {
    reset()
    moveTo(left, bottom)
    var x = left
    while (x < right) {
        lineTo(x, surface + sin(phase + (x - left) * k) * amp)
        x += step
    }
    lineTo(right, surface + sin(phase + (right - left) * k) * amp)
    lineTo(right, bottom)
    close()
}
