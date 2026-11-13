package com.azersun.uitemplates.demos.viewas

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private val RollDark = Color(0xFF2A2160)
private val RollMid = Color(0xFF6B5CC4)
private val RollLight = Color(0xFFE6E0FF)

@Composable
fun ScrollUnrollDemo() {
    val state = rememberViewAsState()
    val unroll = remember { Animatable(0f) }
    val roller = remember { Animatable(0f) }
    var shown by remember { mutableStateOf(false) }

    LaunchedEffect(state.open) {
        if (state.open) {
            shown = true
            launch { roller.animateTo(1f, tween(140)) }
            unroll.animateTo(1f, spring(dampingRatio = 0.82f, stiffness = 140f))
        } else if (shown) {
            unroll.animateTo(0f, tween(440, easing = FastOutSlowInEasing))
            roller.animateTo(0f, tween(140))
            shown = false
        }
    }

    ViewAsScene(state, 5, "Scroll Unroll", "The menu unrolls downward like a scroll; rolls back up on pick.") {
        if (shown) {
            ViewAsMenu(state, Modifier.scroll(unroll = { unroll.value }, roller = { roller.value }))
        }
    }
}

/** Reveals the content above a rolled-paper cylinder that travels down to the panel's bottom. */
private fun Modifier.scroll(unroll: () -> Float, roller: () -> Float): Modifier = drawWithCache {
    val thickness = 14.dp.toPx()
    val overhang = 5.dp.toPx()
    val curl = 18.dp.toPx()
    val margin = 30.dp.toPx()
    val rollSize = Size(size.width + overhang * 2, thickness)
    val rollCorner = CornerRadius(thickness / 2)
    val rollBrush = Brush.verticalGradient(
        0f to RollDark,
        0.32f to RollLight,
        0.55f to RollMid,
        1f to RollDark,
        startY = -thickness / 2,
        endY = thickness / 2,
    )
    val curlBrush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f)), startY = -curl, endY = 0f)
    onDrawWithContent {
        val edge = size.height * unroll()
        if (edge > 0f) {
            clipRect(-margin, -margin, size.width + margin, edge) { this@onDrawWithContent.drawContent() }
            // The paper darkens where it bends into the roll; gone once fully open.
            val curlAlpha = ((1f - unroll()) * 5f).coerceIn(0f, 1f)
            if (curlAlpha > 0f) {
                translate(top = edge) {
                    drawRect(curlBrush, Offset(0f, -curl), Size(size.width, curl), alpha = curlAlpha)
                }
            }
        }
        val a = roller()
        if (a > 0f) {
            translate(top = edge) {
                drawRoundRect(Color.Black, Offset(-overhang, -thickness / 2 + 3.dp.toPx()), rollSize, rollCorner, alpha = 0.3f * a)
                drawRoundRect(rollBrush, Offset(-overhang, -thickness / 2), rollSize, rollCorner, alpha = a)
            }
        }
    }
}
