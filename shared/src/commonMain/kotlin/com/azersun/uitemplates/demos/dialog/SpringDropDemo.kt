package com.azersun.uitemplates.demos.dialog

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
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
import kotlin.math.abs

/** Off-stage distance in dialog heights; the stage card clips the dialog beyond it. */
private const val DropDistance = 2.4f
private const val FallTilt = 9f

/** Dips slightly before rising, so Cancel reads as the dialog being lifted away. */
private val LiftEasing = CubicBezierEasing(0.4f, -0.35f, 0.75f, 0.3f)

/** Accelerates like a falling object. */
private val FallEasing = CubicBezierEasing(0.32f, 0f, 0.67f, 0f)

@Composable
fun SpringDropDemo() {
    val state = rememberDialogState()
    val drop = remember { Animatable(-DropDistance) }
    val tilt = remember { Animatable(0f) }
    var shown by remember { mutableStateOf(false) }

    LaunchedEffect(state.open) {
        if (state.open) {
            if (!shown) {
                drop.snapTo(-DropDistance)
                tilt.snapTo(0f)
                shown = true
            }
            launch { tilt.animateTo(0f, tween(200)) }
            drop.animateTo(0f, spring(dampingRatio = 0.58f, stiffness = 240f))
        } else if (shown) {
            if (state.lastAction == DialogAction.Delete) {
                launch { tilt.animateTo(FallTilt, tween(380, easing = FallEasing)) }
                drop.animateTo(DropDistance, tween(420, easing = FallEasing))
            } else {
                drop.animateTo(-DropDistance, tween(360, easing = LiftEasing))
            }
            shown = false
        }
    }

    DialogScene(
        state = state,
        number = 2,
        title = "Spring Drop",
        caption = "Drops in from the top on a spring; Cancel lifts it away, Delete drops it.",
        shown = shown,
        dim = { 1f - abs(drop.value) / DropDistance },
        dialogModifier = Modifier.graphicsLayer {
            translationY = drop.value * size.height
            rotationZ = tilt.value
        },
    )
}
