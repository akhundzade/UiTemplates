package com.azersun.uitemplates.demos.backgrounds

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.random.Random

private val NoiseBase = Color(0xFF0A0912)
private val GlowViolet = Color(0xFF8B5CF6)
private val GlowTeal = Color(0xFF14B8A6)
private const val TilePx = 192
private const val GrainFrameMillis = 42L

/** White and black specks at two strengths, each covering a tenth of the tile's pixels. */
private val GrainShades = listOf(
    Color.White.copy(alpha = 0.9f),
    Color.White.copy(alpha = 0.45f),
    Color.Black.copy(alpha = 0.9f),
    Color.Black.copy(alpha = 0.45f),
)

private fun noiseTile(): ImageBitmap {
    val image = ImageBitmap(TilePx, TilePx)
    val canvas = Canvas(image)
    val random = Random(7)
    val paint = Paint().apply {
        isAntiAlias = false
        strokeWidth = 1f
        strokeCap = StrokeCap.Square
    }
    GrainShades.forEach { shade ->
        paint.color = shade
        val points = List(TilePx * TilePx / 10) {
            Offset(random.nextInt(TilePx) + 0.5f, random.nextInt(TilePx) + 0.5f)
        }
        canvas.drawPoints(PointMode.Points, points, paint)
    }
    return image
}

@Composable
fun NoiseTextureDemo() {
    val grain = remember { noiseTile() }
    val pulse = remember { Animatable(0f) }
    val shiftX = remember { mutableFloatStateOf(0f) }
    val shiftY = remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()

    // Film grain changes at roughly film rate rather than every display frame.
    LaunchedEffect(Unit) {
        while (true) {
            shiftX.floatValue = Random.nextFloat()
            shiftY.floatValue = Random.nextFloat()
            delay(GrainFrameMillis)
        }
    }

    HeroStage(
        3,
        "Noise Texture",
        "Film grain over a soft glow; each tap pulses the grain and the light.",
        onTap = {
            scope.launch {
                pulse.animateTo(1f, tween(180, easing = FastOutSlowInEasing))
                pulse.animateTo(0f, tween(900, easing = FastOutSlowInEasing))
            }
        },
    ) {
        Spacer(
            Modifier.fillMaxSize().drawWithCache {
                val radius = max(size.width, size.height) * 0.55f
                val violet = Brush.radialGradient(
                    listOf(GlowViolet.copy(alpha = 0.55f), GlowViolet.copy(alpha = 0.18f), Color.Transparent),
                    center = Offset.Zero,
                    radius = radius,
                )
                val teal = Brush.radialGradient(
                    listOf(GlowTeal.copy(alpha = 0.5f), GlowTeal.copy(alpha = 0.15f), Color.Transparent),
                    center = Offset.Zero,
                    radius = radius,
                )
                val grainBrush = ShaderBrush(ImageShader(grain, TileMode.Repeated, TileMode.Repeated))
                // About 0.7dp per noise pixel so the grain reads the same on every density.
                val grainScale = max(1f, density * 0.7f)
                val span = TilePx * grainScale
                val grainSize = Size((size.width + span) / grainScale, (size.height + span) / grainScale)
                onDrawBehind {
                    drawRect(NoiseBase)
                    val p = pulse.value
                    val glowAlpha = 0.65f + 0.35f * p
                    val glowScale = 1f + 0.12f * p
                    withTransform({
                        translate(size.width * 0.28f, size.height * 0.32f)
                        scale(glowScale, glowScale, Offset.Zero)
                    }) { drawCircle(violet, radius, Offset.Zero, alpha = glowAlpha) }
                    withTransform({
                        translate(size.width * 0.78f, size.height * 0.72f)
                        scale(glowScale, glowScale, Offset.Zero)
                    }) { drawCircle(teal, radius, Offset.Zero, alpha = glowAlpha) }
                    withTransform({
                        translate(-shiftX.floatValue * span, -shiftY.floatValue * span)
                        scale(grainScale, grainScale, Offset.Zero)
                    }) { drawRect(grainBrush, Offset.Zero, grainSize, alpha = 0.1f + 0.22f * p) }
                }
            },
        )
    }
}
