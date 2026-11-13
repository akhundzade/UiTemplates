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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch

private const val FoldedAngle = -90f

@Composable
fun HingeSwingDemo() {
    val state = rememberDialogState()
    val swing = remember { Animatable(FoldedAngle) }
    val fade = remember { Animatable(0f) }
    var shown by remember { mutableStateOf(false) }

    LaunchedEffect(state.open) {
        if (state.open) {
            if (!shown) {
                swing.snapTo(FoldedAngle)
                shown = true
            }
            launch { fade.animateTo(1f, tween(140)) }
            // Low damping lets it swing past flat and sway a few times on the hinge.
            swing.animateTo(0f, spring(dampingRatio = 0.32f, stiffness = 130f))
        } else if (shown) {
            launch { fade.animateTo(0f, tween(200, delayMillis = 120)) }
            swing.animateTo(FoldedAngle, tween(300, easing = FastOutLinearInEasing))
            shown = false
        }
    }

    DialogScene(
        state = state,
        number = 3,
        title = "Hinge Swing",
        caption = "Swings down in 3D from a hinge on its top edge, then swings back up.",
        shown = shown,
        dim = { fade.value },
        dialogModifier = Modifier.graphicsLayer {
            transformOrigin = TransformOrigin(0.5f, 0f)
            rotationX = swing.value
            cameraDistance = 14f * density
            alpha = fade.value
        },
    )
}
