package com.azersun.uitemplates.demos.like2

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azersun.uitemplates.demos.like.LikeColors
import com.azersun.uitemplates.demos.like.LikeStage
import com.azersun.uitemplates.demos.like.drawLikeHeart
import com.azersun.uitemplates.demos.like.heart
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val HeartSide = 120.dp
private const val SquashX = 0.15f
private const val SquashY = 0.2f

@Composable
fun JellySquishDemo() {
    var liked by remember { mutableStateOf(false) }
    // 1 is fully squashed; the release spring overshoots below 0, which stretches the heart tall.
    val squash = remember { Animatable(0f) }
    val fill = remember { Animatable(0f) }
    val glow = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    fun press() {
        scope.launch { squash.animateTo(1f, spring(dampingRatio = 0.75f, stiffness = 700f)) }
    }

    fun release(commit: Boolean) {
        if (commit) {
            liked = !liked
            scope.launch { fill.animateTo(if (liked) 1f else 0f, tween(200)) }
            scope.launch { glow.animateTo(if (liked) 1f else 0f, tween(420)) }
        }
        scope.launch {
            squash.animateTo(0f, if (commit) spring(dampingRatio = 0.22f, stiffness = 320f) else spring(dampingRatio = 0.6f, stiffness = 500f))
        }
    }

    LikeStage(
        1,
        "Jelly Squish",
        "Press squashes the heart; on release it fills and jiggles like jelly.",
        autoplay = {
            press()
            delay(420)
            release(commit = true)
        },
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(240.dp)
                    .drawBehind {
                        val g = glow.value
                        if (g > 0f) {
                            drawCircle(
                                Brush.radialGradient(
                                    listOf(LikeColors.Pink.copy(alpha = 0.45f), Color.Transparent),
                                    center = center,
                                    radius = size.minDimension / 2,
                                ),
                                alpha = g,
                            )
                        }
                    }
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown()
                            press()
                            release(commit = waitForUpOrCancellation() != null)
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Spacer(
                    Modifier
                        .size(HeartSide)
                        .graphicsLayer {
                            val s = squash.value
                            scaleX = 1f + SquashX * s
                            scaleY = 1f - SquashY * s
                            transformOrigin = TransformOrigin(0.5f, 0.92f)
                        }
                        .drawWithCache {
                            val side = size.minDimension
                            val heart = Path().apply { heart(size.center - Offset(side / 2, side / 2), side) }
                            val brush = Brush.linearGradient(listOf(LikeColors.Pink, LikeColors.Violet), Offset.Zero, Offset(side, side))
                            val outline = 4.dp.toPx()
                            onDrawBehind { drawLikeHeart(heart, fill.value, brush, LikeColors.Outline, outline) }
                        },
                )
            }
            Text(
                if (liked) "Liked" else "Press the heart",
                color = if (liked) LikeColors.Pink else LikeColors.Outline,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
