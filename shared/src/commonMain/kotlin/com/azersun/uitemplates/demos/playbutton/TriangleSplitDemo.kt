package com.azersun.uitemplates.demos.playbutton

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Path
import kotlin.math.PI
import kotlin.math.sin

/** How far the halves drift apart mid-morph, as a fraction of the button. */
private const val SplitGap = 0.05f

@Composable
fun TriangleSplitDemo() {
    val state = rememberPlayerState()
    val morph = remember { Animatable(0f) }

    LaunchedEffect(state.playing) {
        morph.animateTo(if (state.playing) 1f else 0f, spring(dampingRatio = 0.5f, stiffness = 340f))
    }

    PlayerScene(
        state = state,
        number = 1,
        title = "Triangle Split",
        caption = "The play triangle splits into two halves that spring into the pause bars, and back.",
    ) {
        PlayGlow()
        PlayDisc()
        Spacer(
            Modifier
                .matchParentSize()
                .drawWithCache {
                    val path = Path()
                    onDrawBehind {
                        val m = morph.value
                        val split = SplitGap * sin(PI.toFloat() * m.coerceIn(0f, 1f))
                        drawPlayPause(path, m, split = split)
                    }
                },
        )
    }
}
