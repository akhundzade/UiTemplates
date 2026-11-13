package com.azersun.uitemplates.demos.datepicker

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

private const val QuarterTurn = 90f

@Composable
fun TwistInDemo() {
    val state = rememberDatePickerState()
    DatePickerScene(
        state = state,
        number = 6,
        title = "Twist In",
        caption = "The new capsule twists in from a quarter spin while the old one twists away.",
    ) { _, selected -> TwistCapsule(selected) }
}

@Composable
private fun BoxScope.TwistCapsule(selected: Boolean) {
    val turn = remember { Animatable(if (selected) 1f else 0f) }
    // Entering turns from +90°, leaving turns on to −90°, so both read as one continuous twist.
    var entering by remember { mutableStateOf(true) }

    LaunchedEffect(selected) {
        if (selected) {
            if (turn.value == 1f) return@LaunchedEffect
            entering = true
            turn.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 260f))
        } else {
            entering = false
            turn.animateTo(0f, tween(240, easing = FastOutLinearInEasing))
        }
    }

    Capsule(
        Modifier.graphicsLayer {
            val t = turn.value
            val angle = (1f - t) * QuarterTurn * if (entering) 1f else -1f
            rotationY = angle
            rotationZ = angle
            alpha = if (entering) (t * 4f).coerceIn(0f, 1f) else t.coerceIn(0f, 1f)
            cameraDistance = 12f * density
        },
    )
}
