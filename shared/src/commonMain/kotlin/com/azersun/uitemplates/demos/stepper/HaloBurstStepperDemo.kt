package com.azersun.uitemplates.demos.stepper

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.azersun.uitemplates.demos.menus.MenuColors
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val TickCount = 8

@Composable
fun HaloBurstStepperDemo() {
    val scope = rememberCoroutineScope()
    // 1 means "finished": nothing is drawn until a tap restarts that side's burst.
    val halo = remember { Side.entries.associateWith { Animatable(1f) } }

    StepperScene(
        number = 6,
        title = "Halo Burst",
        caption = "A glow ring bursts from the button; the new number zooms down in.",
        onStep = { side, _ ->
            scope.launch {
                val burst = halo.getValue(side)
                burst.snapTo(0f)
                burst.animateTo(1f, tween(620, easing = LinearOutSlowInEasing))
            }
        },
        decoration = {
            Side.entries.forEach { side ->
                val progress = halo.getValue(side).value
                if (progress < 1f) drawHalo(buttonCenter(side), progress)
            }
        },
    ) { quantity ->
        AnimatedContent(
            targetState = quantity,
            contentAlignment = Alignment.Center,
            transitionSpec = {
                (scaleIn(spring(dampingRatio = 0.7f, stiffness = 380f), initialScale = 1.8f) + fadeIn(tween(200)))
                    .togetherWith(scaleOut(tween(110), targetScale = 0.6f) + fadeOut(tween(110)))
                    .using(SizeTransform(clip = false))
            },
            label = "quantity",
        ) { QuantityText(it) }
    }
}

private fun DrawScope.drawHalo(center: Offset, progress: Float) {
    val radius = lerp(23.dp, 60.dp, progress).toPx()
    val alpha = 1f - progress
    drawCircle(MenuColors.Cyan, radius, center, alpha = 0.22f * alpha, style = Stroke(lerp(10.dp, 4.dp, progress).toPx()))
    drawCircle(
        Brush.sweepGradient(listOf(MenuColors.Cyan, MenuColors.Blue, MenuColors.Cyan, MenuColors.Blue, MenuColors.Cyan), center),
        radius,
        center,
        alpha = alpha,
        style = Stroke(lerp(3.dp, 1.2.dp, progress).toPx()),
    )
    val tickStart = radius + 5.dp.toPx()
    val tickLength = lerp(8.dp, 2.dp, progress).toPx()
    repeat(TickCount) { i ->
        val angle = (i + 0.5f) * 2f * PI.toFloat() / TickCount
        val direction = Offset(cos(angle), sin(angle))
        drawLine(
            MenuColors.Cyan.copy(alpha = alpha),
            center + direction * tickStart,
            center + direction * (tickStart + tickLength),
            2.dp.toPx(),
            StrokeCap.Round,
        )
    }
}
