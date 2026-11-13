package com.azersun.uitemplates.demos.buttonstate

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp

private const val SweepMillis = 1000
private const val TailDots = 16
private val TailSpacing = 3.5.dp
private val SeamWidth = 22.dp

@Composable
fun EdgeCometDemo() {
    var base by remember { mutableStateOf(StartState) }
    var next by remember { mutableStateOf<PlayState?>(null) }
    // 0..1 of the way from the left end to the right end, along either half of the outline.
    val sweep = remember { Animatable(0f) }

    ButtonStateScene(
        number = 5,
        title = "Edge Comet",
        caption = "Two comets race around the pill's edge in opposite directions, dragging the new colour behind them.",
        onToggle = { _, to, _ ->
            next = to
            sweep.snapTo(0f)
            sweep.animateTo(1f, tween(SweepMillis, easing = FastOutSlowInEasing))
            base = to
            next = null
        },
    ) {
        Box(Modifier.matchParentSize().clip(Pill)) {
            PillFace(base)
            next?.let { state ->
                PillFace(
                    state,
                    Modifier.drawWithContent {
                        val t = sweep.value
                        val x = cometPoint(t, size).x
                        clipRect(right = x) { this@drawWithContent.drawContent() }
                        // A soft light seam where the new colour meets the old one.
                        val seam = SeamWidth.toPx()
                        drawRect(
                            Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.35f)), x - seam, x),
                            topLeft = Offset(x - seam, 0f),
                            size = Size(seam, size.height),
                            alpha = 1f - phase(t, 0.85f, 1f),
                        )
                    },
                )
            }
        }
        Canvas(Modifier.matchParentSize()) {
            val t = sweep.value
            val color = next?.start ?: return@Canvas
            val fade = phase(t, 0f, 0.06f) * (1f - phase(t, 0.9f, 1f))
            drawComet(t, clockwise = true, color, fade)
            drawComet(t, clockwise = false, color, fade)
        }
    }
}

/** Where the comet running over the top is after [t] of its half lap; the other mirrors it. */
private fun cometPoint(t: Float, size: Size): Offset =
    stadiumPoint(t * stadiumPerimeter(size.width, size.height) / 2, size.width, size.height)

private fun DrawScope.drawComet(t: Float, clockwise: Boolean, color: Color, alpha: Float) {
    val perimeter = stadiumPerimeter(size.width, size.height)
    val head = t * perimeter / 2
    val spacing = TailSpacing.toPx()
    for (k in TailDots downTo 1) {
        val behind = head - k * spacing
        if (behind < 0f) continue
        val at = stadiumPoint(if (clockwise) behind else perimeter - behind, size.width, size.height)
        val fade = 1f - k / (TailDots + 1f)
        drawCircle(lerp(color, Color.White, fade * 0.6f), 3.dp.toPx() * fade, at, alpha = alpha * fade * 0.8f)
    }
    val at = stadiumPoint(if (clockwise) head else perimeter - head, size.width, size.height)
    drawCircle(color, 11.dp.toPx(), at, alpha = alpha * 0.25f)
    drawCircle(color, 6.dp.toPx(), at, alpha = alpha * 0.55f)
    drawCircle(Color.White, 3.2.dp.toPx(), at, alpha = alpha)
}
