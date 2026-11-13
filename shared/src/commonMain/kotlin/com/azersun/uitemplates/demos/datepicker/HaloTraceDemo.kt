package com.azersun.uitemplates.demos.datepicker

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.azersun.uitemplates.demos.menus.MenuColors
import kotlinx.coroutines.launch

private const val TraceMillis = 460
private const val FillMillis = 240

/** Ring brightness once the capsule has filled: a faint halo stays on the selected day. */
private const val RestingGlow = 0.35f

@Composable
fun HaloTraceDemo() {
    val state = rememberDatePickerState()
    DatePickerScene(
        state = state,
        number = 1,
        title = "Halo Trace",
        caption = "A glowing ring traces around the tapped day, then the capsule fills with color.",
    ) { _, selected -> HaloCapsule(selected) }
}

@Composable
private fun BoxScope.HaloCapsule(selected: Boolean) {
    val start = if (selected) 1f else 0f
    val trace = remember { Animatable(start) }
    val glow = remember { Animatable(if (selected) RestingGlow else 0f) }
    val fill = remember { Animatable(start) }

    LaunchedEffect(selected) {
        if (selected) {
            if (fill.value == 1f) return@LaunchedEffect
            trace.snapTo(0f)
            glow.snapTo(1f)
            trace.animateTo(1f, tween(TraceMillis, easing = FastOutSlowInEasing))
            launch { glow.animateTo(RestingGlow, tween(360)) }
            fill.animateTo(1f, tween(FillMillis, easing = LinearOutSlowInEasing))
        } else {
            launch { glow.animateTo(0f, tween(200)) }
            fill.animateTo(0f, tween(240))
        }
    }

    Capsule(
        Modifier.graphicsLayer {
            val f = fill.value
            alpha = f
            scaleX = 0.88f + 0.12f * f
            scaleY = 0.88f + 0.12f * f
        },
    )
    Box(
        Modifier
            .matchParentSize()
            .drawWithCache {
                val outset = 3.dp.toPx()
                val outline = capsuleOutline(Rect(-outset, -outset, size.width + outset, size.height + outset))
                val measure = PathMeasure().apply { setPath(outline, false) }
                val length = measure.length
                val segment = Path()
                onDrawBehind {
                    val t = trace.value
                    val g = glow.value
                    if (t <= 0f || g <= 0f) return@onDrawBehind
                    segment.reset()
                    measure.getSegment(0f, length * t, segment, true)
                    drawPath(segment, MenuColors.Cyan, alpha = 0.16f * g, style = Stroke(10.dp.toPx(), cap = StrokeCap.Round))
                    drawPath(segment, MenuColors.Cyan, alpha = 0.4f * g, style = Stroke(5.dp.toPx(), cap = StrokeCap.Round))
                    drawPath(segment, Color.White, alpha = 0.95f * g, style = Stroke(1.8.dp.toPx(), cap = StrokeCap.Round))
                    if (t < 1f) {
                        val head = measure.getPosition(length * t)
                        drawCircle(MenuColors.Cyan, 9.dp.toPx(), head, alpha = 0.3f * g)
                        drawCircle(Color.White, 3.2.dp.toPx(), head, alpha = g)
                    }
                }
            },
    )
}

/** A capsule outline that starts at top centre and runs clockwise, so the trace begins at 12 o'clock. */
private fun capsuleOutline(bounds: Rect): Path {
    val r = bounds.width / 2
    val top = Rect(bounds.left, bounds.top, bounds.right, bounds.top + 2 * r)
    val bottom = Rect(bounds.left, bounds.bottom - 2 * r, bounds.right, bounds.bottom)
    return Path().apply {
        moveTo(bounds.center.x, bounds.top)
        arcTo(top, -90f, 90f, false)
        lineTo(bounds.right, bounds.bottom - r)
        arcTo(bottom, 0f, 180f, false)
        lineTo(bounds.left, bounds.top + r)
        arcTo(top, 180f, 90f, false)
    }
}
