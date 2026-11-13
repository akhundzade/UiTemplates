package com.azersun.uitemplates.demos.menus

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun HamburgerMorphDemo() {
    var open by remember { mutableStateOf(false) }
    val converge = remember { Animatable(0f) }
    val spin = remember { Animatable(0f) }
    val fill = remember { Animatable(0f) }

    LaunchedEffect(open) {
        if (open) {
            launch { fill.animateTo(1f, tween(420, easing = FastOutSlowInEasing)) }
            converge.animateTo(1f, tween(160, easing = FastOutSlowInEasing))
            spin.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 380f))
        } else {
            launch { fill.animateTo(0f, tween(380, easing = FastOutSlowInEasing)) }
            spin.animateTo(0f, spring(dampingRatio = 0.75f, stiffness = 520f))
            converge.animateTo(0f, tween(180, easing = FastOutSlowInEasing))
        }
    }

    MenuStage(1, "Hamburger Morph", "Bars slide together, then spin into an ✕. Button fills violet→cyan.") {
        Box(Modifier.size(220.dp)) {
            Canvas(Modifier.fillMaxSize()) {
                val glow = fill.value
                drawCircle(
                    Brush.radialGradient(
                        listOf(MenuColors.Violet.copy(alpha = 0.45f * glow), Color.Transparent),
                        center = center,
                        radius = size.minDimension / 2,
                    ),
                )
            }
            Box(
                Modifier
                    .size(112.dp)
                    .align(androidx.compose.ui.Alignment.Center)
                    .clip(CircleShape)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { open = !open },
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    drawCircle(MenuColors.TileLight)
                    drawCircle(
                        Brush.linearGradient(listOf(MenuColors.Violet, MenuColors.Cyan), Offset.Zero, Offset(size.width, size.height)),
                        radius = size.minDimension / 2 * fill.value,
                    )
                    val length = size.width * 0.4f
                    val gap = size.height * 0.13f * (1f - converge.value)
                    val stroke = size.width * 0.055f
                    val c = center
                    fun bar(dy: Float, angle: Float, alpha: Float = 1f) = rotate(angle, c) {
                        drawLine(
                            Color.White.copy(alpha = alpha),
                            Offset(c.x - length / 2, c.y + dy),
                            Offset(c.x + length / 2, c.y + dy),
                            stroke,
                            StrokeCap.Round,
                        )
                    }
                    // The whole icon turns half a revolution while the outer bars cross, so it reads as a spin.
                    rotate(180f * spin.value, c) {
                        bar(-gap, 45f * spin.value)
                        bar(0f, 0f, alpha = 1f - converge.value)
                        bar(gap, -45f * spin.value)
                    }
                }
            }
        }
    }
}
