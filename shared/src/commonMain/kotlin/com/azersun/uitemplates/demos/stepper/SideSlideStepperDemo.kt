package com.azersun.uitemplates.demos.stepper

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private val SlideSpring = spring(dampingRatio = 0.8f, stiffness = 420f, visibilityThreshold = IntOffset.VisibilityThreshold)

@Composable
fun SideSlideStepperDemo() {
    val scope = rememberCoroutineScope()
    val nudge = remember { Animatable(0f) }

    StepperScene(
        number = 2,
        title = "Side Slide",
        caption = "The pill nudges; the new number slides in from the side you tapped.",
        onStep = { side, _ ->
            scope.launch {
                nudge.animateTo(6f * side.delta, tween(70, easing = FastOutSlowInEasing))
                nudge.animateTo(0f, spring(dampingRatio = 0.4f, stiffness = 500f))
            }
        },
        pillModifier = Modifier.graphicsLayer { translationX = nudge.value.dp.toPx() },
    ) { quantity ->
        AnimatedContent(
            targetState = quantity,
            modifier = Modifier
                .padding(horizontal = NumberWindowInset)
                .fillMaxSize()
                .clipToBounds(),
            transitionSpec = {
                // + brings the number in from the right, − from the left.
                val from = if (targetState > initialState) 1 else -1
                (slideInHorizontally(SlideSpring) { from * it / 2 } + fadeIn(tween(160)))
                    .togetherWith(slideOutHorizontally(SlideSpring) { -from * it / 2 } + fadeOut(tween(140)))
            },
            label = "quantity",
        ) { value ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { QuantityText(value) }
        }
    }
}
