package com.azersun.uitemplates.demos.backgrounds

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.max

private const val FlowSeconds = 9f
private const val TurnDegrees = 60f

private val Palettes = listOf(
    listOf(Color(0xFF7C3AED), Color(0xFF2563EB), Color(0xFFDB2777), Color(0xFF0D9488)),
    listOf(Color(0xFF0EA5E9), Color(0xFF14B8A6), Color(0xFF6366F1), Color(0xFF22D3EE)),
    listOf(Color(0xFFF43F5E), Color(0xFFF97316), Color(0xFFA855F7), Color(0xFFEC4899)),
    listOf(Color(0xFF10B981), Color(0xFF06B6D4), Color(0xFF8B5CF6), Color(0xFF84CC16)),
)

@Composable
fun AnimatedGradientDemo() {
    val time = rememberFrameSeconds()
    val angle = remember { Animatable(30f) }
    val blend = remember { Animatable(1f) }
    var palette by remember { mutableIntStateOf(0) }
    var previous by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    HeroStage(
        6,
        "Animated Gradient",
        "A slowly flowing gradient; each tap turns it to the next angle and hue.",
        onTap = {
            previous = palette
            palette = (palette + 1) % Palettes.size
            scope.launch { angle.animateTo(angle.targetValue + TurnDegrees, spring(dampingRatio = 0.85f, stiffness = 30f)) }
            scope.launch {
                blend.snapTo(0f)
                blend.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
            }
        },
    ) {
        Spacer(
            Modifier.fillMaxSize().drawWithCache {
                val period = max(size.width, size.height) * 1.5f
                // Repeating the first colour at the end makes the tiled gradient seamless.
                val brushes = Palettes.map {
                    Brush.linearGradient(it + it.first(), Offset.Zero, Offset(period, 0f), TileMode.Repeated)
                }
                val half = hypot(size.width, size.height) / 2 + 2f
                val cover = Size(half * 2 + period, half * 2)
                val vignette = Brush.radialGradient(
                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f)),
                    radius = half,
                )
                onDrawBehind {
                    val flow = (time.floatValue / FlowSeconds * period) % period
                    val b = blend.value
                    // Painting the new palette over the old at alpha b is a per-pixel colour lerp.
                    withTransform({
                        rotate(angle.value, center)
                        translate(flow, 0f)
                    }) {
                        val topLeft = Offset(center.x - half - period, center.y - half)
                        if (b < 1f) drawRect(brushes[previous], topLeft, cover)
                        drawRect(brushes[palette], topLeft, cover, alpha = b)
                    }
                    drawRect(vignette)
                }
            },
        )
    }
}
