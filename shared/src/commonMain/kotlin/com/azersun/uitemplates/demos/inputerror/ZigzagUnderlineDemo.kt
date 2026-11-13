package com.azersun.uitemplates.demos.inputerror

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow

private const val BuzzMillis = 600

// 25 Hz across the 600 ms decay.
private const val BuzzCycles = 15f
private val ZigzagAmplitude = 6.dp
private val ZigzagWavelength = 10.dp

@Composable
fun ZigzagUnderlineDemo() {
    val form = remember { EmailFormState() }
    val buzz = remember { Animatable(1f) }
    LaunchedEffect(form.errorTick) {
        if (form.errorTick == 0) return@LaunchedEffect
        buzz.snapTo(0f)
        buzz.animateTo(1f, tween(BuzzMillis, easing = LinearEasing))
    }
    val lineColor by animateColorAsState(
        when (form.status) {
            FieldStatus.Error -> InputColors.Red
            FieldStatus.Valid -> InputColors.Teal
            FieldStatus.Neutral -> Color.White.copy(alpha = 0.22f)
        },
        tween(150),
        label = "underline",
    )

    InputErrorStage(1, "Zigzag Underline", "The underline snaps into a red zigzag that buzzes flat.", form) {
        EmailField(
            form,
            Modifier.drawWithContent {
                drawContent()
                val p = buzz.value
                val amplitude = if (form.status == FieldStatus.Error) {
                    ZigzagAmplitude.toPx() * (1f - p).pow(1.5f) * cos(2f * PI.toFloat() * BuzzCycles * p)
                } else {
                    0f
                }
                drawUnderline(lineColor, amplitude)
            },
        )
    }
}

/** A triangle wave along the field's bottom edge; a signed [amplitude] flips its phase, which reads as a buzz. */
private fun DrawScope.drawUnderline(color: Color, amplitude: Float) {
    val y = size.height - 0.75.dp.toPx()
    val start = FieldCorner.toPx()
    val end = size.width - FieldCorner.toPx()
    val half = ZigzagWavelength.toPx() / 2
    val path = Path().apply {
        moveTo(start, y)
        var x = start + half / 2
        var up = true
        while (x < end) {
            lineTo(x, if (up) y - amplitude else y + amplitude)
            up = !up
            x += half
        }
        lineTo(end, y)
    }
    drawPath(path, color, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
}
