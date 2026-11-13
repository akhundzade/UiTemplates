package com.azersun.uitemplates.demos.pullrefresh

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.azersun.uitemplates.demos.menus.MenuColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

private const val PullTurnDegrees = 270f
private const val ArcSweep = 130f
private val Success = Color(0xFF34D399)
private val Disc = Color(0xFF2E2A5C)

@Composable
fun SyncSpinRefreshDemo() {
    val state = rememberPullRefreshState(doneHoldMillis = 550)
    PullRefreshScene(
        number = 3,
        title = "Sync Spin",
        caption = "A refresh icon turns with the pull, spins while it loads, then morphs into a check.",
        state = state,
    ) {
        SyncSpinIndicator(state)
    }
}

@Composable
private fun SyncSpinIndicator(state: PullRefreshState) {
    val phase = state.phase
    val spin = remember { Animatable(0f) }
    val check = remember { Animatable(0f) }

    LaunchedEffect(phase) {
        when (phase) {
            RefreshPhase.Refreshing -> {
                check.snapTo(0f)
                spin.snapTo(state.progress * PullTurnDegrees)
                while (true) spin.animateTo(spin.value + 360f, tween(750, easing = LinearEasing))
            }
            RefreshPhase.Done -> check.animateTo(1f, tween(380, easing = FastOutSlowInEasing))
            RefreshPhase.Idle, RefreshPhase.Pulling -> check.snapTo(0f)
            RefreshPhase.Collapsing -> Unit
        }
    }

    Canvas(Modifier.fillMaxSize()) {
        val pull = state.pull
        val angle = if (state.isLoading) spin.value else state.progress * PullTurnDegrees
        val morph = check.value
        if (pull <= 1f) return@Canvas
        val disc = 18.dp.toPx()
        val fit = (pull / (disc * 2 + 12.dp.toPx())).coerceIn(0f, 1f)
        val center = Offset(size.width / 2, pull / 2)

        scale(fit, pivot = center) {
            drawCircle(lerp(Disc, Success, morph), disc, center, alpha = fit)
            val radius = 8.5.dp.toPx()
            val stroke = 2.4.dp.toPx()
            val arrows = (1f - morph).coerceIn(0f, 1f)
            if (arrows > 0f) {
                rotate(angle, pivot = center) {
                    for (start in listOf(0f, 180f)) {
                        drawSyncArrow(center, radius, start, ArcSweep * arrows, stroke, lerp(MenuColors.Cyan, Color.White, morph), arrows)
                    }
                }
            }
            if (morph > 0f) drawCheck(center, morph, stroke)
        }
    }
}

/** One arc of the two-arrow refresh glyph, with its arrowhead pointing clockwise at the arc's end. */
private fun DrawScope.drawSyncArrow(center: Offset, radius: Float, start: Float, sweep: Float, stroke: Float, color: Color, alpha: Float) {
    drawArc(
        color,
        start,
        sweep,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(stroke, cap = StrokeCap.Round),
        alpha = alpha,
    )
    val end = (start + sweep) * PI.toFloat() / 180f
    val tip = center + Offset(cos(end), sin(end)) * radius
    val along = Offset(-sin(end), cos(end))
    val out = Offset(cos(end), sin(end))
    val head = 3.6.dp.toPx()
    val arrow = Path().apply {
        moveTo(tip.x + along.x * head, tip.y + along.y * head)
        lineTo(tip.x + out.x * head, tip.y + out.y * head)
        lineTo(tip.x - out.x * head, tip.y - out.y * head)
        close()
    }
    drawPath(arrow, color, alpha = alpha)
}

/** A check mark drawn on from its short stroke to its long one as [progress] goes 0 to 1. */
private fun DrawScope.drawCheck(center: Offset, progress: Float, stroke: Float) {
    val a = center + Offset(-6.dp.toPx(), 0.5.dp.toPx())
    val b = center + Offset(-1.8.dp.toPx(), 4.5.dp.toPx())
    val c = center + Offset(6.5.dp.toPx(), -4.5.dp.toPx())
    val first = hypot(b.x - a.x, b.y - a.y)
    val second = hypot(c.x - b.x, c.y - b.y)
    val drawn = (first + second) * progress
    val firstEnd = if (drawn >= first) b else a + (b - a) * (drawn / first)
    drawLine(Color.White, a, firstEnd, stroke, StrokeCap.Round)
    if (drawn > first) {
        drawLine(Color.White, b, b + (c - b) * ((drawn - first) / second), stroke, StrokeCap.Round)
    }
}
