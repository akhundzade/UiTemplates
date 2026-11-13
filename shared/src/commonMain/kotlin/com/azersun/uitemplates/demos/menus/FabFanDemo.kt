package com.azersun.uitemplates.demos.menus

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private val FanActions = listOf(
    Glyph.Camera to MenuColors.Violet,
    Glyph.Pencil to MenuColors.Blue,
    Glyph.Image to MenuColors.Pink,
    Glyph.Mic to MenuColors.Yellow,
    Glyph.Pin to MenuColors.Cyan,
)

@Composable
fun FabFanDemo() {
    var open by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (open) 135f else 0f, spring(dampingRatio = 0.55f, stiffness = 420f), label = "fab")
    val halo by animateFloatAsState(if (open) 1f else 0f, tween(380), label = "halo")

    MenuStage(3, "FAB Fan", "The + turns into ✕ as five buttons shoot up and fan out on an arc.") {
        Box(Modifier.fillMaxSize().padding(bottom = 90.dp), contentAlignment = Alignment.BottomCenter) {
            Canvas(Modifier.size(300.dp).offset(y = 118.dp)) {
                drawCircle(
                    Brush.radialGradient(
                        listOf(MenuColors.Blue.copy(alpha = 0.4f * halo), Color.Transparent),
                        center = center,
                        radius = size.minDimension / 2 * (0.5f + 0.5f * halo),
                    ),
                )
            }
            FanActions.forEachIndexed { i, (glyph, color) ->
                FanButton(index = i, count = FanActions.size, open = open, glyph = glyph, color = color) { open = false }
            }
            Box(
                Modifier
                    .size(64.dp)
                    .shadow(18.dp, CircleShape, ambientColor = MenuColors.Blue, spotColor = MenuColors.Blue)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(MenuColors.Blue, MenuColors.Violet)))
                    .clickable { open = !open },
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.size(24.dp).graphicsLayer { rotationZ = rotation }) {
                    val stroke = size.width * 0.13f
                    drawLine(Color.White, Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), stroke, StrokeCap.Round)
                    drawLine(Color.White, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), stroke, StrokeCap.Round)
                }
            }
        }
    }
}

@Composable
private fun FanButton(index: Int, count: Int, open: Boolean, glyph: Glyph, color: Color, onClick: () -> Unit) {
    val travel = remember { Animatable(0f) }
    LaunchedEffect(open) {
        if (open) {
            delay(index * 35L)
            travel.animateTo(1f, spring(dampingRatio = 0.52f, stiffness = 380f))
        } else {
            delay((count - 1 - index) * 22L)
            travel.animateTo(0f, tween(180))
        }
    }
    // Spread evenly across the upper half-circle, left to right.
    val angle = PI + PI * index / (count - 1)
    val radius = 104.dp
    Box(
        Modifier
            .offset {
                val r = radius.toPx() * travel.value
                IntOffset((cos(angle) * r).roundToInt(), (sin(angle) * r).roundToInt() - 8.dp.roundToPx())
            }
            .size(46.dp)
            .graphicsLayer {
                val t = travel.value
                alpha = t.coerceIn(0f, 1f)
                scaleX = 0.4f + 0.6f * t
                scaleY = 0.4f + 0.6f * t
            }
            .clip(CircleShape)
            .background(Color(0xFF221F45))
            .clickable(enabled = open, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        GlyphIcon(glyph, color, size = 20.dp)
    }
}
