package com.azersun.uitemplates.demos.progresssteps

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.azersun.uitemplates.demos.menus.MenuColors
import kotlin.math.abs
import kotlin.math.sign

private const val BeadMillis = 700
private const val TrailDots = 6

@Composable
fun BeadLineDemo() {
    StepsScene(
        number = 1,
        title = "Bead Line",
        caption = "A glowing bead runs along the line to the next dot; the passed segment lights up and the dot gets a ring.",
    ) { state ->
        val bead = remember { Animatable(StartStep.toFloat()) }
        LaunchedEffect(state.step) {
            bead.animateTo(state.step.toFloat(), tween(BeadMillis, easing = FastOutSlowInEasing))
        }

        Spacer(
            Modifier
                .fillMaxWidth()
                .height(44.dp)
                .drawWithCache {
                    val slot = size.width / StepNames.size
                    val y = size.height / 2
                    val first = Offset(slot / 2, y)
                    val last = Offset(size.width - slot / 2, y)
                    val fill = Brush.horizontalGradient(listOf(StepColors.Blue, StepColors.Cyan), first.x, last.x)
                    val line = 4.dp.toPx()
                    val dot = 6.dp.toPx()
                    val ring = Stroke(2.dp.toPx())
                    onDrawBehind {
                        val p = bead.value
                        val step = state.step
                        val at = Offset(slot * (p + 0.5f), y)
                        drawLine(StepColors.Track, first, last, line, StrokeCap.Round)
                        drawLine(fill, first, at, line, StrokeCap.Round)
                        for (i in 0..LastStep) {
                            val c = Offset(slot * (i + 0.5f), y)
                            if (p >= i - 0.02f) {
                                drawCircle(StepColors.at(i.toFloat()), dot, c)
                            } else {
                                drawCircle(StepColors.Track, dot, c)
                                drawCircle(MenuColors.Tile, dot * 0.45f, c)
                            }
                        }
                        // The ring only grows in over the last stretch, so it reads as the bead's arrival.
                        val arrival = (1f - abs(p - step) * 2.5f).coerceIn(0f, 1f)
                        if (arrival > 0f) {
                            drawCircle(
                                StepColors.at(step.toFloat()),
                                dot + 6.dp.toPx() * backOut(arrival),
                                Offset(slot * (step + 0.5f), y),
                                alpha = arrival,
                                style = ring,
                            )
                        }
                        val moving = (abs(p - step) * 4f).coerceIn(0f, 1f)
                        val behind = -sign(step - p) * slot * 0.05f
                        for (k in TrailDots downTo 1) {
                            val fade = 1f - k / (TrailDots + 1f)
                            drawCircle(Color.White, 3.5.dp.toPx() * fade, at + Offset(behind * k, 0f), alpha = 0.5f * fade * moving)
                        }
                        drawCircle(StepColors.Cyan, 16.dp.toPx(), at, alpha = 0.12f + 0.14f * moving)
                        drawCircle(StepColors.Cyan, 9.dp.toPx(), at, alpha = 0.35f)
                        drawCircle(Color.White, 4.5.dp.toPx(), at)
                    }
                },
        )
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth()) {
            StepNames.indices.forEach { i ->
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { StepLabel(i, state.step) }
            }
        }
    }
}
