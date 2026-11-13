package com.azersun.uitemplates.demos.password

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp
import kotlin.math.abs

private const val FlowMillis = 650

// Segments per second at which the leading edge glows at full strength.
private const val GlowSpeed = 2.5f

@Composable
fun FlowFillDemo() {
    val form = remember { PasswordFormState() }
    val fill = remember { Animatable(0f) }
    LaunchedEffect(form.level) {
        fill.animateTo(form.level.toFloat(), tween(FlowMillis, easing = FastOutSlowInEasing))
    }
    val color by animateLevelColor(form.level, FlowMillis)

    PasswordStage(1, "Flow Fill", "One liquid fill glides through the gaps and blends to the new color.", form) {
        Canvas(Modifier.fillMaxSize()) {
            drawTrack()
            val end = fillEndPx(size.width, SegmentGap.toPx(), fill.value)
            if (end <= 0f) return@Canvas
            val c = color
            clipPath(segmentsPath()) { drawRect(c, size = Size(end, size.height)) }
            val glow = (abs(fill.velocity) / GlowSpeed).coerceIn(0f, 1f)
            if (glow <= 0f) return@Canvas
            // Unclipped head: a thin stream that stays visible while the edge is inside a gap.
            val head = 10.dp.toPx()
            val stream = size.height * 0.6f
            drawRoundRect(
                c.copy(alpha = glow),
                topLeft = Offset(end - head, (size.height - stream) / 2),
                size = Size(head, stream),
                cornerRadius = CornerRadius(stream / 2),
            )
            val radius = 14.dp.toPx()
            val tip = Offset(end, size.height / 2)
            drawCircle(
                Brush.radialGradient(listOf(c.copy(alpha = 0.6f * glow), Color.Transparent), center = tip, radius = radius),
                radius = radius,
                center = tip,
            )
        }
    }
}
