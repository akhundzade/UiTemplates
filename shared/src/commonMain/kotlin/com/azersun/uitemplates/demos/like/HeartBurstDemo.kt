package com.azersun.uitemplates.demos.like

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val BurstCount = 12
private val HeartSide = 96.dp

@Composable
fun HeartBurstDemo() {
    var liked by remember { mutableStateOf(false) }
    val heartScale = remember { Animatable(1f) }
    val fill = remember { Animatable(0f) }
    // Ring and burst run 0 → 1 per like and are hidden at 1.
    val ring = remember { Animatable(1f) }
    val burst = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    fun toggle() {
        liked = !liked
        if (liked) {
            scope.launch { heartScale.pop() }
            scope.launch {
                delay(60)
                fill.animateTo(1f, tween(160))
            }
            scope.launch {
                ring.snapTo(0f)
                delay(60)
                ring.animateTo(1f, tween(520, easing = LinearOutSlowInEasing))
            }
            scope.launch {
                burst.snapTo(0f)
                delay(80)
                burst.animateTo(1f, tween(700, easing = LinearOutSlowInEasing))
            }
        } else {
            scope.launch { heartScale.shrink() }
            scope.launch { fill.animateTo(0f, tween(180)) }
        }
    }

    LikeStage(
        1,
        "Heart Burst",
        "The heart pops and fills, a ring ripples out and twelve tiny hearts and dots burst around it.",
        autoplay = { toggle() },
    ) {
        Box(
            Modifier
                .size(280.dp)
                .drawWithCache {
                    val side = HeartSide.toPx()
                    val corner = size.center - Offset(side / 2, side / 2)
                    val heart = Path().apply { heart(corner, side) }
                    val mini = Path()
                    val brush = Brush.linearGradient(listOf(LikeColors.Pink, LikeColors.Violet), corner, corner + Offset(side, side))
                    val outlineWidth = 3.dp.toPx()
                    onDrawBehind {
                        val r = ring.value
                        val b = burst.value
                        val f = fill.value
                        val s = heartScale.value
                        if (r < 1f) {
                            drawCircle(
                                brush,
                                radius = side * (0.45f + 0.6f * r),
                                alpha = 1f - r,
                                style = Stroke(side * 0.08f * (1f - r)),
                            )
                        }
                        if (b < 1f) {
                            val alpha = if (b < 0.6f) 1f else (1f - b) / 0.4f
                            repeat(BurstCount) { i ->
                                val angle = (i * 360f / BurstCount - 90f + 15f) * PI.toFloat() / 180f
                                val reach = if (i % 2 == 0) 1f else 0.82f
                                val distance = side * (0.5f + 0.75f * b) * reach
                                val at = center + Offset(cos(angle), sin(angle)) * distance
                                val color = SparkColors[i % SparkColors.size]
                                val shrink = 1f - 0.6f * b
                                if (i % 2 == 0) {
                                    val mSide = side * 0.16f * shrink
                                    mini.heart(at - Offset(mSide / 2, mSide / 2), mSide)
                                    drawPath(mini, color, alpha = alpha)
                                } else {
                                    drawCircle(color, side * 0.045f * shrink, at, alpha = alpha)
                                }
                            }
                        }
                        scale(s, center) { drawLikeHeart(heart, f, brush, LikeColors.Outline, outlineWidth) }
                    }
                },
        ) {
            Box(
                Modifier
                    .size(HeartSide + 24.dp)
                    .align(Alignment.Center)
                    .clickable(
                        interactionSource = null,
                        indication = null,
                        onClickLabel = if (liked) "Unlike" else "Like",
                        role = Role.Button,
                        onClick = { toggle() },
                    ),
            )
        }
    }
}
