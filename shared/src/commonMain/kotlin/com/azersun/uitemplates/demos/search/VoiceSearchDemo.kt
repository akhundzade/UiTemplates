package com.azersun.uitemplates.demos.search

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azersun.uitemplates.demos.menus.Glyph
import com.azersun.uitemplates.demos.menus.GlyphIcon
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

private val Spoken = listOf("best", "pizza", "near", "me")
private const val BarCount = 30
private const val FirstWordMillis = 650L
private const val WordGapMillis = 400L
private const val TrailingSilenceMillis = 850L

@Composable
fun VoiceSearchDemo() {
    val autoplay = rememberAutoplay()
    val query = remember { Query() }
    var listening by remember { mutableStateOf(false) }
    var heard by remember { mutableIntStateOf(0) }
    val level = remember { Animatable(0f) }
    var seconds by remember { mutableFloatStateOf(0f) }

    fun commit() {
        listening = false
        if (heard > 0) query.set(Spoken.take(heard).joinToString(" "))
    }

    fun toggleMic() {
        if (listening) {
            commit()
        } else {
            query.set("")
            heard = 0
            listening = true
        }
    }

    LaunchedEffect(listening) {
        if (!listening) return@LaunchedEffect
        delay(FirstWordMillis)
        for (i in Spoken.indices) {
            heard = i + 1
            delay(WordGapMillis)
        }
        delay(TrailingSilenceMillis)
        commit()
    }
    // The wave clock only ticks while the bars are moving, so an idle demo draws nothing per frame.
    LaunchedEffect(listening) {
        coroutineScope {
            launch { level.animateTo(if (listening) 1f else 0f, tween(if (listening) 320 else 480)) }
            while (listening || level.value > 0f) {
                withFrameNanos { seconds = (it % 1_000_000_000_000L) / 1_000_000_000f }
            }
        }
    }

    AutoplayEffect(autoplay) {
        listening = false
        heard = 0
        query.set("")
        delay(AutoplayDelayMillis)
        toggleMic()
    }

    SearchStage(4, "Voice Search", "Tap the mic: rings pulse, the wave dances, words pop in.", autoplay) {
        Column(
            Modifier
                .align(Alignment.TopCenter)
                .padding(top = 96.dp, start = 20.dp, end = 20.dp)
                .widthIn(max = 380.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SearchField(
                query = query,
                placeholder = when {
                    !listening -> "Search or say…"
                    heard == 0 -> "Listening…"
                    else -> ""
                },
                glowing = listening,
                trailing = { MicButton(listening, onClick = ::toggleMic) },
                textOverlay = {
                    if (listening && heard > 0) {
                        Row(Modifier.align(Alignment.CenterStart)) {
                            for (i in 0 until heard) {
                                key(i) { PopWord("${Spoken[i]} ") }
                            }
                        }
                    }
                },
            )
            Spacer(Modifier.height(36.dp))
            Canvas(Modifier.fillMaxWidth().height(64.dp)) {
                val t = seconds
                val l = level.value
                val slot = size.width / BarCount
                val barWidth = slot * 0.46f
                val minHeight = 4.dp.toPx()
                val radius = CornerRadius(barWidth / 2)
                for (i in 0 until BarCount) {
                    val envelope = 0.3f + 0.7f * sin(PI.toFloat() * (i + 0.5f) / BarCount)
                    val wave = abs(sin(t * 7.3f + i * 0.55f)) * 0.6f + abs(sin(t * 4.1f - i * 0.33f + 1.7f)) * 0.4f
                    val h = minHeight + (size.height - minHeight) * l * envelope * wave
                    val tint = lerp(SearchColors.Violet, SearchColors.Cyan, i / (BarCount - 1f))
                    drawRoundRect(
                        lerp(SearchColors.Muted.copy(alpha = 0.35f), tint, l),
                        Offset(i * slot + (slot - barWidth) / 2, (size.height - h) / 2),
                        Size(barWidth, h),
                        radius,
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                if (listening) "Listening…" else "Tap the mic to search by voice",
                color = SearchColors.Muted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PopWord(word: String) {
    val pop = remember { Animatable(0f) }
    LaunchedEffect(Unit) { pop.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 520f)) }
    Text(
        word,
        Modifier.graphicsLayer {
            val p = pop.value
            transformOrigin = TransformOrigin(0.3f, 0.8f)
            scaleX = 0.4f + 0.6f * p
            scaleY = 0.4f + 0.6f * p
            alpha = p.coerceIn(0f, 1f)
        },
        style = QueryStyle,
        softWrap = false,
    )
}

@Composable
private fun MicButton(listening: Boolean, onClick: () -> Unit) {
    val background by animateColorAsState(
        if (listening) SearchColors.Blue else SearchColors.Row,
        tween(220),
        label = "mic",
    )
    Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
        if (listening) {
            val ring by rememberInfiniteTransition(label = "rings").animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing)),
                label = "ring",
            )
            Box(
                Modifier
                    .size(36.dp)
                    .drawBehind {
                        repeat(2) { k ->
                            val p = (ring + k * 0.5f) % 1f
                            drawCircle(
                                SearchColors.Blue.copy(alpha = 0.32f * (1f - p)),
                                radius = size.minDimension / 2 + p * 22.dp.toPx(),
                            )
                        }
                    },
            )
        }
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(background)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            GlyphIcon(Glyph.Mic, if (listening) Color.White else SearchColors.Muted, size = 18.dp)
        }
    }
}
