package com.azersun.uitemplates.demos.like2

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.azersun.uitemplates.demos.like.LikeColors
import com.azersun.uitemplates.demos.like.LikeStage
import com.azersun.uitemplates.demos.like.drawLikeHeart
import com.azersun.uitemplates.demos.like.heart
import com.azersun.uitemplates.demos.like.shrink
import kotlinx.coroutines.launch
import kotlin.math.exp

private val HeartSide = 96.dp
private const val EcgMillis = 1500
private const val WaveMillis = 800

/** The second beat's spike trails the first by this fraction of the line's width. */
private const val BeatGap = 0.26f

@Composable
fun HeartbeatDemo() {
    var liked by remember { mutableStateOf(false) }
    val fill = remember { Animatable(0f) }
    val beat = remember { Animatable(1f) }
    // Waves and the ECG run 0 → 1 per like and are hidden at 1.
    val firstWave = remember { Animatable(1f) }
    val secondWave = remember { Animatable(1f) }
    val ecg = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    fun toggle() {
        liked = !liked
        if (!liked) {
            scope.launch { fill.animateTo(0f, tween(200)) }
            scope.launch { beat.shrink() }
            return
        }
        scope.launch { fill.animateTo(1f, tween(180)) }
        scope.launch {
            ecg.snapTo(0f)
            ecg.animateTo(1f, tween(EcgMillis, easing = LinearEasing))
        }
        scope.launch {
            beat.animateTo(1.15f, tween(130, easing = FastOutSlowInEasing))
            launch { firstWave.send() }
            beat.animateTo(0.96f, tween(140))
            beat.animateTo(1.25f, tween(130, easing = FastOutSlowInEasing))
            launch { secondWave.send() }
            beat.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 380f))
        }
    }

    LikeStage(
        6,
        "Heartbeat",
        "Tap: the heart beats twice, sending heart waves, and a pulse line spikes.",
        autoplay = { toggle() },
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(
                Modifier
                    .size(240.dp)
                    .clickable(
                        interactionSource = null,
                        indication = null,
                        onClickLabel = if (liked) "Unlike" else "Like",
                        role = Role.Button,
                        onClick = ::toggle,
                    )
                    .drawWithCache {
                        val side = HeartSide.toPx()
                        val corner = size.center - Offset(side / 2, side / 2)
                        val heart = Path().apply { heart(corner, side) }
                        val brush = Brush.linearGradient(listOf(LikeColors.Pink, LikeColors.Violet), corner, corner + Offset(side, side))
                        val outline = 3.5.dp.toPx()
                        onDrawBehind {
                            val w1 = firstWave.value
                            val w2 = secondWave.value
                            val f = fill.value
                            val b = beat.value
                            fun wave(w: Float) {
                                if (w >= 1f) return
                                val s = 1f + 0.9f * w
                                scale(s, center) {
                                    drawPath(heart, brush, alpha = (1f - w) * 0.7f, style = Stroke(outline * (1.4f - w) / s, join = StrokeJoin.Round))
                                }
                            }
                            wave(w1)
                            wave(w2)
                            scale(b, center) { drawLikeHeart(heart, f, brush, LikeColors.Outline, outline) }
                        }
                    },
            )
            Spacer(
                Modifier
                    .width(280.dp)
                    .height(90.dp)
                    .drawWithCache {
                        val line = Path()
                        val brush = Brush.horizontalGradient(
                            0f to Color.Transparent,
                            0.15f to LikeColors.Pink,
                            0.85f to LikeColors.Violet,
                            1f to Color.Transparent,
                        )
                        val baseline = size.height * 0.62f
                        val amplitude = size.height * 0.5f
                        val unit = size.width * 0.08f
                        val step = 1.dp.toPx()
                        val width = 2.5.dp.toPx()
                        onDrawBehind {
                            val p = ecg.value
                            line.reset()
                            line.moveTo(0f, baseline)
                            if (p < 1f) {
                                // The first spike enters from the left edge and the second follows, both leaving on the right.
                                val head = -BeatGap * size.width + p * (1f + 2 * BeatGap) * size.width
                                var x = 0f
                                while (x <= size.width) {
                                    val y = ecgShape((x - head) / unit) + 0.8f * ecgShape((x - head + BeatGap * size.width) / unit)
                                    line.lineTo(x, baseline - y * amplitude)
                                    x += step
                                }
                                drawPath(line, brush, alpha = 0.25f, style = Stroke(width * 4f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                            } else {
                                line.lineTo(size.width, baseline)
                            }
                            drawPath(line, brush, alpha = if (p < 1f) 1f else 0.55f, style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
                        }
                    },
            )
        }
    }
}

private suspend fun Animatable<Float, AnimationVector1D>.send() {
    snapTo(0f)
    animateTo(1f, tween(WaveMillis, easing = LinearOutSlowInEasing))
}

/** One PQRST complex around u = 0 (the R spike), in units of the spike's width; 0 far from it. */
private fun ecgShape(u: Float): Float {
    if (u < -2.2f || u > 1.6f) return 0f
    return 0.12f * bump(u, -1.4f, 0.28f) -
        0.12f * bump(u, -0.32f, 0.1f) +
        bump(u, 0f, 0.12f) -
        0.35f * bump(u, 0.26f, 0.1f) +
        0.2f * bump(u, 0.95f, 0.3f)
}

private fun bump(u: Float, at: Float, width: Float): Float {
    val d = (u - at) / width
    return exp(-d * d)
}
