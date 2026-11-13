package com.azersun.uitemplates.demos.playbutton

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val RingCount = 3
private const val RingStaggerMillis = 120L
private const val RingMillis = 900
private val RingReach = 40.dp

@Composable
fun EchoRingsDemo() {
    val state = rememberPlayerState()
    val rings = remember { mutableStateListOf<Animatable<Float, AnimationVector1D>>() }
    val bump = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        snapshotFlow { state.toggles }.collect { taps ->
            if (taps == 0) return@collect
            launch {
                bump.animateTo(0.9f, tween(80))
                bump.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 600f))
            }
            // Rings live in their own coroutines so a quick second tap adds rings instead of cutting these off.
            repeat(RingCount) { i ->
                launch {
                    delay(i * RingStaggerMillis)
                    val ring = Animatable(0f)
                    rings += ring
                    ring.animateTo(1f, tween(RingMillis, easing = LinearOutSlowInEasing))
                    rings -= ring
                }
            }
        }
    }

    PlayerScene(
        state = state,
        number = 3,
        title = "Echo Rings",
        caption = "Every tap sends three rings echoing out from the button, 120 ms apart, fading as they grow.",
    ) {
        Spacer(
            Modifier
                .matchParentSize()
                .drawBehind {
                    val base = size.minDimension / 2
                    val reach = RingReach.toPx()
                    rings.forEach { ring ->
                        val p = ring.value
                        drawCircle(
                            lerp(PlayBlue, PlayViolet, p),
                            base + reach * p,
                            alpha = (1f - p) * 0.85f,
                            style = Stroke((2.5f - 1.8f * p).dp.toPx()),
                        )
                    }
                },
        )
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer {
                    scaleX = bump.value
                    scaleY = bump.value
                },
        ) {
            PlayButtonFace(state.playing)
        }
    }
}
