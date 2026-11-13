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

private const val StartScale = 0.72f

@Composable
fun ScalePopDemo() {
    val state = rememberDialogState()
    val pop = remember { Animatable(0f) }
    var shown by remember { mutableStateOf(false) }

    LaunchedEffect(state.open) {
        if (state.open) {
            shown = true
            pop.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 420f))
        } else if (shown) {
            pop.animateTo(0f, tween(180, easing = FastOutLinearInEasing))
            shown = false
        }
    }

    DialogScene(
        state = state,
        number = 1,
        title = "Scale Pop",
        caption = "The dialog pops up from 72% with a soft overshoot and shrinks away.",
        shown = shown,
        dim = { pop.value },
        dialogModifier = Modifier.graphicsLayer {
            val p = pop.value
            val scale = StartScale + (1f - StartScale) * p
            scaleX = scale
            scaleY = scale
            alpha = (p * 1.6f).coerceIn(0f, 1f)
        },
    )
}
