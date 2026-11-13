package com.azersun.uitemplates.demos.error1

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.azersun.uitemplates.demos.menus.MenuColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal object ErrorColors {
    val Red = MenuColors.Red
    val Green = Color(0xFF34D399)
    val Field = Color(0xFF15132C)
    val FieldBorder = Color(0xFF34305F)
    val Outline = Color(0xFF625D9E)
    val Track = Color(0xFF2E2A5C)
    val Text = MenuColors.Text
    val Muted = MenuColors.Muted
    val Violet = MenuColors.Violet
    val Blue = MenuColors.Blue
    val Cyan = MenuColors.Cyan
    val Tile = MenuColors.Tile
    val TileLight = MenuColors.TileLight
}

/** A side-to-side shake that dies away, in whatever unit the caller reads the value in. */
internal suspend fun Animatable<Float, AnimationVector1D>.shake(amplitude: Float, swings: Int = 6, stepMillis: Int = 45) {
    var reach = amplitude
    repeat(swings) { i ->
        animateTo(if (i % 2 == 0) -reach else reach, tween(stepMillis))
        reach *= 0.7f
    }
    animateTo(0f, tween(stepMillis))
}

/** A filled disc with a "!" cut out of it in [mark]. */
internal fun DrawScope.drawAlertBadge(color: Color = ErrorColors.Red, mark: Color = Color.White) {
    val r = size.minDimension / 2
    drawCircle(color, r, center)
    val width = r * 0.24f
    drawLine(mark, Offset(center.x, center.y - r * 0.5f), Offset(center.x, center.y + r * 0.1f), width, StrokeCap.Round)
    drawCircle(mark, width * 0.65f, Offset(center.x, center.y + r * 0.48f))
}

/** A filled disc with a white ✓. */
internal fun DrawScope.drawDoneBadge(color: Color) {
    val r = size.minDimension / 2
    drawCircle(color, r, center)
    val width = r * 0.22f
    val vertex = Offset(center.x - r * 0.1f, center.y + r * 0.32f)
    drawLine(Color.White, Offset(center.x - r * 0.45f, center.y), vertex, width, StrokeCap.Round)
    drawLine(Color.White, vertex, Offset(center.x + r * 0.45f, center.y - r * 0.32f), width, StrokeCap.Round)
}

/** A ↻: an open circle with an arrowhead on its end. */
internal fun DrawScope.drawRetryGlyph(color: Color) {
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

/** An arc chasing round a faint ring. Only compose it while something is loading: it ticks every frame. */
@Composable
internal fun Spinner(color: Color, modifier: Modifier = Modifier) {
    val angle by rememberInfiniteTransition(label = "spinner").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing)),
        label = "spinnerAngle",
    )
    Spacer(
        modifier.drawWithCache {
            val stroke = Stroke(size.minDimension * 0.14f, cap = StrokeCap.Round)
            val inset = stroke.width / 2
            val topLeft = Offset(inset, inset)
            val arcSize = Size(size.width - stroke.width, size.height - stroke.width)
            onDrawBehind {
                drawArc(color, 0f, 360f, false, topLeft, arcSize, alpha = 0.25f, style = stroke)
                drawArc(color, angle - 90f, 100f, false, topLeft, arcSize, style = stroke)
            }
        },
    )
}
