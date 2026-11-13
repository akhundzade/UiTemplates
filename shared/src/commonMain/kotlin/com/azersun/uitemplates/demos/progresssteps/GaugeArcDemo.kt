package com.azersun.uitemplates.demos.progresssteps

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azersun.uitemplates.demos.menus.MenuColors
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** The ticks sit inside the half circle, so the end labels have room beside the arc. */
private const val FirstTickDegrees = 200f
private const val TickSpanDegrees = 140f

private val ArcWidth = 14.dp
private val LabelGap = 10.dp
private val HubRadius = 22.dp
private val LabelStyle = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium)
private val NumberStyle = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold)

private fun tickDegrees(step: Float): Float = FirstTickDegrees + step * TickSpanDegrees / LastStep

private fun direction(degrees: Float): Offset {
    val radians = degrees * PI.toFloat() / 180f
    return Offset(cos(radians), sin(radians))
}

@Composable
fun GaugeArcDemo() {
    StepsScene(
        number = 6,
        title = "Gauge Arc",
        caption = "The filled arc sweeps to the next step and the needle springs there with a wobble; the hub shows the step number.",
    ) { state ->
        val needle = remember { Animatable(StartStep.toFloat()) }
        val fill = remember { Animatable(StartStep.toFloat()) }
        val pop = remember { Animatable(1f) }
        val measurer = rememberTextMeasurer()

        LaunchedEffect(state.step) {
            val target = state.step.toFloat()
            if (needle.targetValue == target) return@LaunchedEffect
            launch { fill.animateTo(target, tween(520, easing = FastOutSlowInEasing)) }
            launch {
                pop.snapTo(0.6f)
                pop.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 500f))
            }
            needle.animateTo(target, spring(dampingRatio = 0.3f, stiffness = 110f))
        }

        Spacer(
            Modifier
                .fillMaxWidth()
                .height(150.dp)
                .drawWithCache {
                    val labels = StepNames.map { measurer.measure(it, LabelStyle) }
                    val numbers = StepNames.indices.map { measurer.measure("${it + 1}", NumberStyle) }
                    val labelWidth = labels.maxOf { it.size.width }
                    val labelHeight = labels.maxOf { it.size.height }
                    val stroke = ArcWidth.toPx()
                    val gap = LabelGap.toPx()
                    val center = Offset(size.width / 2, size.height - HubRadius.toPx() - 6.dp.toPx())
                    val endCos = abs(direction(FirstTickDegrees).x)
                    val radius = min(
                        (center.x - labelWidth) / endCos - stroke / 2 - gap,
                        center.y - stroke / 2 - gap - labelHeight,
                    )
                    val arcTopLeft = Offset(center.x - radius, center.y - radius)
                    val arcSize = Size(radius * 2, radius * 2)
                    val brush = Brush.horizontalGradient(listOf(StepColors.Blue, StepColors.Cyan), center.x - radius, center.x + radius)
                    val arc = Stroke(stroke, cap = StrokeCap.Round)
                    val hubRing = Stroke(2.dp.toPx())
                    val needleLength = radius - stroke / 2 - 6.dp.toPx()
                    onDrawBehind {
                        val n = needle.value
                        val f = fill.value
                        val step = state.step
                        drawArc(StepColors.Track, 180f, 180f, false, arcTopLeft, arcSize, style = arc)
                        drawArc(brush, 180f, tickDegrees(f) - 180f, false, arcTopLeft, arcSize, style = arc)
                        for (k in 0..18) {
                            val d = direction(180f + k * 10f)
                            val inner = radius - stroke / 2 - 10.dp.toPx()
                            drawLine(
                                MenuColors.Muted,
                                center + d * inner,
                                center + d * (inner + 4.dp.toPx()),
                                1.dp.toPx(),
                                alpha = 0.4f,
                            )
                        }
                        for (i in 0..LastStep) {
                            val d = direction(tickDegrees(i.toFloat()))
                            drawCircle(if (f >= i - 0.02f) Color.White else MenuColors.Muted, 3.dp.toPx(), center + d * radius)
                            val label = labels[i]
                            val anchor = center + d * (radius + stroke / 2 + gap)
                            // Shift each label away from the arc along its own direction.
                            val topLeft = Offset(
                                anchor.x + (d.x - 1f) * label.size.width / 2,
                                anchor.y + (d.y - 1f) * label.size.height / 2,
                            )
                            val near = (1f - abs(n - i)).coerceIn(0f, 1f)
                            drawText(label, color = lerp(MenuColors.Muted, MenuColors.Text, near), topLeft = topLeft)
                        }
                        val d = direction(tickDegrees(n))
                        drawLine(Color.White, center, center + d * needleLength, 9.dp.toPx(), StrokeCap.Round, alpha = 0.12f)
                        drawLine(Color.White, center, center + d * needleLength, 3.5.dp.toPx(), StrokeCap.Round)
                        drawCircle(MenuColors.Screen, HubRadius.toPx(), center)
                        drawCircle(StepColors.Blue, HubRadius.toPx(), center, style = hubRing)
                        val number = numbers[step]
                        val scale = pop.value
                        withTransform({ scale(scale, scale, center) }) {
                            drawText(
                                number,
                                color = Color.White,
                                topLeft = center - Offset(number.size.width / 2f, number.size.height / 2f),
                            )
                        }
                    }
                },
        )
    }
}
