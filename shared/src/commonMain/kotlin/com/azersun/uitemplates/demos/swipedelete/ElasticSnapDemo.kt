package com.azersun.uitemplates.demos.swipedelete

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer

/** How much of the finger's travel turns into stretch. */
private const val Give = 0.85f

/** How much thinner the row gets per unit of stretch, like rubber under tension. */
private const val Thinning = 0.16f

@Composable
fun ElasticSnapDemo() {
    SwipeDeleteScene(
        number = 5,
        title = "Elastic Snap",
        caption = "It stretches like rubber, then snaps off to the left.",
        spec = SwipeSpec(
            exitMillis = 460,
            collapseAt = 0.42f,
            // Under-damped so a short pull wobbles back like a released band.
            settle = spring(dampingRatio = 0.3f, stiffness = 420f),
        ),
    ) { message, state -> ElasticRow(message, state) }
}

@Composable
private fun ElasticRow(message: Message, state: SwipeRowState) {
    MessageRow(
        message,
        Modifier.graphicsLayer {
            val w = size.width
            if (!state.deleting) {
                val stretch = state.drag.value * Give / w
                transformOrigin = TransformOrigin(1f, 0.5f)
                scaleX = (1f + stretch).coerceAtLeast(0.6f)
                scaleY = thinned(stretch)
            } else {
                val e = state.exit.value
                val stretch = state.releasedDrag * Give / w
                val recoil = FastOutSlowInEasing.transform(phase(e, 0f, 0.3f))
                val relax = FastOutSlowInEasing.transform(phase(e, 0.3f, 0.6f))
                val fly = FastOutLinearInEasing.transform(phase(e, 0.06f, 0.85f))
                // The pivot jumps to the left edge (same geometry at e = 0) so the right end
                // whips over towards it, then the whole row flies off.
                transformOrigin = TransformOrigin(0f, 0.5f)
                scaleX = lerp(lerp(1f + stretch, 0.86f, recoil), 1f, relax)
                scaleY = lerp(lerp(thinned(stretch), 1.06f, recoil), 1f, relax)
                translationX = -w * stretch - w * 1.6f * fly
                alpha = 1f - phase(e, 0.55f, 0.85f)
            }
        },
    )
}

private fun thinned(stretch: Float): Float = (1f - Thinning * stretch).coerceIn(0.8f, 1.08f)
