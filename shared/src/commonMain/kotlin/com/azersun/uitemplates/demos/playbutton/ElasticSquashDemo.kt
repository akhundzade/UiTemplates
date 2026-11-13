package com.azersun.uitemplates.demos.playbutton

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

private const val Squash = 0.18f

@Composable
fun ElasticSquashDemo() {
    val state = rememberPlayerState()
    val wobble = remember { Animatable(0f) }

    // Squashed at 1; the underdamped release swings through 0 into a stretch, alternating until it settles.
    LaunchedEffect(state.toggles) {
        if (state.toggles == 0) return@LaunchedEffect
        wobble.animateTo(1f, tween(90, easing = FastOutLinearInEasing))
        wobble.animateTo(0f, spring(dampingRatio = 0.22f, stiffness = 520f))
    }

    PlayerScene(
        state = state,
        number = 6,
        title = "Elastic Squash",
        caption = "On tap the button squashes and wobbles like jelly before settling, the icon cross-fading.",
    ) {
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer {
                    val w = wobble.value
                    scaleX = 1f + Squash * w
                    scaleY = 1f - Squash * w
                },
        ) {
            PlayButtonFace(state.playing)
        }
    }
}
