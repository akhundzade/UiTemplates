package com.azersun.uitemplates.demos.backgrounds

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

private val MeshBase = Color(0xFF070A1F)

private class Blob(val color: Color, start: Offset, val driftSpeed: Float, val driftPhase: Float) {
    val position = Animatable(start, Offset.VectorConverter)
}

private fun meshBlobs() = listOf(
    Blob(Color(0xFF7C3AED), Offset(0.22f, 0.24f), 0.31f, 0f),
    Blob(Color(0xFF06B6D4), Offset(0.8f, 0.32f), 0.23f, 1.9f),
    Blob(Color(0xFFEC4899), Offset(0.7f, 0.8f), 0.27f, 3.4f),
    Blob(Color(0xFF4338CA), Offset(0.2f, 0.74f), 0.19f, 5.1f),
)

private fun randomSpot() = Offset(0.1f + Random.nextFloat() * 0.8f, 0.1f + Random.nextFloat() * 0.8f)

@Composable
fun MeshGradientDemo() {
    val blobs = remember { meshBlobs() }
    val time = rememberFrameSeconds()
    val scope = rememberCoroutineScope()

    HeroStage(
        1,
        "Mesh Gradient",
        "Soft violet, cyan and pink blobs; each tap drifts them to new spots.",
        onTap = {
            blobs.forEach { blob ->
                scope.launch { blob.position.animateTo(randomSpot(), spring(dampingRatio = 0.9f, stiffness = 16f)) }
            }
        },
    ) {
        Spacer(
            Modifier.fillMaxSize().drawWithCache {
                val radius = max(size.width, size.height) * 0.62f
                // Centred on the origin and moved with translate, so each shader is built once per size.
                val brushes = blobs.map {
                    Brush.radialGradient(
                        0f to it.color.copy(alpha = 0.85f),
                        0.45f to it.color.copy(alpha = 0.38f),
                        1f to Color.Transparent,
                        center = Offset.Zero,
                        radius = radius,
                    )
                }
                onDrawBehind {
                    drawRect(MeshBase)
                    val t = time.floatValue
                    for (i in blobs.indices) {
                        val blob = blobs[i]
                        val p = blob.position.value
                        val dx = sin(t * blob.driftSpeed + blob.driftPhase) * 0.07f
                        val dy = cos(t * blob.driftSpeed * 0.8f + blob.driftPhase) * 0.06f
                        translate((p.x + dx) * size.width, (p.y + dy) * size.height) {
                            drawCircle(brushes[i], radius, Offset.Zero)
                        }
                    }
                }
            },
        )
    }
}
