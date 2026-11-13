package com.azersun.uitemplates.demos.backgrounds

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.pow

private val DotBase = Color(0xFF06070B)
private val DotColor = Color(0xFFB9BCD0)
private val RippleColor = Color(0xFFCFF8FF)
private const val RippleSeconds = 1.8f

private class Ripple(val center: Offset) {
    var startNanos = -1L
}

/** Ripples live in a plain list owned by the frame loop; only [frameNanos] is observed by draw. */
@Stable
private class RippleField {
    val ripples = ArrayList<Ripple>()
    var frameNanos by mutableLongStateOf(0L)
    var running by mutableStateOf(false)

    fun add(center: Offset) {
        ripples += Ripple(center)
        running = true
    }
}

@Composable
fun DotGridDemo() {
    val field = remember { RippleField() }

    LaunchedEffect(field.running) {
        if (!field.running) return@LaunchedEffect
        while (field.ripples.isNotEmpty()) {
            withFrameNanos { now ->
                field.ripples.forEach { if (it.startNanos < 0) it.startNanos = now }
                field.ripples.removeAll { (now - it.startNanos) / 1_000_000_000f > RippleSeconds }
                field.frameNanos = now
            }
        }
        field.running = false
    }

    HeroStage(
        4,
        "Dot Grid",
        "A fading dot grid; each tap sends a ripple through the dots.",
        onTap = field::add,
    ) {
        Spacer(
            Modifier.fillMaxSize().drawWithCache {
                val spacing = 14.dp.toPx()
                val baseRadius = 1.1.dp.toPx()
                val ringSpeed = 560.dp.toPx()
                val ringWidth = 26.dp.toPx()
                val cols = (size.width / spacing).toInt() + 1
                val rows = (size.height / spacing).toInt() + 1
                val left = (size.width - (cols - 1) * spacing) / 2
                val top = (size.height - (rows - 1) * spacing) / 2
                // Brightest toward the bottom-right, fading out toward the top-left.
                val maskCenter = Offset(size.width * 0.9f, size.height * 0.95f)
                val maskRadius = hypot(size.width, size.height) * 0.95f
                val mask = FloatArray(cols * rows) { i ->
                    val d = hypot(left + (i % cols) * spacing - maskCenter.x, top + (i / cols) * spacing - maskCenter.y)
                    (1f - d / maskRadius).coerceIn(0f, 1f).pow(1.5f)
                }
                onDrawBehind {
                    drawRect(DotBase)
                    val now = field.frameNanos
                    val ripples = field.ripples
                    for (i in mask.indices) {
                        val x = left + (i % cols) * spacing
                        val y = top + (i / cols) * spacing
                        var lift = 0f
                        for (r in ripples.indices) {
                            val ripple = ripples[r]
                            if (ripple.startNanos < 0) continue
                            val t = (now - ripple.startNanos) / 1_000_000_000f
                            val z = (hypot(x - ripple.center.x * size.width, y - ripple.center.y * size.height) - t * ringSpeed) / ringWidth
                            if (abs(z) > 3f) continue
                            lift = max(lift, exp(-z * z) * (1f - t / RippleSeconds).coerceIn(0f, 1f))
                        }
                        val m = mask[i]
                        val alpha = (m * 0.4f + lift * (0.4f + 0.6f * m)).coerceAtMost(1f)
                        if (alpha < 0.01f) continue
                        drawCircle(
                            lerp(DotColor, RippleColor, lift),
                            radius = baseRadius * (1f + 1.8f * lift),
                            center = Offset(x, y),
                            alpha = alpha,
                        )
                    }
                }
            },
        )
    }
}
