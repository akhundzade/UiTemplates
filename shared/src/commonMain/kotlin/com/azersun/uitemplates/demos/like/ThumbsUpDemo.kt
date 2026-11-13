package com.azersun.uitemplates.demos.like

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val RestTilt = -25f
private const val SparkCount = 10
private val ThumbSide = 110.dp

@Composable
fun ThumbsUpDemo() {
    var liked by remember { mutableStateOf(false) }
    val tilt = remember { Animatable(RestTilt) }
    val fill = remember { Animatable(0f) }
    val bump = remember { Animatable(1f) }
    // 0 → 1 per like; hidden at 1.
    val sparks = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    fun toggle() {
        liked = !liked
        if (liked) {
            // Underdamped: overshoots upright by about 10°, then wobbles in.
            scope.launch { tilt.animateTo(0f, spring(dampingRatio = 0.28f, stiffness = 260f)) }
            scope.launch { fill.animateTo(1f, tween(220)) }
            scope.launch {
                bump.animateTo(1.12f, tween(120))
                bump.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 400f))
            }
            scope.launch {
                sparks.snapTo(0f)
                delay(80)
                sparks.animateTo(1f, tween(600, easing = LinearOutSlowInEasing))
            }
        } else {
            scope.launch { tilt.animateTo(RestTilt, tween(280, easing = FastOutSlowInEasing)) }
            scope.launch { fill.animateTo(0f, tween(200)) }
        }
    }

    LikeStage(
        6,
        "Thumbs Up",
        "The tilted thumb springs upright with a wobble, fills violet to cyan and throws off sparks.",
        autoplay = { toggle() },
    ) {
        Box(
            Modifier
                .size(260.dp)
                .drawWithCache {
                    val side = ThumbSide.toPx()
                    val corner = size.center - Offset(side / 2, side / 2)
                    val cuff = Path()
                    val hand = Path()
                    thumbPaths(corner, side, cuff, hand)
                    val k = side / 22f
                    val pivot = Offset(corner.x + 3f * k, corner.y + 20f * k)
                    val gradient = Brush.linearGradient(listOf(LikeColors.Violet, LikeColors.Cyan), corner + Offset(0f, side), corner + Offset(side, 0f))
                    val outline = Stroke(1.6f * k, join = StrokeJoin.Round)
                    val sparkWidth = 3.dp.toPx()
                    val sparkLength = 16.dp.toPx()
                    val reach = side * 0.62f
                    onDrawBehind {
                        val angle = tilt.value
                        val f = fill.value
                        val b = bump.value
                        val p = sparks.value
                        if (p < 1f) {
                            repeat(SparkCount) { i ->
                                val a = (i * 360f / SparkCount - 90f) * PI.toFloat() / 180f
                                val dir = Offset(cos(a), sin(a))
                                val inner = reach + side * 0.4f * p
                                val length = sparkLength * (if (i % 2 == 0) 1f else 0.6f) * (1f - p)
                                drawLine(
                                    SparkColors[i % SparkColors.size],
                                    center + dir * inner,
                                    center + dir * (inner + length),
                                    sparkWidth,
                                    StrokeCap.Round,
                                    alpha = 1f - p * p,
                                )
                            }
                        }
                        scale(b, center) {
                            rotate(angle, pivot) {
                                if (f < 1f) {
                                    drawPath(cuff, LikeColors.Outline, alpha = 1f - f, style = outline)
                                    drawPath(hand, LikeColors.Outline, alpha = 1f - f, style = outline)
                                }
                                if (f > 0f) {
                                    drawPath(cuff, gradient, alpha = f)
                                    drawPath(hand, gradient, alpha = f)
                                    // The matching stroke rounds the filled corners like the outline's.
                                    drawPath(hand, gradient, alpha = f, style = outline)
                                }
                            }
                        }
                    }
                },
        ) {
            Box(
                Modifier
                    .align(Alignment.Center)
                    .size(ThumbSide + 30.dp)
                    .clickable(
                        interactionSource = null,
                        indication = null,
                        onClickLabel = if (liked) "Remove like" else "Like",
                        role = Role.Button,
                    ) { toggle() },
            )
        }
    }
}

/**
 * Writes the Material "thumb_up" glyph (cuff and hand) into [cuff] and [hand], scaled to fill the square
 * at [topLeft] with side [side].
 */
private fun thumbPaths(topLeft: Offset, side: Float, cuff: Path, hand: Path) {
    val k = side / 22f
    val dy = (side - 20f * k) / 2
    fun x(v: Float) = topLeft.x + (v - 1f) * k
    fun y(v: Float) = topLeft.y + dy + (v - 1f) * k
    cuff.reset()
    cuff.moveTo(x(1f), y(9f))
    cuff.lineTo(x(5f), y(9f))
    cuff.lineTo(x(5f), y(21f))
    cuff.lineTo(x(1f), y(21f))
    cuff.close()
    hand.reset()
    hand.moveTo(x(23f), y(10f))
    hand.cubicTo(x(23f), y(8.9f), x(22.1f), y(8f), x(21f), y(8f))
    hand.lineTo(x(14.69f), y(8f))
    hand.lineTo(x(15.64f), y(3.43f))
    hand.lineTo(x(15.67f), y(3.11f))
    hand.cubicTo(x(15.67f), y(2.7f), x(15.5f), y(2.32f), x(15.23f), y(2.05f))
    hand.lineTo(x(14.17f), y(1f))
    hand.lineTo(x(7.59f), y(7.59f))
    hand.cubicTo(x(7.22f), y(7.95f), x(7f), y(8.45f), x(7f), y(9f))
    hand.lineTo(x(7f), y(19f))
    hand.cubicTo(x(7f), y(20.1f), x(7.9f), y(21f), x(9f), y(21f))
    hand.lineTo(x(18f), y(21f))
    hand.cubicTo(x(18.83f), y(21f), x(19.54f), y(20.5f), x(19.84f), y(19.78f))
    hand.lineTo(x(22.86f), y(12.73f))
    hand.cubicTo(x(22.95f), y(12.5f), x(23f), y(12.26f), x(23f), y(12f))
    hand.close()
}
