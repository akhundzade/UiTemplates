package com.azersun.uitemplates.demos.otp

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.floor

/** Slots the reel scrolls through before landing: four lead-in digits and the real one. */
private const val ReelLanding = 4f

@Composable
fun DigitSpinOtpDemo() {
    val state = remember { OtpState() }
    OtpScene(3, "Digit Spin", "Digits spin into place like a slot machine, then spin once more.", state) { metrics ->
        Row(horizontalArrangement = Arrangement.spacedBy(metrics.gap)) {
            repeat(CodeLength) { SpinBox(it, state, metrics) }
        }
    }
}

@Composable
private fun SpinBox(index: Int, state: OtpState, metrics: OtpMetrics) {
    val digit = state.code.getOrNull(index)
    // Reel position in slots; slot k shows (digit - 4 + k) mod 10, so whole slots 4, 14, … show the digit.
    val reel = remember { Animatable(0f) }
    var landed by remember { mutableStateOf(false) }

    LaunchedEffect(digit) {
        reel.snapTo(0f)
        if (digit != null) {
            // Starts fast and decelerates; the speed matches the spring so it lands with a small overshoot.
            reel.animateTo(ReelLanding, spring(dampingRatio = 0.68f, stiffness = 140f), initialVelocity = 34f)
        }
    }
    LaunchedEffect(state.verified) {
        if (!state.verified) {
            landed = false
            return@LaunchedEffect
        }
        delay(index * 70L)
        reel.animateTo(reel.targetValue + 10f, tween(760, easing = FastOutSlowInEasing))
        landed = true
    }
    val success by animateFloatAsState(if (landed) 1f else 0f, tween(240), label = "success")

    OtpBox(metrics, active = index == state.activeIndex, success = { success }) {
        if (digit != null) {
            val first by remember { derivedStateOf { floor(reel.value).toInt() } }
            // Inset so the scrolling digits pass under the border rather than over it.
            Box(
                Modifier.matchParentSize().padding(2.dp).clip(RoundedCornerShape(BoxCorner - 2.dp)),
                contentAlignment = Alignment.Center,
            ) {
                for (slot in first..first + 1) {
                    key(slot) {
                        OtpDigit(
                            '0' + (digit - '0' - ReelLanding.toInt() + slot).mod(10),
                            Modifier.graphicsLayer { translationY = (reel.value - slot) * metrics.boxHeight.toPx() },
                        )
                    }
                }
            }
        }
    }
}
