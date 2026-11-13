package com.azersun.uitemplates.common

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

private val ConfettiColors = listOf(
    Color(0xFF7C6CF2), Color(0xFF22B07D), Color(0xFFFFC53D), Color(0xFFFF5C8A),
    Color(0xFF3DA5FF), Color(0xFFFF8A3D), Color(0xFF1F2430),
)

@Stable
class ConfettiState {
    internal val bursts = mutableStateListOf<Burst>()

    /** Fires a burst centred on [center], in the coordinates of the [Confetti] canvas. */
    fun burst(center: Offset) {
        bursts += Burst(center, Random.nextInt())
    }

    fun clear() = bursts.clear()
}

internal class Burst(val center: Offset, val seed: Int) {
    var startNanos = -1L
}

@Composable
fun rememberConfettiState(): ConfettiState = remember { ConfettiState() }

/**
 * Physics confetti: each piece is launched upwards with air drag and gravity, spins, and flips
 * (scaleX wobble) like paper. Positions are analytic in time, so nothing is allocated per frame.
 */
@Composable
fun Confetti(
    state: ConfettiState,
    modifier: Modifier = Modifier,
    pieces: Int = 90,
    durationMillis: Int = 2600,
) {
    var frameNanos by remember { mutableLongStateOf(0L) }
    val active = state.bursts.isNotEmpty()

    LaunchedEffect(active) {
        while (state.bursts.isNotEmpty()) {
            withFrameNanos { now ->
                frameNanos = now
                state.bursts.forEach { if (it.startNanos < 0) it.startNanos = now }
                state.bursts.removeAll { now - it.startNanos > durationMillis * 1_000_000L }
            }
        }
    }

    Canvas(modifier) {
        // Read the clock before any early return so every frame invalidates this draw.
        val now = frameNanos
        if (!active) return@Canvas
        val dp = density
        val gravity = 650f * dp
        val drag = 3f
        val duration = durationMillis / 1000f
        state.bursts.forEach { burst ->
            if (burst.startNanos < 0) return@forEach
            val t = (now - burst.startNanos) / 1_000_000_000f
            val random = Random(burst.seed)
            repeat(pieces) {
                val angle = (-PI / 2 + (random.nextFloat() - 0.5f) * PI * 0.95f).toFloat()
                val speed = (260f + random.nextFloat() * 520f) * dp
                val vx = cos(angle) * speed
                val vy = sin(angle) * speed
                val decay = (1f - exp(-drag * t)) / drag
                val x = burst.center.x + vx * decay
                val y = burst.center.y + vy * decay + gravity / drag * (t - decay)
                val spin = (random.nextFloat() - 0.5f) * 1440f
                val wobble = 4f + random.nextFloat() * 8f
                val w = (5f + random.nextFloat() * 4f) * dp
                val h = (8f + random.nextFloat() * 6f) * dp
                val round = random.nextFloat() < 0.25f
                val color = ConfettiColors[random.nextInt(ConfettiColors.size)]
                val alpha = ((duration - t) / (duration * 0.3f)).coerceIn(0f, 1f)
                withTransform({
                    translate(x, y)
                    rotate(spin * t, Offset.Zero)
                    scale(cos(t * wobble), 1f, Offset.Zero)
                }) {
                    if (round) {
                        drawCircle(color, radius = w * 0.55f, center = Offset.Zero, alpha = alpha)
                    } else {
                        drawRect(color, topLeft = Offset(-w / 2, -h / 2), size = Size(w, h), alpha = alpha)
                    }
                }
            }
        }
    }
}
