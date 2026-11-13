package com.azersun.uitemplates.demos.fab2

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azersun.uitemplates.demos.menus.Glyph
import com.azersun.uitemplates.demos.menus.GlyphIcon
import com.azersun.uitemplates.demos.menus.MenuColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val RecordScale = 1.25f
private const val WavePeriodMillis = 1500f
private const val AutoplayHoldMillis = 2500L
private val RecordPink = Brush.linearGradient(listOf(MenuColors.Pink, Color(0xFFFF8FB8)))
private val RecordRed = Color(0xFFFF4D6D)

// Tabular figures keep the timer from shifting as its digits change.
private val TimerStyle = TextStyle(
    color = MenuColors.Text,
    fontSize = 13.sp,
    fontWeight = FontWeight.SemiBold,
    fontFeatureSettings = "tnum",
)

@Composable
fun HoldToRecordFabDemo() {
    val autoplay = rememberAutoplay()
    // Kept apart so cancelling the autoplay never releases a finger that is really holding.
    var userHold by remember { mutableStateOf(false) }
    var autoHold by remember { mutableStateOf(false) }
    val recording = userHold || autoHold

    val grow by animateFloatAsState(if (recording) 1f else 0f, spring(dampingRatio = 0.5f, stiffness = 420f), label = "grow")
    val pink by animateFloatAsState(if (recording) 1f else 0f, tween(240), label = "pink")
    val pill by animateFloatAsState(if (recording) 1f else 0f, spring(dampingRatio = 0.75f, stiffness = 420f), label = "pill")
    val waves = remember { Animatable(0f) }
    var wavePhase by remember { mutableFloatStateOf(0f) }
    var elapsed by remember { mutableLongStateOf(0L) }
    val seconds by remember { derivedStateOf { (elapsed / 1000).toInt() } }

    LaunchedEffect(recording) {
        if (recording) {
            elapsed = 0L
            waves.snapTo(1f)
        } else {
            launch { waves.animateTo(0f, tween(450)) }
        }
        var start = -1L
        var last = -1L
        // Keeps ticking after release until the waves have faded, so they don't freeze mid-pulse.
        while (recording || waves.value > 0f) {
            withFrameMillis { now ->
                if (start < 0) start = now
                if (last >= 0) wavePhase += (now - last) / WavePeriodMillis
                last = now
                if (recording) elapsed = now - start
            }
        }
    }

    AutoplayEffect(autoplay) {
        try {
            autoHold = true
            delay(AutoplayHoldMillis)
        } finally {
            autoHold = false
        }
    }

    NotesScene(
        2,
        "Hold to Record",
        "Hold the mic: it grows and turns pink, waves pulse out and a timer counts. Let go to stop.",
        autoplay,
    ) {
        Column(Modifier.fillMaxSize()) {
            NotesHeader()
            NoteList()
        }
        Row(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(end = FabInset, bottom = FabInset),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TimerPill(
                seconds = seconds,
                elapsed = { elapsed },
                modifier = Modifier.graphicsLayer {
                    alpha = pill.coerceIn(0f, 1f)
                    val s = 0.8f + 0.2f * pill
                    scaleX = s
                    scaleY = s
                    translationX = (1f - pill) * 24.dp.toPx()
                    transformOrigin = TransformOrigin(1f, 0.5f)
                },
            )
            Spacer(Modifier.width(22.dp))
            Box(
                Modifier
                    .size(FabSize)
                    .drawWithCache {
                        val ring = Stroke(1.5.dp.toPx())
                        onDrawBehind {
                            val fade = waves.value
                            val phase = wavePhase
                            val base = size.minDimension / 2 * (1f + (RecordScale - 1f) * grow)
                            if (fade > 0f) {
                                repeat(3) { i ->
                                    val f = (phase + i / 3f) % 1f
                                    val radius = base * (1f + 0.95f * f)
                                    val a = (1f - f) * fade
                                    drawCircle(MenuColors.Pink, radius, alpha = 0.16f * a)
                                    drawCircle(MenuColors.Pink, radius, alpha = 0.5f * a, style = ring)
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Fab(
                    Modifier
                        .graphicsLayer {
                            val s = 1f + (RecordScale - 1f) * grow
                            scaleX = s
                            scaleY = s
                        }
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                awaitFirstDown().consume()
                                try {
                                    userHold = true
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        event.changes.forEach { it.consume() }
                                        if (event.changes.none { it.pressed }) break
                                    }
                                } finally {
                                    userHold = false
                                }
                            }
                        },
                    glow = MenuColors.Pink,
                    paint = {
                        drawRect(FabGradient)
                        if (pink > 0f) drawRect(RecordPink, alpha = pink)
                    },
                ) {
                    GlyphIcon(Glyph.Mic, Color.White, size = 24.dp)
                }
            }
        }
    }
}

@Composable
private fun TimerPill(seconds: Int, elapsed: () -> Long, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(CircleShape)
            .background(Color(0xFF241F45))
            .border(1.dp, MenuColors.Pink.copy(alpha = 0.4f), CircleShape)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(8.dp)) {
            val on = (elapsed() / 500) % 2 == 0L
            drawCircle(RecordRed, alpha = if (on) 1f else 0.3f)
        }
        Spacer(Modifier.width(7.dp))
        Text("${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}", style = TimerStyle)
    }
}
