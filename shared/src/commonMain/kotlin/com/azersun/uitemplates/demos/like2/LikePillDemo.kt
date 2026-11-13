package com.azersun.uitemplates.demos.like2

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azersun.uitemplates.demos.like.LikeColors
import com.azersun.uitemplates.demos.like.LikeStage
import com.azersun.uitemplates.demos.like.drawLikeHeart
import com.azersun.uitemplates.demos.like.heart
import com.azersun.uitemplates.demos.like.pop
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.sqrt

private val PillShape = RoundedCornerShape(50)
private val PillPadStart = 22.dp
private val HeartSize = 20.dp
private val White = SolidColor(Color.White)

@Composable
fun LikePillDemo() {
    var liked by remember { mutableStateOf(false) }
    // Where the last tap landed in the pill; unspecified means the heart, which is where autoplay "taps".
    var origin by remember { mutableStateOf(Offset.Unspecified) }
    val spread = remember { Animatable(0f) }
    val heartFill = remember { Animatable(0f) }
    val heartScale = remember { Animatable(1f) }
    val press = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val content by animateColorAsState(if (liked) Color.White else LikeColors.Text, tween(300), label = "content")

    fun toggle(at: Offset) {
        origin = at
        liked = !liked
        scope.launch {
            spread.animateTo(if (liked) 1f else 0f, tween(if (liked) 520 else 380, easing = FastOutSlowInEasing))
        }
        scope.launch { heartFill.animateTo(if (liked) 1f else 0f, tween(220)) }
        if (liked) scope.launch { heartScale.pop() }
        scope.launch {
            press.animateTo(0.94f, tween(80))
            press.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 600f))
        }
    }

    LikeStage(
        5,
        "Like Pill",
        "Tap: a gradient spreads from the touch; \"Like\" morphs into \"Liked\".",
        autoplay = { toggle(Offset.Unspecified) },
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                Modifier
                    .graphicsLayer {
                        scaleX = press.value
                        scaleY = press.value
                    }
                    .clip(PillShape)
                    .drawWithCache {
                        val brush = Brush.horizontalGradient(listOf(LikeColors.Pink, LikeColors.Violet), 0f, size.width)
                        onDrawBehind {
                            val s = spread.value
                            if (s > 0f) {
                                val o = if (origin.isSpecified) origin else Offset((PillPadStart + HeartSize / 2).toPx(), size.height / 2)
                                // Far enough to reach the pill's farthest corner from the touch.
                                val reach = sqrt(max(o.x, size.width - o.x).let { it * it } + max(o.y, size.height - o.y).let { it * it })
                                drawCircle(brush, reach * s, o)
                            }
                        }
                    }
                    .border(1.5.dp, LikeColors.Outline.copy(alpha = 0.6f), PillShape)
                    .pointerInput(Unit) { detectTapGestures(onTap = ::toggle) }
                    .padding(start = PillPadStart, end = 26.dp, top = 14.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(
                    Modifier
                        .size(HeartSize)
                        .graphicsLayer {
                            scaleX = heartScale.value
                            scaleY = heartScale.value
                        }
                        .drawWithCache {
                            val heart = Path().apply { heart(Offset.Zero, size.width) }
                            val outline = 2.dp.toPx()
                            onDrawBehind { drawLikeHeart(heart, heartFill.value, White, content, outline) }
                        },
                )
                Spacer(Modifier.width(10.dp))
                AnimatedContent(
                    targetState = liked,
                    transitionSpec = {
                        (fadeIn(tween(200, delayMillis = 80)) + slideInVertically(tween(260)) { it / 2 }) togetherWith
                            (fadeOut(tween(120)) + slideOutVertically(tween(200)) { -it / 2 }) using
                            SizeTransform(clip = false)
                    },
                    label = "label",
                ) { isLiked ->
                    Text(
                        if (isLiked) "Liked" else "Like",
                        color = content,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Text(
                "Tap anywhere on the pill",
                color = LikeColors.Outline,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 22.dp),
            )
        }
    }
}
