package com.azersun.uitemplates.common

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue

/** An integer that rolls to [target] whenever it changes, like a dashboard counter. */
@Composable
fun countUp(target: Int, durationMillis: Int = 1000, delayMillis: Int = 0): Int {
    val value by animateIntAsState(
        targetValue = target,
        animationSpec = tween(durationMillis, delayMillis, FastOutSlowInEasing),
        label = "countUp",
    )
    return value
}
