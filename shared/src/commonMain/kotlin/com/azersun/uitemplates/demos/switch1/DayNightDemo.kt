package com.azersun.uitemplates.demos.switch1

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.azersun.uitemplates.demos.checkbox1.CheckboxStage
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

// x and y are fractions of the track, radius is in dp, phase offsets each star's twinkle.
private class Star(val x: Float, val y: Float, val radius: Float, val phase: Float)

private val Stars = listOf(
    Star(0.12f, 0.3f, 1.6f, 0f),
    Star(0.2f, 0.66f, 1.1f, 1.7f),
    Star(0.29f, 0.22f, 1.2f, 3.1f),
    Star(0.38f, 0.5f, 1.8f, 4.4f),
    Star(0.16f, 0.84f, 1f, 2.4f),
    Star(0.47f, 0.2f, 1.1f, 5.3f),
    Star(0.52f, 0.74f, 1.3f, 0.9f),
)

// (x, y, radius) of each crater as fractions of the moon's diameter, from its centre.
private val Craters = listOf(
    Triple(-0.17f, -0.12f, 0.15f),
    Triple(0.18f, 0.1f, 0.12f),
    Triple(-0.06f, 0.26f, 0.08f),
)

private val DaySky = Brush.verticalGradient(listOf(Color(0xFF3E9BEA), Color(0xFF86CCFF)))
private val NightSky = Brush.verticalGradient(listOf(Color(0xFF0A1033), Color(0xFF1F2766)))
private val Sun = Color(0xFFFFC83D)
private val Moon = Color(0xFFE6E4F4)
private val Crater = Color(0xFFC3BFDB)
private val CloudBack = Color(0xFFCFE8FF)
private val TrackW = 168.dp
private val TrackH = 76.dp
private val Inset = 8.dp
private const val TwoPi = 2f * PI.toFloat()

@Composable
fun DayNightDemo() {
    var night by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }
    val twinkle = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(night) {
        if (!night) return@LaunchedEffect
        while (true) {
            twinkle.snapTo(0f)
            twinkle.animateTo(TwoPi, tween(2600, easing = LinearEasing))
        }
    }

    fun toggle() {
        night = !night
        scope.launch { progress.animateTo(if (night) 1f else 0f, tween(750, easing = FastOutSlowInEasing)) }
    }

    CheckboxStage(
        2,
        "Day & Night",
        "Sun knob rolls right and becomes a moon. Clouds sink, stars twinkle in.",
        autoplay = { toggle() },
    ) {
        Spacer(
            Modifier
                .size(TrackW, TrackH)
                .clickable(interactionSource = null, indication = null, role = Role.Switch) { toggle() }
                .drawWithCache {
                    val clip = Path().apply {
                        addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(size.height / 2)))
                    }
                    val inset = Inset.toPx()
                    val knobRadius = size.height / 2 - inset
                    val travel = size.width - inset * 2 - knobRadius * 2
                    // A ball rolling this far turns by travel / radius radians.
                    val rollDegrees = travel / knobRadius * 180f / PI.toFloat()
                    val cloudUnit = size.height / 10f
                    val dp = 1.dp.toPx()
                    onDrawBehind {
                        val p = progress.value
                        val center = Offset(inset + knobRadius + p * travel, size.height / 2)
                        clipPath(clip) {
                            drawRect(DaySky)
                            if (p > 0f) drawRect(NightSky, alpha = p)
                            for (i in 1..3) drawCircle(Color.White, knobRadius * (1f + i * 0.42f), center, alpha = 0.07f)
                            val stars = ((p - 0.35f) / 0.65f).coerceIn(0f, 1f)
                            if (stars > 0f) {
                                val t = twinkle.value
                                val drop = (1f - stars) * size.height * 0.25f
                                Stars.forEach { star ->
                                    drawCircle(
                                        Color.White,
                                        star.radius * dp,
                                        Offset(star.x * size.width, star.y * size.height - drop),
                                        alpha = stars * (0.6f + 0.4f * sin(t + star.phase)),
                                    )
                                }
                            }
                            val sink = (p / 0.65f).coerceIn(0f, 1f) * size.height * 0.75f
                            drawCloud(Offset(size.width * 0.66f, size.height * 0.6f + sink * 1.15f), cloudUnit, CloudBack)
                            drawCloud(Offset(size.width * 0.8f, size.height * 0.8f + sink), cloudUnit * 1.1f, Color.White)
                        }
                        drawKnob(center, knobRadius, lerp(Sun, Moon, p))
                        if (p > 0f) {
                            rotate(p * rollDegrees, center) {
                                Craters.forEach { (x, y, r) ->
                                    val diameter = knobRadius * 2
                                    drawCircle(Crater, r * diameter, center + Offset(x * diameter, y * diameter), alpha = p)
                                }
                            }
                        }
                    }
                },
        )
    }
}

private fun DrawScope.drawCloud(base: Offset, u: Float, color: Color) {
    drawCircle(color, u * 1.1f, base + Offset(-u * 1.6f, 0f))
    drawCircle(color, u * 1.6f, base + Offset(-u * 0.2f, -u * 0.7f))
    drawCircle(color, u * 1.3f, base + Offset(u * 1.4f, -u * 0.2f))
    drawCircle(color, u, base + Offset(u * 2.6f, u * 0.3f))
    drawRoundRect(color, base + Offset(-u * 2.7f, -u * 0.1f), Size(u * 6.3f, u * 1.4f), CornerRadius(u * 0.7f))
}
