package com.azersun.uitemplates.demos.pullrefresh

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.azersun.uitemplates.demos.menus.MenuColors
import kotlin.math.sqrt

private const val Coils = 6
private val WireLength = 9.dp
private val WeightSize = 18.dp
private val WeightTop = 6.dp

@Composable
fun SpringCoilRefreshDemo() {
    // Underdamped, so the list bounces on the spring when it is let go.
    val state = rememberPullRefreshState(releaseSpec = spring(dampingRatio = 0.28f, stiffness = 240f))
    PullRefreshScene(
        number = 2,
        title = "Spring Coil",
        caption = "A coil spring stretches with the pull and bounces when you let go, a little weight hanging on top.",
        state = state,
    ) {
        SpringCoilIndicator(state)
    }
}

@Composable
private fun SpringCoilIndicator(state: PullRefreshState) {
    val refreshing = state.phase == RefreshPhase.Refreshing
    val wiggle = remember { Animatable(0f) }

    LaunchedEffect(refreshing) {
        if (!refreshing) {
            wiggle.animateTo(0f, spring(dampingRatio = 0.5f, stiffness = 400f))
            return@LaunchedEffect
        }
        while (true) {
            wiggle.animateTo(10f, tween(240, easing = FastOutSlowInEasing))
            wiggle.animateTo(-10f, tween(240, easing = FastOutSlowInEasing))
        }
    }

    Canvas(Modifier.fillMaxSize()) {
        val pull = state.pull
        val angle = wiggle.value
        if (pull <= 1f) return@Canvas
        val cx = size.width / 2
        val weight = WeightSize.toPx()
        val weightTop = WeightTop.toPx()
        val coilTop = weightTop + weight
        val coilBottom = pull - 2.dp.toPx()
        val appear = (pull / (coilTop + 8.dp.toPx())).coerceIn(0f, 1f)
        val wireBrush = Brush.verticalGradient(
            listOf(MenuColors.Violet, MenuColors.Cyan),
            startY = coilTop,
            endY = coilBottom.coerceAtLeast(coilTop + 1f),
        )

        clipRect(bottom = pull) {
            if (coilBottom > coilTop) {
                // A fixed wire length per half-coil: stretching the spring makes it narrower.
                val step = (coilBottom - coilTop) / (2 * Coils)
                val wire = WireLength.toPx()
                val amplitude = sqrt((wire * wire - step * step).coerceAtLeast(0f)).coerceIn(1.5.dp.toPx(), wire)
                val coil = Path().apply {
                    moveTo(cx, coilTop)
                    for (k in 1 until 2 * Coils) {
                        lineTo(cx + if (k % 2 == 1) amplitude else -amplitude, coilTop + step * k)
                    }
                    lineTo(cx, coilBottom)
                }
                drawPath(coil, wireBrush, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round), alpha = appear)
                val foot = 9.dp.toPx()
                drawLine(
                    MenuColors.Cyan,
                    Offset(cx - foot, coilBottom),
                    Offset(cx + foot, coilBottom),
                    2.5.dp.toPx(),
                    StrokeCap.Round,
                    alpha = appear,
                )
            }
            rotate(angle, pivot = Offset(cx, coilTop)) {
                drawRoundRect(
                    Brush.linearGradient(
                        listOf(MenuColors.Pink, MenuColors.Violet),
                        Offset(cx - weight / 2, weightTop),
                        Offset(cx + weight / 2, coilTop),
                    ),
                    topLeft = Offset(cx - weight / 2, weightTop),
                    size = Size(weight, weight),
                    cornerRadius = CornerRadius(4.dp.toPx()),
                    alpha = appear,
                )
            }
        }
    }
}
