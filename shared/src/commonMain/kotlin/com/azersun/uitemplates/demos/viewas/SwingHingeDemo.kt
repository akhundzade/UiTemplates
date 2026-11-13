package com.azersun.uitemplates.demos.viewas

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private const val RaisedAngle = -70f
private val PinBrush = Brush.radialGradient(listOf(Color(0xFFF4F1FF), Color(0xFF8E86C9)))

@Composable
fun SwingHingeDemo() {
    val state = rememberViewAsState()
    val swing = remember { Animatable(RaisedAngle) }
    val fade = remember { Animatable(0f) }
    var shown by remember { mutableStateOf(false) }

    LaunchedEffect(state.open) {
        if (state.open) {
            shown = true
            launch { fade.animateTo(1f, tween(120)) }
            // Low damping gives the 2–3 decaying sways of a sign dropping onto its pin.
            swing.animateTo(0f, spring(dampingRatio = 0.3f, stiffness = 120f))
        } else if (shown) {
            launch { fade.animateTo(0f, tween(260, delayMillis = 40)) }
            swing.animateTo(RaisedAngle, tween(300, easing = FastOutSlowInEasing))
            shown = false
        }
    }

    ViewAsScene(state, 1, "Swing Hinge", "The menu swings down from a hinge pin and settles with a sway.") {
        if (shown) {
            ViewAsMenu(
                state,
                Modifier.graphicsLayer {
                    transformOrigin = TransformOrigin(0f, 0f)
                    rotationZ = swing.value
                    alpha = fade.value
                },
            )
            Box(
                Modifier
                    .offset((-4.5).dp, (-4.5).dp)
                    .size(9.dp)
                    .graphicsLayer { alpha = fade.value }
                    .background(PinBrush, CircleShape)
                    .border(1.dp, Color(0xFF4A3F8F), CircleShape),
            )
        }
    }
}
