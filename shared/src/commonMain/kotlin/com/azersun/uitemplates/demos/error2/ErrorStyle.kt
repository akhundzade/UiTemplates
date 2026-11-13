package com.azersun.uitemplates.demos.error2

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

internal object ErrorColors {
    val Card = MenuColors.Card
    val Border = MenuColors.CardBorder
    val Tile = MenuColors.Tile
    val Text = MenuColors.Text
    val Muted = MenuColors.Muted
    val Blue = MenuColors.Blue
    val Violet = MenuColors.Violet
    val Cyan = MenuColors.Cyan
    val Red = MenuColors.Red
    val DeepRed = Color(0xFFB4233F)
}

private const val AutoplayDelayMillis = 700L

/**
 * The shared stage: runs [autoplay] 700 ms after opening unless a touch lands first, and shows a Replay
 * pill when [onReplay] is given (the pill resets the demo and runs the autoplay again).
 */
@Composable
internal fun ErrorStage(
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

/** A decaying side-to-side offset for a shake running 0 → 1, in units of its peak. */
internal fun shakeOffset(progress: Float): Float =
    if (progress <= 0f || progress >= 1f) 0f else sin(progress * PI.toFloat() * 6f) * (1f - progress)

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
        Canvas(Modifier.size(12.dp)) { drawReplayGlyph(ErrorColors.Text) }
        Spacer(Modifier.width(6.dp))
        Text("Replay", color = ErrorColors.Text, fontSize = 12.sp, fontWeight = FontWeight.Medium)
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
