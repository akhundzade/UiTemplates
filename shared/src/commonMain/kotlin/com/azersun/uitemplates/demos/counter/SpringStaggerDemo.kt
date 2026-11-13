package com.azersun.uitemplates.demos.counter

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay

private const val StaggerMillis = 60L

@Composable
fun SpringStaggerDemo() {
    CounterScene(
        number = 4,
        title = "Spring Stagger",
        caption = "Digits swap one by one from the left; each new one drops in from above with a springy bounce.",
    ) { value ->
        val digits = digitsOf(value)
        DigitRow { index -> DropDigit(digits[index], index) }
    }
}

@Composable
private fun DropDigit(digit: Int, index: Int) {
    var old by remember { mutableIntStateOf(digit) }
    var current by remember { mutableIntStateOf(digit) }
    val drop = remember { Animatable(1f) }

    LaunchedEffect(digit) {
        if (digit == current) return@LaunchedEffect
        delay(index * StaggerMillis)
        old = current
        current = digit
        drop.snapTo(0f)
        drop.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = 420f))
    }

    DigitCell {
        Text(
            old.toString(),
            style = NumberStyle,
            modifier = Modifier.graphicsLayer {
                val p = drop.value.coerceIn(0f, 1f)
                translationY = p * size.height * 0.5f
                alpha = (1f - p * 2.5f).coerceIn(0f, 1f)
            },
        )
        Text(
            current.toString(),
            style = NumberStyle,
            modifier = Modifier.graphicsLayer {
                val p = drop.value
                translationY = -(1f - p) * size.height * 0.8f
                alpha = (p * 2f).coerceIn(0f, 1f)
                // The spring's overshoot past 1 is the landing: squash on the baseline.
                val squash = (p - 1f).coerceAtLeast(0f)
                transformOrigin = TransformOrigin(0.5f, 1f)
                scaleY = 1f - squash * 0.6f
                scaleX = 1f + squash * 0.35f
            },
        )
    }
}
