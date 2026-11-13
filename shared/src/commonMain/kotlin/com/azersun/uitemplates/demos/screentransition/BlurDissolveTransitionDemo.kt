package com.azersun.uitemplates.demos.screentransition

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

private val MaxBlur = 18.dp

@Composable
fun BlurDissolveTransitionDemo() {
    // Linear overall; the smoothsteps below ease each screen and let the two overlap mid-way.
    val nav = rememberNavigator(tween(850, easing = LinearEasing))
    TransitionScene(
        number = 5,
        title = "Blur Dissolve",
        caption = "The old screen blurs away as it fades while the new one sharpens into focus.",
        nav = nav,
    ) { explore, detail, progress ->
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val gone = smoothstep(0f, 0.7f, progress())
                    alpha = 1f - gone
                    val s = 1f + 0.05f * gone
                    scaleX = s
                    scaleY = s
                    renderEffect = blur(MaxBlur.toPx() * gone)
                },
        ) {
            explore()
        }
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val focus = smoothstep(0.3f, 1f, progress())
                    alpha = focus
                    val s = 0.95f + 0.05f * focus
                    scaleX = s
                    scaleY = s
                    renderEffect = blur(MaxBlur.toPx() * (1f - focus))
                },
        ) {
            detail()
        }
    }
}

// Blur only renders on Android 12+ and iOS; below that the fade and scale carry the effect alone.
private fun blur(radius: Float): RenderEffect? = if (radius > 0.5f) BlurEffect(radius, radius, TileMode.Decal) else null
