package com.azersun.uitemplates.demos.inputerror

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos

private const val FlashPeriodMillis = 200f
private const val FlashCount = 2
private const val EchoMillis = 500f
private val EchoGrowth = 10.dp

// Each echo leaves at a flash peak: 100 ms and 300 ms.
private val TotalMillis = (FlashPeriodMillis * (FlashCount - 0.5f) + EchoMillis).toInt()

@Composable
fun RedEchoDemo() {
    val form = remember { EmailFormState() }
    val clock = remember { Animatable(TotalMillis.toFloat()) }
    LaunchedEffect(form.errorTick) {
        if (form.errorTick == 0) return@LaunchedEffect
        clock.snapTo(0f)
        clock.animateTo(TotalMillis.toFloat(), tween(TotalMillis, easing = LinearEasing))
    }

    InputErrorStage(4, "Red Echo", "The border flashes red twice and sends out echo outlines.", form) {
        val error = form.status == FieldStatus.Error
        val t = clock.value
        val flash = if (error && t < FlashPeriodMillis * FlashCount) 0.5f - 0.5f * cos(2f * PI.toFloat() * t / FlashPeriodMillis) else 0f
        EmailField(
            form,
            modifier = Modifier.drawBehind {
                if (!error) return@drawBehind
                repeat(FlashCount) { k ->
                    val e = (clock.value - FlashPeriodMillis * (k + 0.5f)) / EchoMillis
                    if (e <= 0f || e >= 1f) return@repeat
                    val grow = EchoGrowth.toPx() * FastOutSlowInEasing.transform(e)
                    drawRoundRect(
                        InputColors.Red.copy(alpha = 0.75f * (1f - e)),
                        topLeft = Offset(-grow, -grow),
                        size = Size(size.width + grow * 2, size.height + grow * 2),
                        cornerRadius = CornerRadius(FieldCorner.toPx() + grow),
                        style = Stroke(2.dp.toPx() * (1f - e * 0.5f)),
                    )
                }
            },
            borderColor = if (error) lerp(InputColors.Red, InputColors.RedFlash, flash) else null,
        )
    }
}
