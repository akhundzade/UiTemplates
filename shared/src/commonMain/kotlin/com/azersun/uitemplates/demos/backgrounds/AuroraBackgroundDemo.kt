package com.azersun.uitemplates.demos.backgrounds

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.sin

private val AuroraSky = Brush.verticalGradient(listOf(Color(0xFF030614), Color(0xFF071029), Color(0xFF0A1433)))

/** One ribbon: its resting height and swing as fractions of the hero, and how its sine moves. */
private class Band(
    val color: Color,
    val baseY: Float,
    val amplitude: Float,
    val waves: Float,
    val speed: Float,
    val phase: Float,
    val glow: Float,
)

private val Bands = listOf(
    Band(Color(0xFF22D3EE), 0.24f, 0.05f, 1.3f, 0.45f, 0f, 0.55f),
    Band(Color(0xFF3B82F6), 0.34f, 0.06f, 0.9f, -0.35f, 1.7f, 0.5f),
    Band(Color(0xFF8B5CF6), 0.44f, 0.045f, 1.6f, 0.3f, 3.1f, 0.45f),
)

@Composable
fun AuroraBackgroundDemo() {
    val time = rememberFrameSeconds()
    val surge = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    HeroStage(
        2,
        "Aurora Background",
        "Glowing aurora bands sway; each tap makes them surge and brighten.",
        onTap = {
            scope.launch {
                surge.animateTo(1f, tween(260, easing = FastOutSlowInEasing))
                surge.animateTo(0f, tween(1500, easing = FastOutSlowInEasing))
            }
        },
    ) {
        Spacer(
            Modifier.fillMaxSize().drawWithCache {
                // Whole-pixel strips so neighbouring columns meet without anti-aliased seams.
                val step = ceil(3.dp.toPx())
                val up = size.height * 0.22f
                val down = 26.dp.toPx()
                val peak = up / (up + down)
                val strip = Size(step, up + down)
                val stripTop = Offset(0f, -up)
                val brushes = Bands.map {
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        peak * 0.55f to it.color.copy(alpha = 0.25f),
                        peak to it.color,
                        1f to Color.Transparent,
                        startY = -up,
                        endY = down,
                    )
                }
                onDrawBehind {
                    drawRect(AuroraSky)
                    val t = time.floatValue
                    val s = surge.value
                    val swing = 1f + 0.9f * s
                    val bright = 1f + 0.7f * s
                    for (b in Bands.indices) {
                        val band = Bands[b]
                        val k = (2 * PI).toFloat() * band.waves / size.width
                        var x = 0f
                        while (x < size.width) {
                            val wave = 0.8f * sin(k * x + t * band.speed + band.phase) +
                                0.2f * sin(2.3f * k * x - t * band.speed * 1.7f)
                            val y = size.height * (band.baseY + band.amplitude * swing * wave)
                            val curtain = 0.55f + 0.45f * sin(3.1f * k * x + t * 0.9f + band.phase)
                            translate(x, y) {
                                drawRect(
                                    brushes[b],
                                    stripTop,
                                    strip,
                                    alpha = (band.glow * bright * curtain).coerceAtMost(1f),
                                    blendMode = BlendMode.Plus,
                                )
                            }
                            x += step
                        }
                    }
                }
            },
        )
    }
}
