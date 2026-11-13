package com.azersun.uitemplates.demos.otp

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private val LeadSpring: AnimationSpec<Float> = spring(dampingRatio = 0.75f, stiffness = 900f)
private val TrailSpring: AnimationSpec<Float> = spring(dampingRatio = 0.8f, stiffness = 220f)
private val WrapSpring: AnimationSpec<Float> = spring(dampingRatio = 0.7f, stiffness = 260f)

@Composable
fun StretchyFocusOtpDemo() {
    val state = remember { OtpState() }
    OtpScene(2, "Stretchy Focus", "The focus ring stretches box to box, then wraps the whole code.", state) { metrics ->
        val success by animateFloatAsState(if (state.verified) 1f else 0f, tween(320), label = "success")
        Box {
            Row(horizontalArrangement = Arrangement.spacedBy(metrics.gap)) {
                repeat(CodeLength) { i ->
                    val digit = state.code.getOrNull(i)
                    OtpBox(metrics, active = i == state.activeIndex, success = { success }, showFocus = false) {
                        if (digit != null) PopDigit(digit)
                    }
                }
            }
            FocusRing(state, metrics, Modifier.matchParentSize())
        }
    }
}

/**
 * Both edges of the ring are springs measured in box slots. The edge on the side it is heading
 * is stiff and the other soft, so the ring reaches ahead, stretches across the gap, then catches up.
 */
@Composable
private fun FocusRing(state: OtpState, metrics: OtpMetrics, modifier: Modifier) {
    val left = remember { Animatable(0f) }
    val right = remember { Animatable(0f) }
    val wrap = remember { Animatable(0f) }
    val target = if (state.verified) -1 else minOf(state.code.length, CodeLength - 1)

    LaunchedEffect(target) {
        if (target < 0) {
            launch { left.animateTo(0f, WrapSpring) }
            launch { right.animateTo(CodeLength - 1f, WrapSpring) }
            wrap.animateTo(1f, WrapSpring)
        } else {
            val forward = target >= (left.value + right.value) / 2
            launch { wrap.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = 400f)) }
            launch { left.animateTo(target.toFloat(), if (forward) TrailSpring else LeadSpring) }
            right.animateTo(target.toFloat(), if (forward) LeadSpring else TrailSpring)
        }
    }

    Canvas(modifier) {
        val step = (metrics.boxWidth + metrics.gap).toPx()
        val w = wrap.value.coerceIn(0f, 1f)
        val pad = 4.dp.toPx() * w
        val x0 = left.value * step - pad
        val x1 = right.value * step + metrics.boxWidth.toPx() + pad
        val topLeft = Offset(x0, -pad)
        val ringSize = Size(x1 - x0, size.height + pad * 2)
        val corner = CornerRadius(BoxCorner.toPx() + pad)
        val color = lerp(OtpColors.Focus, OtpColors.Teal, w)
        val glow = 6.dp.toPx()
        drawRoundRect(
            color.copy(alpha = 0.22f),
            topLeft = topLeft - Offset(glow / 2, glow / 2),
            size = Size(ringSize.width + glow, ringSize.height + glow),
            cornerRadius = CornerRadius(corner.x + glow / 2),
            style = Stroke(glow),
        )
        drawRoundRect(color, topLeft, ringSize, corner, style = Stroke(2.dp.toPx()))
    }
}
