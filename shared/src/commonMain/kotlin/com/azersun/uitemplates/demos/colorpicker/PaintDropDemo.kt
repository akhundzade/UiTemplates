package com.azersun.uitemplates.demos.colorpicker

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private const val FlightMillis = 460
private const val SpreadMillis = 620

/** Share of the spread during which the splash droplets fly out. */
private const val SplashShare = 0.6f

private val DropRadius = 6.dp
private val ArcLift = 56.dp

private val SplashAngles = floatArrayOf(-100f, -40f, 5f, 50f, 95f, 140f, 185f, 230f)
private val SplashReach = floatArrayOf(0.30f, 0.22f, 0.34f, 0.24f, 0.28f, 0.2f, 0.33f, 0.25f)
private val SplashSize = floatArrayOf(1f, 0.7f, 0.85f, 0.6f, 0.9f, 0.65f, 0.8f, 0.7f)

@Composable
fun PaintDropDemo() {
    val state = rememberSwatchState()
    var base by remember { mutableStateOf(state.color) }
    var paint by remember { mutableStateOf(state.color) }
    var source by remember { mutableIntStateOf(state.selected) }
    val flight = remember { Animatable(0f) }
    val spread = remember { Animatable(0f) }
    val reveal = remember { Path() }
    val drop = remember { teardrop() }

    LaunchedEffect(state.selected) {
        if (!state.changed) return@LaunchedEffect
        // A tap mid-spread finishes the previous coat at once so the next drop lands on it.
        if (spread.value > 0f) base = paint
        paint = state.color
        source = state.selected
        spread.snapTo(0f)
        flight.snapTo(0f)
        // Linear time along a quadratic Bézier is a parabola at constant horizontal speed: a thrown drop.
        flight.animateTo(1f, tween(FlightMillis, easing = LinearEasing))
        spread.animateTo(1f, tween(SpreadMillis, easing = LinearOutSlowInEasing))
        base = paint
        spread.snapTo(0f)
        flight.snapTo(0f)
    }

    ColorPickerScene(
        state = state,
        number = 3,
        title = "Paint Drop",
        caption = "A drop of paint arcs from the swatch onto the tee and splashes, spreading the colour from where it lands.",
        tee = { art ->
            val s = spread.value
            drawTee(art, base)
            if (s > 0f) {
                val radius = art.size.width * 0.52f * s
                reveal.reset()
                reveal.addOval(Rect(art.impact, radius))
                clipPath(reveal) { drawTee(art, paint) }
                clipPath(art.outline) {
                    drawCircle(lerp(paint, Color.White, 0.4f), radius, art.impact, alpha = 1f - s, style = Stroke(3.dp.toPx()))
                }
                drawSplash(art.impact, art.size.width, paint, s)
            }
        },
        overlay = { layout ->
            val t = flight.value
            if (t > 0f && t < 1f && layout.measured) drawDrop(layout, source, paint, t, drop)
        },
    )
}

private fun DrawScope.drawDrop(layout: CardLayout, source: Int, color: Color, t: Float, drop: Path) {
    val start = layout.swatchCenter(source.toFloat())
    val tee = layout.teeRect
    val end = Offset(tee.left + tee.width * TeeImpact.x, tee.top + tee.height * TeeImpact.y)
    val control = Offset((start.x + end.x) / 2, min(start.y, end.y) - ArcLift.toPx())
    val u = 1f - t
    val position = start * (u * u) + control * (2 * u * t) + end * (t * t)
    val tangent = (control - start) * (2 * u) + (end - control) * (2 * t)
    val degrees = atan2(tangent.y, tangent.x) * 180f / PI.toFloat()
    // The drop swells as it leaves the swatch.
    val radius = DropRadius.toPx() * (0.55f + 0.45f * (t * 5f).coerceAtMost(1f))
    withTransform({
        translate(position.x, position.y)
        rotate(degrees, Offset.Zero)
        scale(radius, radius, Offset.Zero)
    }) {
        drawPath(drop, color)
        drawCircle(Color.White, 0.28f, Offset(0.1f, -0.42f), alpha = 0.6f)
    }
}

private fun DrawScope.drawSplash(impact: Offset, width: Float, color: Color, spread: Float) {
    val t = (spread / SplashShare).coerceAtMost(1f)
    if (t >= 1f) return
    val eased = 1f - (1f - t) * (1f - t) * (1f - t)
    SplashAngles.forEachIndexed { i, angle ->
        val rad = angle * PI.toFloat() / 180f
        val distance = width * (0.05f + SplashReach[i] * eased)
        val center = impact + Offset(cos(rad), sin(rad)) * distance
        drawCircle(color, width * 0.032f * SplashSize[i] * (1f - t * 0.7f), center, alpha = 1f - t * t)
    }
}

/** A unit teardrop: round head of radius 1 at the origin, tail pointing to −x. */
private fun teardrop(): Path = Path().apply {
    moveTo(-2.6f, 0f)
    lineTo(-0.385f, -0.923f)
    arcTo(Rect(-1f, -1f, 1f, 1f), 247.4f, 225.2f, false)
    close()
}
