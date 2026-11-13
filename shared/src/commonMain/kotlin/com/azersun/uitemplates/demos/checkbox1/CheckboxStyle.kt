package com.azersun.uitemplates.demos.checkbox1

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azersun.uitemplates.demos.menus.MenuColors
import com.azersun.uitemplates.demos.menus.MenuStage
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal object CheckColors {
    val Panel = Color(0xFF221F44)
    val PanelBorder = Color(0xFF332F5E)
    val Outline = Color(0xFF625D9E)
    val Track = Color(0xFF2E2A5C)
    val Text = MenuColors.Text
    val Muted = MenuColors.Muted
    val Blue = MenuColors.Blue
    val Violet = MenuColors.Violet
    val Cyan = MenuColors.Cyan
    val Pink = MenuColors.Pink
    val Yellow = MenuColors.Yellow
    val FillStart = Color(0xFF5B8CFF)
    val FillEnd = Color(0xFF6A5CFF)
}

internal val PanelShape = RoundedCornerShape(18.dp)

private const val AutoplayDelayMillis = 700L

/** Rounded panel behind a row of checkboxes. It does not clip, so bursts and pencils can overflow it. */
internal fun Modifier.checkPanel(): Modifier =
    background(CheckColors.Panel, PanelShape).border(1.dp, CheckColors.PanelBorder, PanelShape)

internal fun mix(from: Float, to: Float, fraction: Float): Float = from + (to - from) * fraction

/** The filled box's gradient, laid over the square at [topLeft]. */
internal fun boxFill(topLeft: Offset, side: Float): Brush =
    Brush.linearGradient(listOf(CheckColors.FillStart, CheckColors.FillEnd), topLeft, topLeft + Offset(side, side))

internal fun checkStroke(side: Float): Stroke = Stroke(side * 0.11f, cap = StrokeCap.Round, join = StrokeJoin.Round)

/** Replaces this path with a ✓ sitting in the square at [topLeft] with side [side]. */
internal fun Path.checkMark(topLeft: Offset, side: Float) {
    reset()
    moveTo(topLeft.x + side * 0.26f, topLeft.y + side * 0.53f)
    lineTo(topLeft.x + side * 0.43f, topLeft.y + side * 0.69f)
    lineTo(topLeft.x + side * 0.75f, topLeft.y + side * 0.35f)
}

/** Trims a fixed path to a sub-range of its length, reusing one output path between frames. */
internal class PathTrim(source: Path) {
    private val measure = PathMeasure().apply { setPath(source, false) }
    private val out = Path()
    val length: Float = measure.length

    fun fraction(start: Float, end: Float): Path = between(start * length, end * length)

    fun between(start: Float, end: Float): Path {
        out.reset()
        if (end > start) measure.getSegment(start, end, out, true)
        return out
    }

    fun position(distance: Float): Offset = measure.getPosition(distance)
}

/** The box itself: an outline that cross-fades into the gradient fill as [fill] goes 0 → 1. */
internal fun DrawScope.drawBoxShell(topLeft: Offset, side: Float, fill: Float, outline: Color = CheckColors.Outline) {
    val radius = side * 0.28f
    if (fill < 1f) {
        val stroke = side * 0.075f
        val inset = stroke / 2
        drawRoundRect(
            outline,
            topLeft + Offset(inset, inset),
            Size(side - stroke, side - stroke),
            CornerRadius(radius - inset),
            style = Stroke(stroke),
            alpha = 1f - fill,
        )
    }
    if (fill > 0f) drawRoundRect(boxFill(topLeft, side), topLeft, Size(side, side), CornerRadius(radius), alpha = fill)
}

/**
 * A three-state box driven by one value: 0 is empty, 1 is a dash and 2 is a ✓. From 0 to 1 the dash
 * grows out of the centre; from 1 to 2 its three points slide so the straight dash bends into the check.
 */
internal fun DrawScope.drawTriStateBox(topLeft: Offset, side: Float, mark: Float, path: Path) {
    val grow = mark.coerceIn(0f, 1f)
    val bend = (mark - 1f).coerceIn(0f, 1f)
    drawBoxShell(topLeft, side, grow)
    if (grow <= 0f) return
    fun point(dashX: Float, dashY: Float, checkX: Float, checkY: Float): Offset {
        val x = mix(dashX, checkX, bend)
        return Offset(topLeft.x + side * (0.5f + (x - 0.5f) * grow), topLeft.y + side * mix(dashY, checkY, bend))
    }
    val a = point(0.28f, 0.5f, 0.26f, 0.53f)
    val b = point(0.5f, 0.5f, 0.43f, 0.69f)
    val c = point(0.72f, 0.5f, 0.75f, 0.35f)
    path.reset()
    path.moveTo(a.x, a.y)
    path.lineTo(b.x, b.y)
    path.lineTo(c.x, c.y)
    drawPath(path, Color.White, style = checkStroke(side))
}

/** A square checkbox whose box fills by [fill] and whose ✓ draws in by [check]; both are read at draw time. */
@Composable
internal fun CheckBoxMark(fill: () -> Float, check: () -> Float, modifier: Modifier = Modifier) {
    Spacer(
        modifier.drawWithCache {
            val side = size.minDimension
            val topLeft = Offset((size.width - side) / 2, (size.height - side) / 2)
            val trim = PathTrim(Path().apply { checkMark(topLeft, side) })
            val stroke = checkStroke(side)
            onDrawBehind {
                val tick = check()
                drawBoxShell(topLeft, side, fill())
                if (tick > 0f) drawPath(trim.fraction(0f, tick), Color.White, style = stroke)
            }
        },
    )
}

/** The checkbox pop: dip, overshoot, then settle on a bouncy spring. */
internal suspend fun Animatable<Float, AnimationVector1D>.checkPop(dip: Float = 0.85f, peak: Float = 1.15f) {
    animateTo(dip, tween(100))
    animateTo(peak, tween(150, easing = FastOutSlowInEasing))
    animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 520f))
}

/**
 * The shared stage: runs [autoplay] 700 ms after opening unless a touch lands first, and shows a Replay
 * pill when [onReplay] is given (the pill resets the demo and runs the autoplay again).
 */
@Composable
internal fun CheckboxStage(
    number: Int,
    title: String,
    caption: String,
    onReplay: (() -> Unit)? = null,
    autoplay: suspend () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    var interacted by remember { mutableStateOf(false) }
    var runs by remember { mutableIntStateOf(0) }
    val currentAutoplay by rememberUpdatedState(autoplay)
    LaunchedEffect(runs, interacted) {
        if (interacted) return@LaunchedEffect
        delay(AutoplayDelayMillis)
        currentAutoplay()
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
            content = content,
        )
        if (onReplay != null) {
            ReplayPill(Modifier.align(Alignment.TopEnd).padding(14.dp)) {
                onReplay()
                interacted = false
                runs++
            }
        }
    }
}

@Composable
private fun ReplayPill(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.08f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(12.dp)) { drawReplayGlyph(CheckColors.Text) }
        Spacer(Modifier.width(6.dp))
        Text("Replay", color = CheckColors.Text, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

private fun DrawScope.drawReplayGlyph(color: Color) {
    val stroke = size.width * 0.14f
    val radius = size.minDimension / 2 - stroke
    drawArc(
        color,
        startAngle = 40f,
        sweepAngle = 290f,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(stroke, cap = StrokeCap.Round),
    )
    val angle = 330f * PI.toFloat() / 180f
    val normal = Offset(cos(angle), sin(angle))
    val tangent = Offset(-normal.y, normal.x)
    val end = center + normal * radius
    val head = Path().apply {
        moveTo(end.x + tangent.x * stroke * 1.6f, end.y + tangent.y * stroke * 1.6f)
        lineTo(end.x + normal.x * stroke * 1.6f, end.y + normal.y * stroke * 1.6f)
        lineTo(end.x - normal.x * stroke * 1.6f, end.y - normal.y * stroke * 1.6f)
        close()
    }
    drawPath(head, color)
}
