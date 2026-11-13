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
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

private val SpotBase = Color(0xFF07060F)
private val SpotViolet = Color(0xFF8B5CF6)
private val LitLine = Color(0xFFD4C8FF)

@Composable
fun SpotlightDemo() {
    val light = remember { Animatable(Offset(0.5f, 0.36f), Offset.VectorConverter) }
    val scope = rememberCoroutineScope()

    HeroStage(
        5,
        "Spotlight Effect",
        "A soft light glides to the tap and lights up the grid lines.",
        onTap = { target ->
            scope.launch { light.animateTo(target, spring(dampingRatio = 0.8f, stiffness = 45f)) }
        },
    ) {
        Spacer(
            Modifier.fillMaxSize().drawWithCache {
                val cell = 28.dp.toPx()
                val grid = Path().apply {
                    var x = (size.width % cell) / 2
                    while (x <= size.width) {
                        moveTo(x, 0f); lineTo(x, size.height)
                        x += cell
                    }
                    var y = (size.height % cell) / 2
                    while (y <= size.height) {
                        moveTo(0f, y); lineTo(size.width, y)
                        y += cell
                    }
                }
                val hairline = Stroke(max(1f, 0.8.dp.toPx()))
                val radius = min(size.width, size.height) * 0.6f
                val glow = Brush.radialGradient(
                    listOf(SpotViolet.copy(alpha = 0.42f), SpotViolet.copy(alpha = 0.12f), Color.Transparent),
                    center = Offset.Zero,
                    radius = radius,
                )
                val mask = Brush.radialGradient(
                    listOf(Color.White, Color.White.copy(alpha = 0.35f), Color.Transparent),
                    center = Offset.Zero,
                    radius = radius * 0.8f,
                )
                val bounds = size.toRect()
                val layerPaint = Paint()
                onDrawBehind {
                    drawRect(SpotBase)
                    drawPath(grid, Color.White, alpha = 0.05f, style = hairline)
                    val p = light.value
                    val lx = p.x * size.width
                    val ly = p.y * size.height
                    translate(lx, ly) { drawCircle(glow, radius, Offset.Zero) }
                    // A bright copy of the grid, kept only where the light's mask falls on it.
                    drawIntoCanvas { canvas ->
                        canvas.saveLayer(bounds, layerPaint)
                        drawPath(grid, LitLine, alpha = 0.85f, style = hairline)
                        translate(lx, ly) {
                            drawRect(mask, Offset(-lx, -ly), size, blendMode = BlendMode.DstIn)
                        }
                        canvas.restore()
                    }
                }
            },
        )
    }
}
