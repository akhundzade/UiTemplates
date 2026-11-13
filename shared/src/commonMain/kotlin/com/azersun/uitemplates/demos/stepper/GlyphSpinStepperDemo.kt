package com.azersun.uitemplates.demos.stepper

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun GlyphSpinStepperDemo() {
    val scope = rememberCoroutineScope()
    val spin = remember { Side.entries.associateWith { Animatable(0f) } }
    val flip = remember { Animatable(0f) }
    var shown by remember { mutableIntStateOf(StartQuantity) }

    StepperScene(
        number = 5,
        title = "Glyph Spin",
        caption = "The + or − spins half a turn; the number flips over edge-on.",
        onStep = { side, next ->
            scope.launch {
                val glyph = spin.getValue(side)
                // Building on the running target keeps rapid taps adding half turns instead of restarting.
                glyph.animateTo(glyph.targetValue + 180f * side.delta, spring(dampingRatio = 0.55f, stiffness = 260f))
            }
            scope.launch {
                val direction = side.delta.toFloat()
                flip.animateTo(90f * direction, tween(140, easing = FastOutLinearInEasing))
                shown = next
                flip.snapTo(-90f * direction)
                flip.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = 420f))
            }
        },
        glyphRotation = { side -> spin.getValue(side).value },
    ) {
        QuantityText(
            shown,
            Modifier.graphicsLayer {
                rotationX = flip.value
                cameraDistance = 12f * density
                alpha = 1f - 0.4f * (abs(flip.value) / 90f).coerceAtMost(1f)
            },
        )
    }
}
