package com.azersun.uitemplates.demos.follow

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

@Composable
fun CoinFlipFollowDemo() {
    val state = remember { FollowState() }
    // Always turns forward by half a turn, like a real coin, instead of flipping back and forth.
    val angle = remember { Animatable(0f) }
    LaunchedEffect(state.toggles) {
        angle.animateTo(state.toggles * 180f, spring(dampingRatio = 0.6f, stiffness = 160f))
    }
    val showingFollowing by remember { derivedStateOf { faceAt(angle.value) == 1 } }

    FollowScene(state, 1, "Coin Flip", "The button flips over like a coin to show its other side.") {
        FollowFace(
            showingFollowing,
            Modifier.graphicsLayer {
                val a = angle.value
                rotationX = tiltAt(a)
                cameraDistance = 12f * density
                val lift = 1f + 0.06f * abs(sin(a * PI.toFloat() / 180f))
                scaleX = lift
                scaleY = lift
            },
        )
    }
}
