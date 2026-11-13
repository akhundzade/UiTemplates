package com.azersun.uitemplates.demos.buttonstate

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azersun.uitemplates.demos.menus.MenuStage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal enum class PlayState(val label: String, colors: List<Color>) {
    Pause("Pause", listOf(Color(0xFFF472B6), Color(0xFF8B5CF6))),
    Resume("Resume", listOf(Color(0xFF22D3EE), Color(0xFF3B82F6))),
    ;

    val start: Color = colors.first()
    val brush: Brush = Brush.horizontalGradient(colors)
    val next: PlayState get() = if (this == Pause) Resume else Pause
}

internal val StartState = PlayState.Pause
internal val PillWidth = 260.dp
internal val PillHeight = 56.dp
internal val Pill = RoundedCornerShape(50)

private val GlyphSize = 15.dp
internal val GlyphGap = 10.dp

private const val AutoplayDelayMillis = 700L
private const val AutoplayGapMillis = 1600L
private const val AutoplayPressMillis = 220L

/** Where the autoplay "finger" lands for each of its taps, as fractions of the pill. */
private val AutoplayTaps = listOf(Offset(0.72f, 0.62f), Offset(0.3f, 0.4f))

/** Whether the pill is held down and where, in the pill's own pixels. */
@Stable
internal class PressState {
    var pressed by mutableStateOf(false)
        private set
    var position by mutableStateOf(Offset.Zero)
    private var token = 0

    fun begin(at: Offset): Int {
        position = at
        pressed = true
        return ++token
    }

    /** Ends the press started with [token], unless a newer press has taken over since. */
    fun end(token: Int) {
        if (token == this.token) pressed = false
    }
}

/**
 * The pause/resume pill all six demos share, centred on the stage. It owns the state, the
 * press tracking and the reel's autoplay (two taps, 1.6 s apart). A tap runs [onToggle] to
 * completion; taps that land while it is still running are ignored, so every transition
 * finishes cleanly. [content] draws the pill inside a [PillWidth] × [PillHeight] box that
 * does not clip, so effects may spill past the pill.
 */
@Composable
internal fun ButtonStateScene(
    number: Int,
    title: String,
    caption: String,
    onToggle: suspend (from: PlayState, to: PlayState, tap: Offset) -> Unit,
    press: PressState = remember { PressState() },
    content: @Composable BoxScope.() -> Unit,
) {
    var state by remember { mutableStateOf(StartState) }
    var busy by remember { mutableStateOf(false) }
    var interacted by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val currentOnToggle by rememberUpdatedState(onToggle)
    val density = LocalDensity.current
    val pill = with(density) { Size(PillWidth.toPx(), PillHeight.toPx()) }

    fun toggle(at: Offset) {
        if (busy) return
        busy = true
        val from = state
        state = from.next
        scope.launch {
            try {
                currentOnToggle(from, from.next, at)
            } finally {
                busy = false
            }
        }
    }

    LaunchedEffect(interacted) {
        if (interacted) return@LaunchedEffect
        delay(AutoplayDelayMillis)
        AutoplayTaps.forEachIndexed { i, fraction ->
            if (i > 0) delay(AutoplayGapMillis - AutoplayPressMillis)
            val at = Offset(pill.width * fraction.x, pill.height * fraction.y)
            val token = press.begin(at)
            try {
                delay(AutoplayPressMillis)
            } finally {
                press.end(token)
            }
            toggle(at)
        }
    }

    MenuStage(number, title, caption) {
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        interacted = true
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            val pressScale by animateFloatAsState(
                if (press.pressed) 0.97f else 1f,
                spring(dampingRatio = 0.6f, stiffness = 700f),
                label = "press",
            )
            Box(
                Modifier
                    .size(PillWidth, PillHeight)
                    .graphicsLayer {
                        scaleX = pressScale
                        scaleY = pressScale
                    }
                    .semantics(mergeDescendants = true) {
                        role = Role.Button
                        onClick {
                            toggle(pill.center)
                            true
                        }
                    }
                    .pointerInput(Unit) { trackPress(press, ::toggle) },
                contentAlignment = Alignment.Center,
                content = content,
            )
        }
    }
}

/** Follows one finger on the pill; a release inside it counts as a tap at the release point. */
private suspend fun PointerInputScope.trackPress(press: PressState, onTap: (Offset) -> Unit) {
    awaitEachGesture {
        val down = awaitFirstDown()
        down.consume()
        val token = press.begin(down.position)
        var released: Offset? = null
        try {
            while (true) {
                val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                if (change.changedToUp()) {
                    change.consume()
                    released = change.position
                    break
                }
                press.position = change.position
            }
        } finally {
            press.end(token)
        }
        val at = released ?: return@awaitEachGesture
        if (at.x in 0f..size.width.toFloat() && at.y in 0f..size.height.toFloat()) onTap(at)
    }
}

/** One state's look: its gradient behind the glyph and label. */
@Composable
internal fun PillFace(state: PlayState, modifier: Modifier = Modifier, shape: Shape = Pill) {
    Box(modifier.fillMaxSize().background(state.brush, shape), contentAlignment = Alignment.Center) {
        StateLabel(state)
    }
}

@Composable
internal fun StateLabel(state: PlayState, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        StateGlyph(state)
        Spacer(Modifier.width(GlyphGap))
        LabelText(state.label)
    }
}

@Composable
internal fun StateGlyph(state: PlayState, modifier: Modifier = Modifier) {
    Canvas(modifier.size(GlyphSize)) { drawStateGlyph(state) }
}

@Composable
internal fun LabelText(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
}

/** ⏸ as two rounded bars, ▶ as a softly rounded triangle. */
private fun DrawScope.drawStateGlyph(state: PlayState) {
    val w = size.width
    val h = size.height
    when (state) {
        PlayState.Pause -> {
            val bar = Size(w * 0.3f, h * 0.9f)
            val top = h * 0.05f
            drawRoundRect(Color.White, Offset(w * 0.12f, top), bar, CornerRadius(w * 0.1f))
            drawRoundRect(Color.White, Offset(w * 0.58f, top), bar, CornerRadius(w * 0.1f))
        }
        PlayState.Resume -> {
            val triangle = Path().apply {
                moveTo(w * 0.2f, h * 0.1f)
                lineTo(w * 0.9f, h * 0.5f)
                lineTo(w * 0.2f, h * 0.9f)
                close()
            }
            drawPath(triangle, Color.White)
            drawPath(triangle, Color.White, style = Stroke(w * 0.14f, join = StrokeJoin.Round))
        }
    }
}

/**
 * The point [distance] along the outline of a [width] × [height] stadium, starting at the
 * middle of its left end and running clockwise (over the top first).
 */
internal fun stadiumPoint(distance: Float, width: Float, height: Float): Offset {
    val r = height / 2
    val straight = width - 2 * r
    val quarter = (PI * r / 2).toFloat()
    val half = (PI * r).toFloat()
    val perimeter = 2 * straight + 2 * half
    var d = ((distance % perimeter) + perimeter) % perimeter
    if (d < quarter) return arcPoint(r, r, r, PI + d / r)
    d -= quarter
    if (d < straight) return Offset(r + d, 0f)
    d -= straight
    if (d < half) return arcPoint(width - r, r, r, 1.5 * PI + d / r)
    d -= half
    if (d < straight) return Offset(width - r - d, height)
    d -= straight
    return arcPoint(r, r, r, 0.5 * PI + d / r)
}

internal fun stadiumPerimeter(width: Float, height: Float): Float =
    2 * (width - height) + (PI * height).toFloat()

private fun arcPoint(cx: Float, cy: Float, r: Float, angle: Double): Offset =
    Offset(cx + r * cos(angle).toFloat(), cy + r * sin(angle).toFloat())

/** Maps [t] onto 0..1 across the window [start]..[end] of a timeline. */
internal fun phase(t: Float, start: Float, end: Float): Float = ((t - start) / (end - start)).coerceIn(0f, 1f)
