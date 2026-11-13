package com.azersun.uitemplates.demos.otp

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

@Composable
fun BoxBounceOtpDemo() {
    val state = remember { OtpState() }
    OtpScene(1, "Box Bounce", "Each digit drops into its box, which squashes and bounces back.", state) { metrics ->
        Row(horizontalArrangement = Arrangement.spacedBy(metrics.gap)) {
            repeat(CodeLength) { BounceBox(it, state, metrics) }
        }
    }
}

@Composable
private fun BounceBox(index: Int, state: OtpState, metrics: OtpMetrics) {
    val digit = state.code.getOrNull(index)
    val drop = remember { Animatable(0f) }
    val squash = remember { Animatable(0f) }
    LaunchedEffect(digit) {
        drop.snapTo(0f)
        squash.snapTo(0f)
        if (digit == null) return@LaunchedEffect
        drop.animateTo(1f, tween(190, easing = FastOutLinearInEasing))
        squash.animateTo(1f, tween(70, easing = LinearOutSlowInEasing))
        // Underdamped, so the box overshoots into a brief stretch before settling.
        squash.animateTo(0f, spring(dampingRatio = 0.32f, stiffness = 520f))
    }
    val success by animateFloatAsState(
        if (state.verified) 1f else 0f,
        tween(220, delayMillis = if (state.verified) index * 70 else 0),
        label = "success",
    )

    OtpBox(
        metrics = metrics,
        active = index == state.activeIndex,
        success = { success },
        modifier = Modifier.graphicsLayer {
            transformOrigin = TransformOrigin(0.5f, 1f)
            scaleX = 1f + 0.06f * squash.value
            scaleY = 1f - 0.12f * squash.value
        },
    ) {
        if (digit != null) {
            OtpDigit(
                digit,
                Modifier.graphicsLayer {
                    translationY = (1f - drop.value) * -30.dp.toPx()
                    alpha = (drop.value * 2f).coerceAtMost(1f)
                },
            )
        }
    }
}
