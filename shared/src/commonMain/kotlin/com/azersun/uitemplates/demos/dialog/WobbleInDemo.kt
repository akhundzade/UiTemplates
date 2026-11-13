package com.azersun.uitemplates.demos.dialog

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch

private const val StartScale = 0.3f

/** Starting velocity of the wobble; with the spring below it peaks at roughly ±13% stretch. */
private const val WobbleKick = 2.2f
private const val FlatWiden = 0.3f

@Composable
fun WobbleInDemo() {
    val state = rememberDialogState()
    val grow = remember { Animatable(StartScale) }
    val wobble = remember { Animatable(0f) }
    val flat = remember { Animatable(0f) }
    val fade = remember { Animatable(0f) }
    var shown by remember { mutableStateOf(false) }

    LaunchedEffect(state.open) {
        if (state.open) {
            if (!shown) {
                grow.snapTo(StartScale)
                flat.snapTo(0f)
                shown = true
            }
            launch { fade.animateTo(1f, tween(120)) }
            launch { flat.animateTo(0f, tween(150)) }
            // Released at rest with a kick, the spring rings: X stretches while Y squeezes, then the reverse.
            launch { wobble.animateTo(0f, spring(dampingRatio = 0.22f, stiffness = 280f), initialVelocity = WobbleKick) }
            grow.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 320f))
        } else if (shown) {
            launch { wobble.animateTo(0f, tween(120)) }
            launch { fade.animateTo(0f, tween(100, delayMillis = 160)) }
            flat.animateTo(1f, tween(260, easing = FastOutLinearInEasing))
            shown = false
        }
    }

    DialogScene(
        state = state,
        number = 6,
        title = "Wobble In",
        caption = "Grows in with a jelly wobble and squashes flat when it closes.",
        shown = shown,
        dim = { fade.value },
        dialogModifier = Modifier.graphicsLayer {
            val g = grow.value
            val w = wobble.value
            val f = flat.value
            scaleX = g * (1f + w) * (1f + FlatWiden * f)
            scaleY = (g * (1f - w) * (1f - f)).coerceAtLeast(0f)
            alpha = fade.value
        },
    )
}
