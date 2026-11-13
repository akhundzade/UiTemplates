package com.azersun.uitemplates.demos.pullrefresh

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.azersun.uitemplates.demos.menus.MenuColors
import kotlin.math.abs

private const val ChevronCount = 3
private val ChevronColors = listOf(MenuColors.Violet, MenuColors.Blue, MenuColors.Cyan)
private val ChevronGap = 11.dp

@Composable
fun ChevronStackRefreshDemo() {
    val state = rememberPullRefreshState()
    PullRefreshScene(
        number = 4,
        title = "Chevron Stack",
        caption = "Three chevrons light up one by one as you pull; while it refreshes a light runs down them.",
        state = state,
    ) {
        ChevronStackIndicator(state)
    }
}

@Composable
private fun ChevronStackIndicator(state: PullRefreshState) {
    val refreshing = state.phase == RefreshPhase.Refreshing
    val lit = List(ChevronCount) { i ->
        val on by remember {
            derivedStateOf {
                state.progress >= (i + 1f) / ChevronCount || state.phase == RefreshPhase.Refreshing || state.phase == RefreshPhase.Done
            }
        }
        // Underdamped so each chevron overshoots in size as it lights: a small pop.
        animateFloatAsState(if (on) 1f else 0f, spring(dampingRatio = 0.45f, stiffness = 600f), label = "chevron$i")
    }
    // Position of the running light, in chevron indexes; it starts and ends off the stack.
    val runner = remember { Animatable(-1f) }

    LaunchedEffect(refreshing) {
        if (!refreshing) {
            runner.snapTo(-1f)
            return@LaunchedEffect
        }
        while (true) {
            runner.snapTo(-1f)
            runner.animateTo(ChevronCount.toFloat(), tween(820, easing = LinearEasing))
        }
    }

    Canvas(Modifier.fillMaxSize()) {
        val pull = state.pull
        val run = runner.value
        val levels = lit.map { it.value }
        if (pull <= 1f) return@Canvas
        val gap = ChevronGap.toPx()
        val halfWidth = 10.dp.toPx()
        val halfHeight = 4.5.dp.toPx()
        val stroke = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val dim = MenuColors.Muted.copy(alpha = 0.3f)
        val alpha = (pull / state.thresholdPx * 1.5f).coerceIn(0f, 1f)

        clipRect(bottom = pull) {
            repeat(ChevronCount) { i ->
                val level = levels[i]
                val glow = if (refreshing) (1f - abs(run - i)).coerceIn(0f, 1f) else 0f
                val scale = 0.8f + 0.2f * level
                val cy = pull / 2 + (i - (ChevronCount - 1) / 2f) * gap
                val color = lerp(lerp(dim, ChevronColors[i], level.coerceIn(0f, 1f)), Color.White, glow)
                val chevron = Path().apply {
                    moveTo(size.width / 2 - halfWidth * scale, cy - halfHeight * scale)
                    lineTo(size.width / 2, cy + halfHeight * scale)
                    lineTo(size.width / 2 + halfWidth * scale, cy - halfHeight * scale)
                }
                if (glow > 0f) {
                    drawPath(
                        chevron,
                        ChevronColors[i].copy(alpha = 0.35f * glow),
                        style = Stroke(9.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                        alpha = alpha,
                    )
                }
                drawPath(chevron, color, style = stroke, alpha = alpha)
            }
        }
    }
}
