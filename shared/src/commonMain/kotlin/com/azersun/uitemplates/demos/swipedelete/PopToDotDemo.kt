package com.azersun.uitemplates.demos.swipedelete

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import com.azersun.uitemplates.demos.menus.MenuColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private val SparkColors = listOf(DeleteRed, DeleteRedLight, MenuColors.Yellow)
private const val SparkCount = 12
private const val ShrinkStart = 0.14f
private const val ShrinkEnd = 0.56f
private const val PopAt = 0.62f

@Composable
fun PopToDotDemo() {
    SwipeDeleteScene(
        number = 2,
        title = "Pop to Dot",
        caption = "The row turns red, shrinks into a dot and pops into sparks.",
        spec = SwipeSpec(exitMillis = 900, collapseAt = 0.7f),
    ) { message, state -> PopRow(message, state) }
}

@Composable
private fun PopRow(message: Message, state: SwipeRowState) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(RowHeight)
            .drawWithContent {
                val e = state.exit.value
                val drag = state.drag.value
                val w = size.width
                val h = RowHeight.toPx()
                val dot = 14.dp.toPx()
                val deleting = state.deleting
                val shrink = if (deleting) FastOutSlowInEasing.transform(phase(e, ShrinkStart, ShrinkEnd)) else 0f
                // The row drifts back to the middle while it turns red, so the dot lands centred.
                val shift = if (deleting) {
                    state.releasedDrag * (1f - FastOutSlowInEasing.transform(phase(e, 0f, 0.4f)))
                } else {
                    drag
                }
                val tint = if (deleting) {
                    0.85f * LinearOutSlowInEasing.transform(phase(e, 0f, 0.16f)) + 0.15f * shrink
                } else {
                    0.22f * (drag / (w * DeleteThreshold)).coerceIn(0f, 1f)
                }
                val rectW = lerp(w, dot, shrink)
                val rectH = lerp(h, dot, shrink)
                val center = Offset(w / 2 - shift, h / 2)

                if (shrink < 1f) {
                    withTransform({
                        translate(-shift, 0f)
                        scale(rectW / w, rectH / h, Offset(w / 2, h / 2))
                    }) { this@drawWithContent.drawContent() }
                }
                if (tint > 0f && e < PopAt) {
                    val swell = 1f + 0.45f * sin(PI.toFloat() * phase(e, ShrinkEnd, PopAt))
                    val rw = rectW * swell
                    val rh = rectH * swell
                    val radius = lerp(16.dp.toPx(), rh / 2, shrink).coerceAtMost(min(rw, rh) / 2)
                    drawRoundRect(
                        DeleteRed,
                        topLeft = Offset(center.x - rw / 2, center.y - rh / 2),
                        size = Size(rw, rh),
                        cornerRadius = CornerRadius(radius),
                        alpha = tint.coerceIn(0f, 1f),
                    )
                }
                if (deleting && e > PopAt - 0.02f) drawSparks(center, dot, phase(e, PopAt - 0.02f, 1f))
            },
    ) {
        MessageRow(message)
    }
}

private fun DrawScope.drawSparks(center: Offset, dot: Float, progress: Float) {
    if (progress >= 1f) return
    val travel = LinearOutSlowInEasing.transform(progress)
    val fade = 1f - progress
    drawCircle(
        DeleteRedLight,
        radius = dot / 2 + 24.dp.toPx() * travel,
        center = center,
        alpha = 0.55f * fade,
        style = Stroke(2.dp.toPx() * fade),
    )
    val reach = 34.dp.toPx()
    val length = 9.dp.toPx() * fade + 2.dp.toPx()
    repeat(SparkCount) { i ->
        val angle = (i * 2 * PI / SparkCount).toFloat()
        val dir = Offset(cos(angle), sin(angle))
        val inner = dot * 0.6f + reach * travel * (if (i % 2 == 0) 1f else 0.78f)
        drawLine(
            SparkColors[i % SparkColors.size],
            center + dir * inner,
            center + dir * (inner + length),
            strokeWidth = 2.5.dp.toPx(),
            cap = StrokeCap.Round,
            alpha = fade,
        )
    }
}
