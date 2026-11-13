package com.azersun.uitemplates.demos.checkbox1

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

private class Droplet(val x: Float, val peak: Float, val start: Float, val radius: Float)

// x and peak are fractions of the box side; start is a fraction of the droplet animation.
private val Droplets = listOf(
    Droplet(0.22f, 0.75f, 0f, 2.4f),
    Droplet(0.48f, 1.15f, 0.08f, 3f),
    Droplet(0.7f, 0.9f, 0.04f, 2.2f),
    Droplet(0.86f, 0.55f, 0.14f, 1.8f),
)
private const val DropletSpan = 0.78f
private val BoxSide = 34.dp

@Composable
fun InkFillDemo() {
    var on by remember { mutableStateOf(false) }
    val level = remember { Animatable(0f) }
    val tick = remember { Animatable(0f) }
    val drops = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    var job by remember { mutableStateOf<Job?>(null) }

    fun toggle() {
        on = !on
        job?.cancel()
        job = scope.launch {
            if (on) {
                scope.launch {
                    delay(320)
                    drops.snapTo(0f)
                    drops.animateTo(1f, tween(760, easing = LinearEasing))
                }
                level.animateTo(1f, tween(950, easing = FastOutSlowInEasing))
                tick.animateTo(1f, tween(300, easing = FastOutSlowInEasing))
            } else {
                tick.animateTo(0f, tween(140))
                level.animateTo(0f, tween(450, easing = FastOutSlowInEasing))
            }
        }
    }

    CheckboxStage(
        6,
        "Ink Fill",
        "Liquid rises in the box with a wavy top, then a white ✓ draws in.",
        autoplay = { toggle() },
    ) {
        Row(
            Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .checkPanel()
                .clickable(interactionSource = null, indication = null, role = Role.Checkbox) { toggle() }
                .padding(horizontal = 18.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(
                Modifier
                    .size(BoxSide)
                    .drawWithCache {
                        val side = size.minDimension
                        val radius = side * 0.28f
                        val stroke = side * 0.075f
                        val clip = Path().apply { addRoundRect(RoundRect(0f, 0f, side, side, CornerRadius(radius))) }
                        val trim = PathTrim(Path().apply { checkMark(Offset.Zero, side) })
                        val checkStyle = checkStroke(side)
                        val liquid = Brush.verticalGradient(listOf(Color(0xFF7FA6FF), CheckColors.FillEnd), 0f, side)
                        val front = Path()
                        val back = Path()
                        val amp = side * 0.09f
                        val step = side / 16f
                        fun Path.wave(surface: Float, height: Float, phase: Float) {
                            reset()
                            moveTo(0f, side)
                            var x = 0f
                            while (x <= side) {
                                lineTo(x, surface + sin(phase + x / side * 2f * PI.toFloat()) * height)
                                x += step
                            }
                            lineTo(side, side)
                            close()
                        }
                        onDrawBehind {
                            val fill = level.value
                            val t = tick.value
                            val d = drops.value
                            // The wave calms to flat at both ends, so empty and full boxes stay crisp.
                            val height = amp * sin(fill * PI.toFloat())
                            val surface = side * (1f - fill)
                            val phase = fill * 5f * PI.toFloat()
                            if (fill > 0f) {
                                back.wave(surface - height * 0.3f, height * 0.8f, -phase + 2f)
                                front.wave(surface, height, phase)
                                clipPath(clip) {
                                    drawPath(back, liquid, alpha = 0.45f)
                                    drawPath(front, liquid)
                                }
                            }
                            drawRoundRect(
                                lerp(CheckColors.Outline, CheckColors.FillStart, (fill * 3f).coerceAtMost(1f)),
                                Offset(stroke / 2, stroke / 2),
                                Size(side - stroke, side - stroke),
                                CornerRadius(radius - stroke / 2),
                                style = Stroke(stroke),
                            )
                            if (t > 0f) drawPath(trim.fraction(0f, t), Color.White, style = checkStyle)
                            if (d < 1f) {
                                Droplets.forEach { drop ->
                                    val p = ((d - drop.start) / DropletSpan).coerceIn(0f, 1f)
                                    if (p > 0f && p < 1f) {
                                        val lift = side * drop.peak * 4f * p * (1f - p)
                                        drawCircle(
                                            CheckColors.FillStart,
                                            drop.radius.dp.toPx(),
                                            Offset(side * drop.x, surface - lift),
                                            alpha = 1f - p * p,
                                        )
                                    }
                                }
                            }
                        }
                    },
            )
            Spacer(Modifier.width(16.dp))
            Text("Keep me signed in", color = CheckColors.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
