package com.azersun.uitemplates.demos.like

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azersun.uitemplates.demos.menus.MenuColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

private val BigHeartSide = 96.dp
private val FloatRise = 90.dp
private const val HoldMillis = 450L
private const val StarCount = 30
private val CardShape = RoundedCornerShape(22.dp)

private class BigHeart(val at: Offset) {
    val scale = Animatable(0f)
    val rise = Animatable(0f)
}

@Composable
fun DoubleTapHeartDemo() {
    var liked by remember { mutableStateOf(false) }
    val smallScale = remember { Animatable(1f) }
    val smallFill by animateFloatAsState(if (liked) 1f else 0f, tween(180), label = "smallFill")
    val hearts = remember { mutableStateListOf<BigHeart>() }
    var photoSize by remember { mutableStateOf(IntSize.Zero) }
    val scope = rememberCoroutineScope()

    fun setLiked(value: Boolean) {
        if (liked == value) return
        liked = value
        scope.launch { if (value) smallScale.pop() else smallScale.shrink() }
    }

    fun doubleTap(at: Offset) {
        val heart = BigHeart(at)
        hearts += heart
        setLiked(true)
        scope.launch {
            launch { heart.scale.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 380f)) }
            delay(HoldMillis)
            heart.rise.animateTo(1f, tween(550, easing = FastOutLinearInEasing))
            hearts -= heart
        }
    }

    LikeStage(
        2,
        "Double-Tap Heart",
        "Double-tap the photo: a big heart bounces in where you tapped, floats away, and the like fills pink.",
        autoplay = {
            if (photoSize != IntSize.Zero) doubleTap(Offset(photoSize.width * 0.5f, photoSize.height * 0.45f))
        },
    ) {
        Column(
            Modifier
                .padding(horizontal = 24.dp)
                .widthIn(max = 340.dp)
                .fillMaxWidth()
                .clip(CardShape)
                .background(MenuColors.Tile),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .onSizeChanged { photoSize = it }
                    .pointerInput(Unit) { detectTapGestures(onDoubleTap = { doubleTap(it) }) }
                    .duskPhoto()
                    .drawWithCache {
                        val side = BigHeartSide.toPx()
                        val rise = FloatRise.toPx()
                        val shadow = 3.dp.toPx()
                        val path = Path().apply { heart(Offset(-side / 2, -side / 2), side) }
                        onDrawWithContent {
                            drawContent()
                            hearts.forEach { heart ->
                                val r = heart.rise.value
                                val s = heart.scale.value * (1f - 0.2f * r)
                                translate(heart.at.x, heart.at.y - rise * r) {
                                    scale(s, Offset.Zero) {
                                        translate(0f, shadow) { drawPath(path, Color.Black, alpha = 0.22f * (1f - r)) }
                                        drawPath(path, Color.White, alpha = 1f - r)
                                    }
                                }
                            }
                        }
                    },
            )
            Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Spacer(
                    Modifier
                        .size(28.dp)
                        .graphicsLayer {
                            scaleX = smallScale.value
                            scaleY = smallScale.value
                        }
                        .clickable(
                            interactionSource = null,
                            indication = null,
                            onClickLabel = if (liked) "Unlike" else "Like",
                            role = Role.Button,
                        ) { setLiked(!liked) }
                        .drawWithCache {
                            val path = Path().apply { heart(Offset.Zero, size.width) }
                            val pink = SolidColor(LikeColors.Pink)
                            val stroke = 2.dp.toPx()
                            onDrawBehind { drawLikeHeart(path, smallFill, pink, LikeColors.Text, stroke) }
                        },
                )
                Spacer(Modifier.width(10.dp))
                Text("Double-tap the photo", color = LikeColors.Outline, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

/** A purple dusk: gradient sky, stars, a low sun and two mountain ridges. */
private fun Modifier.duskPhoto(): Modifier = drawWithCache {
    val w = size.width
    val h = size.height
    val sky = Brush.verticalGradient(
        0f to Color(0xFF150F38),
        0.45f to Color(0xFF45277A),
        0.75f to Color(0xFFA85590),
        1f to Color(0xFFF29A7A),
    )
    val random = Random(7)
    val stars = FloatArray(StarCount * 3) { i ->
        when (i % 3) {
            0 -> random.nextFloat() * w
            1 -> random.nextFloat() * h * 0.5f
            else -> 0.4f + random.nextFloat() * 0.6f
        }
    }
    val starRadius = 1.2.dp.toPx()
    val sunCenter = Offset(w * 0.64f, h * 0.64f)
    val sunRadius = w * 0.12f
    val sun = Brush.verticalGradient(listOf(Color(0xFFFFE08A), Color(0xFFFF7E8A)), sunCenter.y - sunRadius, sunCenter.y + sunRadius)
    val sunGlow = Brush.radialGradient(listOf(Color(0x66FFB38A), Color.Transparent), sunCenter, sunRadius * 2.6f)
    val back = ridge(w, h, 0f to 0.72f, 0.18f to 0.56f, 0.34f to 0.66f, 0.5f to 0.5f, 0.68f to 0.63f, 0.85f to 0.54f, 1f to 0.66f)
    val front = ridge(w, h, 0f to 0.86f, 0.2f to 0.72f, 0.4f to 0.82f, 0.6f to 0.69f, 0.8f to 0.83f, 1f to 0.75f)
    onDrawBehind {
        drawRect(sky)
        for (i in 0 until StarCount) {
            drawCircle(Color.White, starRadius * stars[i * 3 + 2], Offset(stars[i * 3], stars[i * 3 + 1]), alpha = stars[i * 3 + 2])
        }
        drawCircle(sunGlow, sunRadius * 2.6f, sunCenter)
        drawCircle(sun, sunRadius, sunCenter)
        drawPath(back, Color(0xFF3A2468))
        drawPath(front, Color(0xFF1E1438))
    }
}

private fun ridge(w: Float, h: Float, vararg points: Pair<Float, Float>): Path = Path().apply {
    moveTo(0f, h)
    points.forEach { (x, y) -> lineTo(x * w, y * h) }
    lineTo(w, h)
    close()
}
