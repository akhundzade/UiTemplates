package com.azersun.uitemplates.demos.viewas

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

private const val TwoPi = (2 * PI).toFloat()
private val RiseEasing = CubicBezierEasing(0.3f, 0.1f, 0.25f, 1f)

@Composable
fun LiquidFillDemo() {
    val state = rememberViewAsState()
    val level = remember { Animatable(0f) }
    var shown by remember { mutableStateOf(false) }

    LaunchedEffect(state.open) {
        if (state.open) {
            shown = true
            level.animateTo(1f, tween(900, easing = RiseEasing))
        } else if (shown) {
            level.animateTo(0f, tween(620, easing = FastOutSlowInEasing))
            shown = false
        }
    }

    ViewAsScene(state, 3, "Liquid Fill", "The menu fills up like liquid with a wavy edge; drains on pick.") {
        if (shown) {
            val phase by rememberInfiniteTransition(label = "wave").animateFloat(
                initialValue = 0f,
                targetValue = TwoPi,
                animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
                label = "phase",
            )
            ViewAsMenu(state, Modifier.liquid(level = { level.value }, phase = { phase }))
        }
    }
}

/** Clips the content below a travelling wave whose resting line rises with [level]. */
private fun Modifier.liquid(level: () -> Float, phase: () -> Float): Modifier = drawWithCache {
    val amplitude = 5.dp.toPx()
    val step = 4.dp.toPx()
    val margin = 24.dp.toPx()
    val k = TwoPi / (size.width * 0.85f)
    val fill = Path()
    val edge = Path()
    val panel = Path().apply { addRoundRect(RoundRect(size.toRect(), CornerRadius(MenuCorner.toPx()))) }
    val glow = Stroke(6.dp.toPx(), cap = StrokeCap.Round)
    val line = Stroke(2.dp.toPx(), cap = StrokeCap.Round)
    onDrawWithContent {
        val l = level()
        if (l >= 1f) {
            drawContent()
            return@onDrawWithContent
        }
        val p = phase()
        // Spans one amplitude beyond each end so the wave is fully off the panel at 0 and 1.
        val rest = (size.height + amplitude * 2) * (1f - l) - amplitude
        fill.reset()
        edge.reset()
        fill.moveTo(-margin, size.height + margin)
        var x = -margin
        var first = true
        while (x <= size.width + margin) {
            val y = rest + amplitude * (sin(k * x + p) + 0.35f * sin(2 * k * x - 2 * p)) / 1.35f
            fill.lineTo(x, y)
            if (first) edge.moveTo(x, y) else edge.lineTo(x, y)
            first = false
            x += step
        }
        fill.lineTo(size.width + margin, size.height + margin)
        fill.close()
        clipPath(fill) { this@onDrawWithContent.drawContent() }
        clipPath(panel) {
            drawPath(edge, WaveBlue, alpha = 0.3f, style = glow)
            drawPath(edge, WaveBlue, style = line)
        }
    }
}
