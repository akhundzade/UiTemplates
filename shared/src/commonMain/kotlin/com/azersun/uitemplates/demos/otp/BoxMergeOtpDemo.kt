package com.azersun.uitemplates.demos.otp

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val CheckSize = 26.dp
private val CheckGap = 10.dp

@Composable
fun BoxMergeOtpDemo() {
    val state = remember { OtpState() }
    OtpScene(4, "Box Merge", "Digits pop in; on success the six boxes slide into one bar.", state) { metrics ->
        MergeRow(state, metrics)
    }
}

/**
 * Left edge of box [index] when the gaps are [merge] closed. The group of boxes plus the ✓ that
 * follows them is kept centred, so the ✓ fits inside the row's original width.
 */
private fun boxLeft(index: Int, merge: Float, metrics: OtpMetrics): Dp {
    val gap = metrics.gap * (1f - merge)
    val group = metrics.boxWidth * CodeLength + gap * (CodeLength - 1) + (CheckGap + CheckSize) * merge
    return (metrics.rowWidth - group) / 2 + (metrics.boxWidth + gap) * index
}

@Composable
private fun MergeRow(state: OtpState, metrics: OtpMetrics) {
    val merge = remember { Animatable(0f) }
    val check = remember { Animatable(0f) }
    LaunchedEffect(state.verified) {
        if (!state.verified) {
            check.snapTo(0f)
            merge.snapTo(0f)
            return@LaunchedEffect
        }
        launch {
            delay(260)
            check.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 420f))
        }
        merge.animateTo(1f, spring(dampingRatio = 0.78f, stiffness = 240f))
    }
    val active = state.activeIndex

    Box(Modifier.size(metrics.rowWidth, metrics.boxHeight)) {
        Canvas(Modifier.matchParentSize()) {
            val m = merge.value.coerceIn(0f, 1f)
            val w = metrics.boxWidth.toPx()
            val outer = CornerRadius(BoxCorner.toPx())
            val inner = CornerRadius(BoxCorner.toPx() * (1f - m))
            val fill = lerp(OtpColors.BoxFill, OtpColors.SuccessFill, m)
            val path = Path()
            repeat(CodeLength) { i ->
                val x = boxLeft(i, merge.value, metrics).toPx()
                path.reset()
                path.addRoundRect(
                    RoundRect(
                        left = x,
                        top = 0f,
                        right = x + w,
                        bottom = size.height,
                        topLeftCornerRadius = if (i == 0) outer else inner,
                        topRightCornerRadius = if (i == CodeLength - 1) outer else inner,
                        bottomRightCornerRadius = if (i == CodeLength - 1) outer else inner,
                        bottomLeftCornerRadius = if (i == 0) outer else inner,
                    ),
                )
                drawPath(path, fill)
                val focused = i == active
                // Each box's own outline fades as the boxes meet, leaving only the bar's outline.
                drawPath(
                    path,
                    if (focused) OtpColors.Focus else OtpColors.BoxBorder,
                    alpha = 1f - m,
                    style = Stroke((if (focused) 2.dp else 1.dp).toPx()),
                )
            }
            if (m > 0f) {
                val x0 = boxLeft(0, merge.value, metrics).toPx()
                val x1 = boxLeft(CodeLength - 1, merge.value, metrics).toPx() + w
                drawRoundRect(OtpColors.Teal, Offset(x0, 0f), Size(x1 - x0, size.height), outer, alpha = m, style = Stroke(2.dp.toPx()))
            }
        }
        repeat(CodeLength) { i ->
            val digit = state.code.getOrNull(i)
            Box(
                Modifier
                    .size(metrics.boxWidth, metrics.boxHeight)
                    .graphicsLayer { translationX = boxLeft(i, merge.value, metrics).toPx() },
                contentAlignment = Alignment.Center,
            ) {
                if (digit != null) PopDigit(digit)
                if (i == active) BlinkingCaret()
            }
        }
        Canvas(
            Modifier
                .size(CheckSize)
                .align(Alignment.CenterStart)
                .graphicsLayer {
                    translationX = (boxLeft(CodeLength - 1, merge.value, metrics) + metrics.boxWidth + CheckGap).toPx()
                    scaleX = check.value
                    scaleY = check.value
                    alpha = check.value.coerceIn(0f, 1f)
                },
        ) {
            drawCircle(OtpColors.Teal)
            drawCheck(OtpColors.OnTeal, strokeWidth = 2.4.dp.toPx())
        }
    }
}
