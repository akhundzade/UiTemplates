package com.azersun.uitemplates.demos.button2

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
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

internal val Orange = Color(0xFFFF9F43)

private const val AutoplayDelayMillis = 700L

/**
 * The shared stage: runs [autoplay] 700 ms after opening unless a touch lands first (a touch also
 * cancels it mid-way), and shows a Replay pill when [onReplay] is given, which resets the demo and
 * runs the autoplay again.
 */
@Composable
internal fun ButtonStage(
    number: Int,
    title: String,
    caption: String,
    onReplay: (() -> Unit)? = null,
    autoplay: (suspend () -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    var interacted by remember { mutableStateOf(false) }
    var runs by remember { mutableIntStateOf(0) }
    val currentAutoplay by rememberUpdatedState(autoplay)
    LaunchedEffect(runs, interacted) {
        if (interacted) return@LaunchedEffect
        delay(AutoplayDelayMillis)
        // Read after the delay so the script sees sizes measured in the meantime.
        currentAutoplay?.invoke()
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

internal fun mix(start: Float, stop: Float, fraction: Float): Float = start + (stop - start) * fraction

internal fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
    val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

/** A ✓ in the square of side [side] at [topLeft], drawn up to [progress] of its length. */
internal fun DrawScope.drawCheck(progress: Float, color: Color, topLeft: Offset, side: Float, strokeWidth: Float) {
    if (progress <= 0f) return
    val a = topLeft + Offset(side * 0.22f, side * 0.53f)
    val b = topLeft + Offset(side * 0.41f, side * 0.72f)
    val c = topLeft + Offset(side * 0.79f, side * 0.31f)
    val first = (b - a).getDistance()
    val second = (c - b).getDistance()
    val drawn = (first + second) * progress.coerceAtMost(1f)
    if (drawn <= first) {
        drawLine(color, a, a + (b - a) * (drawn / first), strokeWidth, StrokeCap.Round)
    } else {
        drawLine(color, a, b, strokeWidth, StrokeCap.Round)
        drawLine(color, b, b + (c - b) * ((drawn - first) / second), strokeWidth, StrokeCap.Round)
    }
}

@Composable
private fun ReplayPill(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.08f))
            .clickable(onClickLabel = "Replay", onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(12.dp)) { drawReplayGlyph(MenuColors.Text) }
        Spacer(Modifier.width(6.dp))
        Text("Replay", color = MenuColors.Text, fontSize = 12.sp, fontWeight = FontWeight.Medium)
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
