package com.azersun.uitemplates.demos.screentransition

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp
import com.azersun.uitemplates.demos.menus.MenuColors
import kotlin.math.PI
import kotlin.math.sin

private val WaveAmplitude = 18.dp
private val WaveOvershoot = 30.dp
private val WaveStep = 6.dp
private const val WaveCycles = 1.6f

/** How many wavelengths the ripple travels along the edge during one wipe. */
private const val WaveDrift = 1.5f
private const val BulgeFactor = 0.8f
private val WipeEasing = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)

@Composable
fun LiquidWipeTransitionDemo() {
    val nav = rememberNavigator(tween(1000, easing = WipeEasing))
    TransitionScene(
        number = 6,
        title = "Liquid Wipe",
        caption = "A wavy, glowing edge sweeps across from left to right, revealing the next screen behind it.",
        nav = nav,
    ) { explore, detail, progress ->
        explore()
        Box(
            Modifier
                .fillMaxSize()
                .drawWithCache {
                    val revealed = Path()
                    onDrawWithContent {
                        waveEdge(revealed, progress(), closed = true)
                        clipPath(revealed) { this@onDrawWithContent.drawContent() }
                    }
                },
        ) {
            detail()
        }
        Spacer(
            Modifier
                .fillMaxSize()
                .drawWithCache {
                    val edge = Path()
                    val glow = Brush.verticalGradient(listOf(MenuColors.Cyan, MenuColors.Violet, MenuColors.Pink, MenuColors.Cyan))
                    val wide = Stroke(18.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    val mid = Stroke(7.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    val core = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    onDrawBehind {
                        val p = progress()
                        val shown = smoothstep(0f, 0.06f, p) * (1f - smoothstep(0.94f, 1f, p))
                        if (shown > 0f) {
                            waveEdge(edge, p, closed = false)
                            drawPath(edge, glow, alpha = 0.18f * shown, style = wide)
                            drawPath(edge, glow, alpha = 0.45f * shown, style = mid)
                            drawPath(edge, Color.White, alpha = 0.9f * shown, style = core)
                        }
                    }
                },
        )
    }
}

/**
 * Rebuilds [path] as the wavy front at [progress]: just the edge, or ([closed]) the whole region
 * to its left. The ripple swells mid-wipe and is flat at both ends, so the start and end frames
 * match the plain screens exactly.
 */
private fun DrawScope.waveEdge(path: Path, progress: Float, closed: Boolean) {
    val w = size.width
    val h = size.height
    val swell = sin(PI.toFloat() * progress)
    val amplitude = WaveAmplitude.toPx() * swell
    val reach = WaveOvershoot.toPx() + WaveAmplitude.toPx() * (1f + BulgeFactor)
    val front = mix(-reach, w + reach, progress)
    val k = 2f * PI.toFloat() * WaveCycles / h
    val phase = 2f * PI.toFloat() * WaveDrift * progress
    val step = WaveStep.toPx()
    path.reset()
    if (closed) {
        path.moveTo(-w, 0f)
        path.lineTo(edgeX(front, amplitude, k, phase, 0f, h), 0f)
    } else {
        path.moveTo(edgeX(front, amplitude, k, phase, 0f, h), 0f)
    }
    var y = step
    while (y < h) {
        path.lineTo(edgeX(front, amplitude, k, phase, y, h), y)
        y += step
    }
    path.lineTo(edgeX(front, amplitude, k, phase, h, h), h)
    if (closed) {
        path.lineTo(-w, h)
        path.close()
    }
}

/** A ripple plus a bulge that leads in the middle, like liquid pushing ahead of its rim. */
private fun edgeX(front: Float, amplitude: Float, k: Float, phase: Float, y: Float, h: Float): Float =
    front + amplitude * sin(k * y + phase) + amplitude * BulgeFactor * sin(PI.toFloat() * y / h)
