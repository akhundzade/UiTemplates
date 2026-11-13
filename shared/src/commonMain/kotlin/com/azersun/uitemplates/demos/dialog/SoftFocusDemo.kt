package com.azersun.uitemplates.demos.dialog

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val DialogBlur = 20.dp
private val AppBlur = 8.dp

@Composable
fun SoftFocusDemo() {
    val state = rememberDialogState()
    val focus = remember { Animatable(0f) }
    var shown by remember { mutableStateOf(false) }

    LaunchedEffect(state.open) {
        if (state.open) {
            shown = true
            focus.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
        } else if (shown) {
            focus.animateTo(0f, tween(280, easing = FastOutSlowInEasing))
            shown = false
        }
    }

    DialogScene(
        state = state,
        number = 5,
        title = "Soft Focus",
        caption = "Comes into focus out of a blur while the app behind softly blurs.",
        shown = shown,
        dim = { focus.value * 0.75f },
        // Scale and alpha carry the motion on their own where blur is unsupported (Android < 12).
        dialogModifier = Modifier.graphicsLayer {
            val f = focus.value
            alpha = f
            val scale = 1.08f - 0.08f * f
            scaleX = scale
            scaleY = scale
            blur(DialogBlur * (1f - f))
        },
        appModifier = Modifier.graphicsLayer {
            val f = focus.value
            val scale = 1f - 0.03f * f
            scaleX = scale
            scaleY = scale
            blur(AppBlur * f)
        },
    )
}

private fun GraphicsLayerScope.blur(radius: Dp) {
    val px = radius.toPx()
    renderEffect = if (px > 0.5f) BlurEffect(px, px, TileMode.Decal) else null
}
