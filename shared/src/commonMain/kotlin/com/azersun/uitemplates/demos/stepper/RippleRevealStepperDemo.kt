package com.azersun.uitemplates.demos.stepper

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.max

private val RippleColor = Color(0xFF2EC4D6)

@Composable
fun RippleRevealStepperDemo() {
    val scope = rememberCoroutineScope()
    // Plain state rather than Animatables so a tap resets the ripple in the same frame it swaps the digits.
    var wave by remember { mutableFloatStateOf(0f) }
    var fade by remember { mutableFloatStateOf(0f) }
    var rippleJob by remember { mutableStateOf<Job?>(null) }
    var from by remember { mutableIntStateOf(StartQuantity) }
    var to by remember { mutableIntStateOf(StartQuantity) }
    var origin by remember { mutableStateOf(Side.Plus) }

    fun DrawScope.waveRadius(center: Offset): Float =
        wave * hypot(max(center.x, size.width - center.x), size.height / 2)

    fun DrawScope.waveCircle(): Path {
        val center = buttonCenter(origin)
        return Path().apply { addOval(Rect(center, waveRadius(center))) }
    }

    StepperScene(
        number = 3,
        title = "Ripple Reveal",
        caption = "A ripple spreads from the button and reveals the new number as it passes.",
        onStep = { side, next ->
            from = to
            to = next
            origin = side
            wave = 0f
            fade = 1f
            rippleJob?.cancel()
            rippleJob = scope.launch {
                animate(0f, 1f, animationSpec = tween(560, easing = FastOutSlowInEasing)) { value, _ -> wave = value }
                from = next
                animate(1f, 0f, animationSpec = tween(260)) { value, _ -> fade = value }
            }
        },
        decoration = {
            val center = buttonCenter(origin)
            val radius = waveRadius(center)
            if (radius > 0f && fade > 0f) {
                val pill = Path().apply {
                    addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(size.height / 2)))
                }
                clipPath(pill) {
                    drawCircle(
                        Brush.radialGradient(
                            0f to RippleColor.copy(alpha = 0.1f),
                            1f to RippleColor.copy(alpha = 0.38f),
                            center = center,
                            radius = radius,
                        ),
                        radius,
                        center,
                        alpha = fade,
                    )
                    drawCircle(RippleColor, radius, center, alpha = 0.75f * fade, style = Stroke(2.dp.toPx()))
                }
            }
        },
    ) {
        // Old and new digits split the same space along the wavefront, so the edge itself swaps them.
        Box(Modifier.matchParentSize().clipToWave(ClipOp.Difference) { waveCircle() }, contentAlignment = Alignment.Center) {
            QuantityText(from)
        }
        Box(Modifier.matchParentSize().clipToWave(ClipOp.Intersect) { waveCircle() }, contentAlignment = Alignment.Center) {
            QuantityText(to)
        }
    }
}

private fun Modifier.clipToWave(op: ClipOp, circle: DrawScope.() -> Path): Modifier = drawWithContent {
    clipPath(circle(), op) { this@drawWithContent.drawContent() }
}
