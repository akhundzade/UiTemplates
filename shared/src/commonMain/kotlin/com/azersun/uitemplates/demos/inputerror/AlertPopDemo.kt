package com.azersun.uitemplates.demos.inputerror

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val SparkCount = 8
private val SparkTravel = 14.dp

@Composable
fun AlertPopDemo() {
    val form = remember { EmailFormState() }
    InputErrorStage(2, "Alert Pop", "A red ! pops out on a spring with a burst of sparks.", form) {
        EmailField(form, trailing = { AlertPopBadge(form) })
    }
}

@Composable
private fun AlertPopBadge(form: EmailFormState) {
    val scale = remember { Animatable(0f) }
    val burst = remember { Animatable(1f) }
    var shown by remember { mutableStateOf(FieldStatus.Neutral) }
    LaunchedEffect(form.status, form.errorTick) {
        when (form.status) {
            FieldStatus.Error -> {
                shown = FieldStatus.Error
                scale.snapTo(0f)
                launch {
                    // Sparks leave just as the badge passes full size on its way to the overshoot.
                    delay(50)
                    burst.snapTo(0f)
                    burst.animateTo(1f, tween(480, easing = FastOutSlowInEasing))
                }
                scale.animateTo(1f, spring(dampingRatio = 0.32f, stiffness = 520f))
            }
            FieldStatus.Valid -> {
                shown = FieldStatus.Valid
                scale.snapTo(0f)
                scale.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 600f))
            }
            FieldStatus.Neutral -> scale.animateTo(0f, tween(140))
        }
    }

    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            val p = burst.value
            if (p >= 1f || shown != FieldStatus.Error) return@Canvas
            val inner = size.minDimension / 2 + 2.dp.toPx() + SparkTravel.toPx() * p
            val length = 7.dp.toPx() * (1f - p) + 1.dp.toPx()
            repeat(SparkCount) { i ->
                val angle = (i * 2 * PI / SparkCount + PI / SparkCount).toFloat()
                val dir = Offset(cos(angle), sin(angle))
                drawLine(
                    InputColors.Red.copy(alpha = 1f - p),
                    center + dir * inner,
                    center + dir * (inner + length),
                    2.dp.toPx(),
                    StrokeCap.Round,
                )
            }
        }
        Canvas(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                },
        ) { drawStatusBadge(shown) }
    }
}
