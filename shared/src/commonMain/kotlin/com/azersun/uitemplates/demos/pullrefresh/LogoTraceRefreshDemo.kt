package com.azersun.uitemplates.demos.pullrefresh

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import com.azersun.uitemplates.demos.menus.MenuColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Share of the trace spent on the hexagon; the inner circle draws in the rest. */
private const val HexShare = 0.72f
private val LogoRadius = 18.dp

@Composable
fun LogoTraceRefreshDemo() {
    val state = rememberPullRefreshState()
    PullRefreshScene(
        number = 6,
        title = "Logo Trace",
        caption = "The logo draws itself as you pull, then fills with colour and spins while it refreshes.",
        state = state,
    ) {
        LogoTraceIndicator(state)
    }
}

@Composable
private fun LogoTraceIndicator(state: PullRefreshState) {
    val loading = state.isLoading
    val spin = remember { Animatable(0f) }
    val fill = remember { Animatable(0f) }

    LaunchedEffect(loading) {
        if (!loading) {
            fill.snapTo(0f)
            spin.snapTo(0f)
            return@LaunchedEffect
        }
        fill.animateTo(1f, tween(320))
        while (true) spin.animateTo(spin.value + 360f, tween(1100, easing = LinearEasing))
    }

    Box(
        Modifier
            .fillMaxSize()
            .drawWithCache {
                val radius = LogoRadius.toPx()
                // Pointy-top hexagon around the origin; the logo is translated into place when drawn.
                val hexagon = Path().apply {
                    repeat(6) { i ->
                        val a = (-90f + 60f * i) * PI.toFloat() / 180f
                        val corner = Offset(cos(a), sin(a)) * radius
                        if (i == 0) moveTo(corner.x, corner.y) else lineTo(corner.x, corner.y)
                    }
                    close()
                }
                val circle = Path().apply { addOval(Rect(Offset.Zero, radius * 0.38f)) }
                val hexMeasure = PathMeasure().apply { setPath(hexagon, false) }
                val circleMeasure = PathMeasure().apply { setPath(circle, false) }
                val tracedHex = Path()
                val tracedCircle = Path()
                val gradient = Brush.linearGradient(listOf(MenuColors.Violet, MenuColors.Cyan), Offset(-radius, -radius), Offset(radius, radius))
                val stroke = Stroke(2.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                val needed = radius * 2 + 14.dp.toPx()

                onDrawBehind {
                    val pull = state.pull
                    val angle = spin.value
                    val filled = fill.value
                    if (pull <= 1f) return@onDrawBehind
                    val trace = if (state.isLoading) 1f else state.progress.coerceIn(0f, 1f)
                    val fit = (pull / needed).coerceIn(0f, 1f)

                    tracedHex.reset()
                    hexMeasure.getSegment(0f, hexMeasure.length * (trace / HexShare).coerceAtMost(1f), tracedHex, true)
                    tracedCircle.reset()
                    val circleTrace = ((trace - HexShare) / (1f - HexShare)).coerceIn(0f, 1f)
                    if (circleTrace > 0f) circleMeasure.getSegment(0f, circleMeasure.length * circleTrace, tracedCircle, true)

                    translate(size.width / 2, pull / 2) {
                        scale(fit, pivot = Offset.Zero) {
                            rotate(angle, pivot = Offset.Zero) {
                                if (filled > 0f) drawPath(hexagon, gradient, alpha = filled)
                                drawPath(tracedHex, gradient, style = stroke)
                                if (filled > 0f) drawPath(circle, MenuColors.Card, alpha = filled)
                                drawPath(tracedCircle, if (filled > 0.5f) MenuColors.Card else MenuColors.Cyan, style = stroke)
                            }
                        }
                    }
                }
            },
    )
}
