package com.azersun.uitemplates.demos.stepper

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/** How far the tapped edge reaches out: 8 % of the pill's width. */
private val StretchReach = PillWidth * 0.08f

@Composable
fun StretchPillStepperDemo() {
    val scope = rememberCoroutineScope()
    // Each edge stretches on its own, so tapping the other side mid-wobble never makes the pill jump.
    val reach = remember { Side.entries.associateWith { Animatable(0f) } }
    val wobble = remember { Animatable(0f) }

    StepperScene(
        number = 4,
        title = "Stretch Pill",
        caption = "The pill stretches toward the tap and springs back; the number wobbles.",
        onStep = { side, _ ->
            scope.launch {
                val edge = reach.getValue(side)
                edge.animateTo(StretchReach.value, tween(110, easing = FastOutSlowInEasing))
                edge.animateTo(0f, spring(dampingRatio = 0.32f, stiffness = 380f))
            }
            scope.launch {
                // A kick of velocity into an underdamped spring peaks near ±8° and rings out.
                wobble.animateTo(0f, spring(dampingRatio = 0.3f, stiffness = 300f), initialVelocity = 210f * side.delta)
            }
        },
        trackModifier = Modifier.graphicsLayer {
            val left = reach.getValue(Side.Minus).value.dp.toPx()
            val right = reach.getValue(Side.Plus).value.dp.toPx()
            scaleX = (size.width + left + right) / size.width
            translationX = (right - left) / 2
        },
        buttonModifier = { side ->
            Modifier.graphicsLayer { translationX = side.delta * reach.getValue(side).value.dp.toPx() }
        },
    ) { quantity ->
        AnimatedContent(
            targetState = quantity,
            modifier = Modifier.graphicsLayer {
                val left = reach.getValue(Side.Minus).value.dp.toPx()
                val right = reach.getValue(Side.Plus).value.dp.toPx()
                translationX = (right - left) / 2
                rotationZ = wobble.value
            },
            contentAlignment = Alignment.Center,
            transitionSpec = {
                fadeIn(tween(140)).togetherWith(fadeOut(tween(100))).using(SizeTransform(clip = false))
            },
            label = "quantity",
        ) { QuantityText(it) }
    }
}
