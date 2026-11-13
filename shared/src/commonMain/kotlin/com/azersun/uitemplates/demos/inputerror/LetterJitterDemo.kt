package com.azersun.uitemplates.demos.inputerror

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

private const val StaggerMillis = 40f
private const val LetterMillis = 460f
private const val HopMillis = 240f
private const val RedInMillis = 100f
private val HopHeight = 6.dp
private val ShiverX = 1.6.dp
private const val ShiverDegrees = 10f

@Composable
fun LetterJitterDemo() {
    val form = remember { EmailFormState() }
    val clock = remember { Animatable(0f) }
    var running by remember { mutableStateOf(false) }
    var snapshot by remember { mutableStateOf("") }
    LaunchedEffect(form.errorTick) {
        if (form.errorTick == 0) return@LaunchedEffect
        snapshot = form.text
        running = true
        val total = (snapshot.length - 1).coerceAtLeast(0) * StaggerMillis + LetterMillis
        clock.snapTo(0f)
        clock.animateTo(total, tween(total.toInt(), easing = LinearEasing))
        running = false
    }
    val error = form.status == FieldStatus.Error
    val jittering = running && error && form.text == snapshot

    InputErrorStage(5, "Letter Jitter", "Each letter hops and shivers red, one after another.", form) {
        EmailField(
            form,
            textColor = when {
                jittering -> Color.Transparent
                error -> InputColors.Red
                else -> InputColors.Text
            },
            textOverlay = { layout -> if (jittering) JitterLetters(snapshot, layout, clock) },
        )
    }
}

/** Draws each character over its slot in the real text layout, animated on its own staggered clock. */
@Composable
private fun JitterLetters(text: String, layout: TextLayoutResult, clock: Animatable<Float, AnimationVector1D>) {
    val count = minOf(text.length, layout.layoutInput.text.length)
    for (i in 0 until count) {
        val box = layout.getBoundingBox(i)
        fun local() = clock.value - i * StaggerMillis
        BasicText(
            text[i].toString(),
            style = ValueStyle,
            color = { lerp(InputColors.Text, InputColors.Red, (local() / RedInMillis).coerceIn(0f, 1f)) },
            modifier = Modifier
                .offset { IntOffset(box.left.roundToInt(), box.top.roundToInt()) }
                .graphicsLayer {
                    val t = local()
                    if (t <= 0f) return@graphicsLayer
                    val hop = (t / HopMillis).coerceAtMost(1f)
                    val envelope = (1f - t / LetterMillis).coerceAtLeast(0f).let { it * it }
                    translationY = -HopHeight.toPx() * sin(PI.toFloat() * hop)
                    translationX = ShiverX.toPx() * envelope * sin(2f * PI.toFloat() * t / 38f)
                    rotationZ = ShiverDegrees * envelope * sin(2f * PI.toFloat() * t / 52f + i)
                },
        )
    }
}
