package com.azersun.uitemplates.demos.stepper

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch

@Composable
fun SoftPressStepperDemo() {
    val scope = rememberCoroutineScope()
    val press = remember { Side.entries.associateWith { Animatable(1f) } }

    StepperScene(
        number = 1,
        title = "Soft Press",
        caption = "The tapped button squishes softly; the new number pops up from small.",
        onStep = { side, _ ->
            scope.launch {
                val scale = press.getValue(side)
                scale.animateTo(0.82f, tween(90, easing = FastOutSlowInEasing))
                scale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 600f))
            }
        },
        buttonModifier = { side ->
            Modifier.graphicsLayer {
                val scale = press.getValue(side).value
                scaleX = scale
                scaleY = scale
            }
        },
    ) { quantity ->
        AnimatedContent(
            targetState = quantity,
            contentAlignment = Alignment.Center,
            transitionSpec = {
                (scaleIn(spring(dampingRatio = 0.45f, stiffness = 420f), initialScale = 0.4f) + fadeIn(tween(120)))
                    .togetherWith(fadeOut(tween(90)))
                    .using(SizeTransform(clip = false))
            },
            label = "quantity",
        ) { QuantityText(it) }
    }
}
