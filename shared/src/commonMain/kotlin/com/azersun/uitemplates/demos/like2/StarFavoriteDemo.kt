package com.azersun.uitemplates.demos.like2

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.azersun.uitemplates.demos.like.LikeColors
import com.azersun.uitemplates.demos.like.LikeStage
import com.azersun.uitemplates.demos.like.drawLikeHeart
import com.azersun.uitemplates.demos.like.shrink
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val StarRadius = 54.dp
private const val SparkleCount = 7
private const val TwinkleMillis = 1500
private val Gold = Color(0xFFFFC53D)
private val SparkleColors = listOf(Gold, LikeColors.Pink, Color.White)

@Composable
fun StarFavoriteDemo() {
    var favorite by remember { mutableStateOf(false) }
    val spin = remember { Animatable(0f) }
    val scale = remember { Animatable(1f) }
    val fill = remember { Animatable(0f) }
    // Runs 0 → 1 per favourite; the sparkles are hidden at 1.
    val twinkle = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    fun toggle() {
        favorite = !favorite
        if (favorite) {
            scope.launch {
                spin.snapTo(0f)
                spin.animateTo(360f, tween(650, easing = FastOutSlowInEasing))
            }
            scope.launch {
                scale.animateTo(1.2f, tween(260, easing = FastOutSlowInEasing))
                scale.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 420f))
            }
            scope.launch {
                delay(140)
                fill.animateTo(1f, tween(380))
            }
            scope.launch {
                delay(420)
                twinkle.snapTo(0f)
                twinkle.animateTo(1f, tween(TwinkleMillis, easing = LinearEasing))
            }
        } else {
            scope.launch { fill.animateTo(0f, tween(200)) }
            scope.launch { scale.shrink() }
            scope.launch { twinkle.snapTo(1f) }
        }
    }

    LikeStage(
        2,
        "Star Favorite",
        "Tap: the star spins once, fills gold→pink, and sparkles twinkle.",
        autoplay = { toggle() },
    ) {
        Box(
            Modifier
                .size(280.dp)
                .drawWithCache {
                    val outer = StarRadius.toPx()
                    val star = Path().apply { star(size.center, outer, outer * 0.5f, outer * 0.1f) }
                    val sparkle = Path()
                    val brush = Brush.linearGradient(
                        listOf(Gold, Color(0xFFFF8A5C), LikeColors.Pink),
                        size.center - Offset(outer, outer),
                        size.center + Offset(outer, outer),
                    )
                    val outline = 3.5.dp.toPx()
                    onDrawBehind {
                        val t = twinkle.value
                        val f = fill.value
                        val s = scale.value
                        val r = spin.value
                        if (t < 1f) {
                            repeat(SparkleCount) { i ->
                                // Each sparkle blinks once in its own window, so they light up one after another.
                                val local = ((t - i * 0.075f) / 0.45f).coerceIn(0f, 1f)
                                val blink = sin(local * PI.toFloat())
                                if (blink > 0f) {
                                    val angle = (i * 360f / SparkleCount - 75f) * PI.toFloat() / 180f
                                    val distance = outer * if (i % 2 == 0) 1.75f else 1.45f
                                    val at = center + Offset(cos(angle), sin(angle)) * distance
                                    val radius = outer * (if (i % 2 == 0) 0.2f else 0.13f) * blink
                                    rotate(local * 90f, at) {
                                        sparkle.sparkle(at, radius)
                                        drawPath(sparkle, SparkleColors[i % SparkleColors.size], alpha = blink)
                                    }
                                }
                            }
                        }
                        rotate(r, center) {
                            scale(s, center) { drawLikeHeart(star, f, brush, LikeColors.Outline, outline) }
                        }
                    }
                },
        ) {
            Box(
                Modifier
                    .size(StarRadius * 2 + 24.dp)
                    .align(Alignment.Center)
                    .clickable(
                        interactionSource = null,
                        indication = null,
                        onClickLabel = if (favorite) "Remove favourite" else "Add favourite",
                        role = Role.Button,
                        onClick = ::toggle,
                    ),
            )
        }
    }
}

/** Replaces this path with a four-point twinkle whose sides curve in towards [center]. */
private fun Path.sparkle(center: Offset, radius: Float) {
    val pinch = radius * 0.16f
    reset()
    moveTo(center.x, center.y - radius)
    quadraticTo(center.x + pinch, center.y - pinch, center.x + radius, center.y)
    quadraticTo(center.x + pinch, center.y + pinch, center.x, center.y + radius)
    quadraticTo(center.x - pinch, center.y + pinch, center.x - radius, center.y)
    quadraticTo(center.x - pinch, center.y - pinch, center.x, center.y - radius)
    close()
}
