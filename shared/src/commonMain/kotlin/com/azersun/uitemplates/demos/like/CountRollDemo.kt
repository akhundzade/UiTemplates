package com.azersun.uitemplates.demos.like

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azersun.uitemplates.demos.menus.MenuColors
import kotlinx.coroutines.launch

private const val BaseCount = 1247
private const val RollMillis = 320
private val CountStyle = TextStyle(
    color = LikeColors.Text,
    fontSize = 30.sp,
    fontWeight = FontWeight.Bold,
    fontFeatureSettings = "tnum",
)

@Composable
fun CountRollDemo() {
    var liked by remember { mutableStateOf(false) }
    val heartScale = remember { Animatable(1f) }
    val fill by animateFloatAsState(if (liked) 1f else 0f, tween(160), label = "fill")
    // 0 → 1 per like; hidden at 1.
    val plusOne = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    fun toggle() {
        liked = !liked
        scope.launch { if (liked) heartScale.pop() else heartScale.shrink() }
        if (liked) {
            scope.launch {
                plusOne.snapTo(0f)
                plusOne.animateTo(1f, tween(850, easing = LinearOutSlowInEasing))
            }
        }
    }

    LikeStage(
        3,
        "Count Roll",
        "The heart pops, only the digits that change roll up, and a +1 floats away.",
        autoplay = { toggle() },
    ) {
        Row(
            Modifier
                .clip(RoundedCornerShape(50))
                .background(MenuColors.Tile)
                .clickable(onClickLabel = if (liked) "Unlike" else "Like", role = Role.Button) { toggle() }
                .padding(start = 22.dp, end = 30.dp, top = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(
                Modifier
                    .size(36.dp)
                    .drawWithCache {
                        val path = Path().apply { heart(Offset.Zero, size.width) }
                        val brush = Brush.linearGradient(listOf(LikeColors.Pink, LikeColors.Violet), Offset.Zero, Offset(size.width, size.height))
                        val stroke = 2.5.dp.toPx()
                        onDrawBehind { scale(heartScale.value) { drawLikeHeart(path, fill, brush, LikeColors.Text, stroke) } }
                    },
            )
            Spacer(Modifier.width(14.dp))
            Box {
                RollingNumber(formatThousands(BaseCount + if (liked) 1 else 0), rollUp = liked)
                Text(
                    "+1",
                    color = LikeColors.Pink,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 20.dp, y = (-10).dp)
                        .graphicsLayer {
                            val p = plusOne.value
                            translationY = -p * 26.dp.toPx()
                            alpha = if (p < 0.15f) p / 0.15f else 1f - (p - 0.15f) / 0.85f
                        },
                )
            }
        }
    }
}

/** Each character keeps its slot counted from the right, so only the digits that change roll. */
@Composable
private fun RollingNumber(text: String, rollUp: Boolean) {
    Row {
        text.forEachIndexed { i, char ->
            key(text.length - 1 - i) {
                AnimatedContent(
                    targetState = char,
                    transitionSpec = {
                        val direction = if (rollUp) 1 else -1
                        (slideInVertically(tween(RollMillis, easing = FastOutSlowInEasing)) { it * direction } + fadeIn(tween(RollMillis))) togetherWith
                            (slideOutVertically(tween(RollMillis, easing = FastOutSlowInEasing)) { -it * direction } + fadeOut(tween(RollMillis)))
                    },
                    label = "digit",
                ) { Text(it.toString(), style = CountStyle) }
            }
        }
    }
}

private fun formatThousands(value: Int): String {
    val digits = value.toString()
    return buildString {
        digits.forEachIndexed { i, c ->
            if (i > 0 && (digits.length - i) % 3 == 0) append(',')
            append(c)
        }
    }
}
