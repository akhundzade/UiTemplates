package com.azersun.uitemplates.demos.playbutton

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azersun.uitemplates.demos.menus.MenuColors
import com.azersun.uitemplates.demos.menus.MenuStage
import kotlinx.coroutines.delay

internal val PlayBlue = Color(0xFF3B82F6)
internal val PlayViolet = Color(0xFF8B5CF6)
internal val ButtonSize = 64.dp

private val PlayBrush = Brush.linearGradient(listOf(PlayBlue, PlayViolet))
private val WaveColors = listOf(Color(0xFF38BDF8), PlayBlue, Color(0xFF6366F1))
private val DimBar = Color.White.copy(alpha = 0.16f)
private val CardShape = RoundedCornerShape(24.dp)

private const val AutoplayDelayMillis = 700L
private const val AutoplayPlayMillis = 2500L
private const val TrackSeconds = 24f
private const val StartProgress = 0.18f

/** Static waveform, one height (fraction of the strip) per bar. */
internal val BarHeights = floatArrayOf(
    0.34f, 0.5f, 0.72f, 0.55f, 0.4f, 0.62f, 0.86f, 0.7f, 0.48f, 0.36f, 0.58f, 0.8f, 0.95f, 0.74f,
    0.52f, 0.66f, 0.88f, 0.6f, 0.42f, 0.54f, 0.76f, 0.9f, 0.68f, 0.46f, 0.38f, 0.56f, 0.44f, 0.3f,
)

/** Play triangle cut at x = 0.555 into two quads (corners TL, TR, BR, BL), the tip quad doubling its tip. */
private val PlayQuads = floatArrayOf(
    0.39f, 0.3f, 0.555f, 0.4f, 0.555f, 0.6f, 0.39f, 0.7f,
    0.555f, 0.4f, 0.72f, 0.5f, 0.72f, 0.5f, 0.555f, 0.6f,
)
private val PauseQuads = floatArrayOf(
    0.37f, 0.31f, 0.455f, 0.31f, 0.455f, 0.69f, 0.37f, 0.69f,
    0.545f, 0.31f, 0.63f, 0.31f, 0.63f, 0.69f, 0.545f, 0.69f,
)

@Stable
internal class PlayerState {
    var playing by mutableStateOf(false)
        private set

    /** Counts every tap so per-tap effects fire even when the state flips back. */
    var toggles by mutableIntStateOf(0)
        private set

    /** Playback position, 0..1; read it only while drawing. */
    var progress by mutableFloatStateOf(StartProgress)

    /** Set on the first real touch so the autoplay stops driving the player. */
    var interacted by mutableStateOf(false)

    fun toggle() {
        playing = !playing
        toggles++
    }
}

@Composable
internal fun rememberPlayerState(): PlayerState = remember { PlayerState() }

/**
 * The shared "Paper Comets" player row. It owns playback, progress and the reel's autoplay
 * (play, then pause after 2.5 s). [button] fills the 64 dp tap target and may draw beyond it;
 * [barScale] scales each waveform bar's height while drawing.
 */
@Composable
internal fun PlayerScene(
    state: PlayerState,
    number: Int,
    title: String,
    caption: String,
    barScale: (index: Int) -> Float = { 1f },
    button: @Composable BoxScope.() -> Unit,
) {
    LaunchedEffect(state.interacted) {
        if (state.interacted) return@LaunchedEffect
        delay(AutoplayDelayMillis)
        state.toggle()
        delay(AutoplayPlayMillis)
        state.toggle()
    }
    LaunchedEffect(state.playing) {
        if (!state.playing) return@LaunchedEffect
        var last = withFrameNanos { it }
        while (true) {
            withFrameNanos { now ->
                state.progress = (state.progress + (now - last) / 1e9f / TrackSeconds) % 1f
                last = now
            }
        }
    }

    MenuStage(number, title, caption) {
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        state.interacted = true
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Row(
                Modifier
                    .padding(horizontal = 16.dp)
                    .widthIn(max = 380.dp)
                    .fillMaxWidth()
                    .background(MenuColors.Tile, CardShape)
                    .border(1.dp, MenuColors.CardBorder, CardShape)
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(ButtonSize)
                        .clickable(
                            interactionSource = null,
                            indication = null,
                            onClickLabel = if (state.playing) "Pause" else "Play",
                            role = Role.Button,
                            onClick = state::toggle,
                        ),
                    contentAlignment = Alignment.Center,
                    content = button,
                )
                Spacer(Modifier.width(18.dp))
                Column(Modifier.weight(1f)) {
                    Text("Paper Comets", color = MenuColors.Text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("Nova Lane", color = MenuColors.Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
                    Spacer(Modifier.height(12.dp))
                    Waveform(state, barScale)
                }
            }
        }
    }
}

@Composable
private fun Waveform(state: PlayerState, barScale: (Int) -> Float) {
    Spacer(
        Modifier
            .fillMaxWidth()
            .height(40.dp)
            .drawWithCache {
                val played = Brush.horizontalGradient(WaveColors, 0f, size.width)
                val slot = size.width / BarHeights.size
                val barWidth = slot * 0.5f
                val headWidth = 1.5.dp.toPx()
                val dot = 3.5.dp.toPx()
                // Round caps add half the bar width at each end, so keep that inside the strip.
                val usable = size.height - barWidth
                onDrawBehind {
                    val head = state.progress * size.width
                    for (i in BarHeights.indices) {
                        val x = slot * (i + 0.5f)
                        val h = (BarHeights[i] * barScale(i)).coerceIn(0.06f, 1f) * usable
                        val top = Offset(x, (size.height - h) / 2)
                        val bottom = Offset(x, (size.height + h) / 2)
                        if (x <= head) {
                            drawLine(played, top, bottom, barWidth, StrokeCap.Round)
                        } else {
                            drawLine(DimBar, top, bottom, barWidth, StrokeCap.Round)
                        }
                    }
                    drawLine(Color.White, Offset(head, dot), Offset(head, size.height), headWidth)
                    drawCircle(Color.White, dot, Offset(head, dot))
                }
            },
    )
}

/** The gradient disc, without its glow. */
@Composable
internal fun BoxScope.PlayDisc() {
    Spacer(Modifier.matchParentSize().background(PlayBrush, CircleShape))
}

/** A soft violet pool of light under the disc. */
@Composable
internal fun BoxScope.PlayGlow() {
    Spacer(
        Modifier
            .matchParentSize()
            .drawWithCache {
                val radius = size.minDimension * 0.78f
                val center = Offset(size.width / 2, size.height / 2 + 5.dp.toPx())
                val glow = Brush.radialGradient(listOf(PlayViolet.copy(alpha = 0.45f), Color.Transparent), center, radius)
                onDrawBehind { drawCircle(glow, radius, center) }
            },
    )
}

/** Disc with its glow and a glyph that cross-fades (with a small scale) between play and pause. */
@Composable
internal fun BoxScope.PlayButtonFace(playing: Boolean) {
    PlayGlow()
    PlayDisc()
    PlayPauseCrossfade(playing)
}

@Composable
internal fun BoxScope.PlayPauseCrossfade(playing: Boolean) {
    val pause = animateFloatAsState(if (playing) 1f else 0f, tween(220), label = "glyph")
    Spacer(
        Modifier
            .matchParentSize()
            .drawWithCache {
                val path = Path()
                onDrawBehind {
                    val p = pause.value
                    if (p < 1f) scale(1f - 0.4f * p) { drawPlayPause(path, 0f, alpha = 1f - p) }
                    if (p > 0f) scale(0.6f + 0.4f * p) { drawPlayPause(path, 1f, alpha = p) }
                }
            },
    )
}

/**
 * Draws the glyph [morph] of the way from play (0) to pause (1), each half of the triangle
 * becoming one bar. Values past 1 from a spring overshoot extrapolate, which reads as a bounce.
 * [split] pushes the halves apart horizontally, as a fraction of the glyph box.
 */
internal fun DrawScope.drawPlayPause(
    path: Path,
    morph: Float,
    color: Color = Color.White,
    alpha: Float = 1f,
    split: Float = 0f,
) {
    val w = size.minDimension
    val left = (size.width - w) / 2
    val top = (size.height - w) / 2
    path.reset()
    for (quad in 0..1) {
        val dx = if (quad == 0) -split else split
        for (corner in 0..3) {
            val i = quad * 8 + corner * 2
            val x = left + w * (PlayQuads[i] + (PauseQuads[i] - PlayQuads[i]) * morph + dx)
            val y = top + w * (PlayQuads[i + 1] + (PauseQuads[i + 1] - PlayQuads[i + 1]) * morph)
            if (corner == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
    }
    drawPath(path, color, alpha)
    // A round-joined outline in the same colour softens the corners.
    drawPath(path, color, alpha, style = Stroke(w * 0.05f, join = StrokeJoin.Round))
}
