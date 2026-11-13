package com.azersun.uitemplates.demos.like

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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

internal object LikeColors {
    val Pink = MenuColors.Pink
    val Violet = MenuColors.Violet
    val Cyan = MenuColors.Cyan
    val Yellow = MenuColors.Yellow
    val Outline = MenuColors.Muted
    val Text = MenuColors.Text
}

internal val SparkColors = listOf(LikeColors.Pink, LikeColors.Violet, LikeColors.Cyan, LikeColors.Yellow)

private const val AutoplayDelayMillis = 700L

/**
 * The shared stage: runs [autoplay] 700 ms after opening unless a touch lands first, and shows a Replay
 * pill when [onReplay] is given (the pill resets the demo and runs the autoplay again).
 */
@Composable
internal fun LikeStage(
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

/** The like pop: dip to 0.8, overshoot to 1.25, then settle on a spring. */
internal suspend fun Animatable<Float, AnimationVector1D>.pop() {
    animateTo(0.8f, tween(90))
    animateTo(1.25f, tween(150, easing = FastOutSlowInEasing))
    animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 500f))
}

/** The unlike: a small shrink and back. */
internal suspend fun Animatable<Float, AnimationVector1D>.shrink() {
    animateTo(0.85f, tween(110))
    animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 500f))
}

/**
 * Replaces this path with a heart (the Material "favorite" outline) filling the square at [topLeft]
 * with side [side], centred vertically.
 */
internal fun Path.heart(topLeft: Offset, side: Float) {
    val k = side / 20f
    val dy = (side - 18.35f * k) / 2
    fun x(v: Float) = topLeft.x + (v - 2f) * k
    fun y(v: Float) = topLeft.y + dy + (v - 3f) * k
    reset()
    moveTo(x(12f), y(21.35f))
    lineTo(x(10.55f), y(20.03f))
    cubicTo(x(5.4f), y(15.36f), x(2f), y(12.28f), x(2f), y(8.5f))
    cubicTo(x(2f), y(5.42f), x(4.42f), y(3f), x(7.5f), y(3f))
    cubicTo(x(9.24f), y(3f), x(10.91f), y(3.81f), x(12f), y(5.09f))
    cubicTo(x(13.09f), y(3.81f), x(14.76f), y(3f), x(16.5f), y(3f))
    cubicTo(x(19.58f), y(3f), x(22f), y(5.42f), x(22f), y(8.5f))
    cubicTo(x(22f), y(12.28f), x(18.6f), y(15.36f), x(13.45f), y(20.04f))
    close()
}

/** A heart that cross-fades from an [outline] stroke to a [brush] fill as [fill] goes 0 → 1. */
internal fun DrawScope.drawLikeHeart(path: Path, fill: Float, brush: Brush, outline: Color, strokeWidth: Float) {
    if (fill < 1f) {
        drawPath(path, outline, alpha = 1f - fill, style = Stroke(strokeWidth, join = StrokeJoin.Round))
    }
    if (fill > 0f) drawPath(path, brush, alpha = fill)
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
        Canvas(Modifier.size(12.dp)) { drawReplayGlyph(LikeColors.Text) }
        Spacer(Modifier.width(6.dp))
        Text("Replay", color = LikeColors.Text, fontSize = 12.sp, fontWeight = FontWeight.Medium)
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
