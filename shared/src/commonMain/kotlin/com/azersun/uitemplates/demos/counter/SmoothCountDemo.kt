package com.azersun.uitemplates.demos.counter

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val Mint = Color(0xFF5EEAD4)
private val Pink = Color(0xFFF472B6)

@Composable
fun SmoothCountDemo() {
    CounterScene(
        number = 6,
        title = "Smooth Count",
        caption = "The number counts to the new total, glowing mint with ▲ when it rises or pink with ▼ when it falls.",
    ) { value ->
        SmoothCountNumber(value)
    }
}

@Composable
private fun SmoothCountNumber(value: Int) {
    val count = remember { Animatable(value.toFloat()) }
    val tint = remember { Animatable(0f) }
    var rising by remember { mutableStateOf(true) }

    LaunchedEffect(value) {
        val target = value.toFloat()
        if (count.targetValue == target) return@LaunchedEffect
        rising = target > count.value
        launch { tint.animateTo(1f, tween(220)) }
        count.animateTo(target, tween(1000, easing = FastOutSlowInEasing))
        delay(350)
        tint.animateTo(0f, tween(650))
    }

    val accent = if (rising) Mint else Pink
    val t = tint.value
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(
            Modifier
                // The arrow's slot opens with the tint, pushing the number right rather than overlapping it.
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    layout((placeable.width * tint.value.coerceIn(0f, 1f)).roundToInt(), placeable.height) {
                        placeable.place(0, 0)
                    }
                }
                .padding(end = 8.dp)
                .size(20.dp)
                .graphicsLayer {
                    val s = tint.value.coerceIn(0f, 1f)
                    scaleX = s
                    scaleY = s
                    alpha = s
                    transformOrigin = TransformOrigin(0f, 0.5f)
                },
        ) {
            val w = size.width
            val h = size.height * 0.8f
            val top = (size.height - h) / 2
            val arrow = Path().apply {
                if (rising) {
                    moveTo(w / 2, top)
                    lineTo(w, top + h)
                    lineTo(0f, top + h)
                } else {
                    moveTo(0f, top)
                    lineTo(w, top)
                    lineTo(w / 2, top + h)
                }
                close()
            }
            drawPath(arrow, accent)
        }
        Text(
            formatSteps(count.value.roundToInt()),
            style = NumberStyle.copy(
                color = lerp(Color.White, accent, t),
                shadow = Shadow(accent.copy(alpha = 0.75f * t), Offset.Zero, blurRadius = 28f * t),
            ),
        )
    }
}
