package com.azersun.uitemplates.demos.password

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val ChainStaggerMillis = 80L
private const val SettleMillis = 420

@Composable
fun ChainLightDemo() {
    val form = remember { PasswordFormState() }
    // Each segment keeps the color it was last lit with until the chain reaches it.
    val lit = remember { mutableStateListOf<Color?>(*arrayOfNulls(SegmentCount)) }
    val flash = remember { List(SegmentCount) { Animatable(0f) } }

    LaunchedEffect(form.level) {
        val level = form.level
        val color = StrengthLevels[level].color
        repeat(SegmentCount) { i ->
            launch {
                delay(i * ChainStaggerMillis)
                if (i < level) {
                    lit[i] = color
                    flash[i].snapTo(1f)
                    flash[i].animateTo(0f, tween(SettleMillis, easing = FastOutSlowInEasing))
                } else {
                    lit[i] = null
                    flash[i].snapTo(0f)
                }
            }
        }
    }

    PasswordStage(4, "Chain Light", "Segments relight one after another like a chain, with a flash.", form) {
        Canvas(Modifier.fillMaxSize()) {
            drawTrack()
            repeat(SegmentCount) { i ->
                val color = lit[i] ?: return@repeat
                val f = flash[i].value
                if (f > 0f) {
                    listOf(3.dp to 0.35f, 7.dp to 0.15f).forEach { (spread, alpha) ->
                        val s = spread.toPx()
                        drawRoundRect(
                            color.copy(alpha = alpha * f),
                            topLeft = Offset(segmentLeft(i) - s, -s),
                            size = Size(segmentWidth() + s * 2, size.height + s * 2),
                            cornerRadius = CornerRadius(size.height / 2 + s),
                        )
                    }
                }
                drawSegment(i, lerp(color, PasswordColors.FlashWhite, 0.85f * f))
            }
        }
    }
}
