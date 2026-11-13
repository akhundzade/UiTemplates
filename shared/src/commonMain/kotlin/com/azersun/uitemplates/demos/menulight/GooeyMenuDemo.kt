package com.azersun.uitemplates.demos.menulight

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

private val BubbleRadius = 17.dp
private val BubbleGap = 12.dp
private val BubbleSpacing = KebabSize / 2 + BubbleRadius + BubbleGap

@Composable
fun GooeyMenuDemo() {
    val state = remember { MinimalMenuState() }
    val scope = rememberCoroutineScope()
    val fill = remember { Animatable(0f) }
    val drops = remember { List(MenuOption.entries.size) { Animatable(0f) } }

    LaunchedEffect(state.open) {
        if (state.open) {
            // The kebab turns solid first so the necks pulled out of it are the same black.
            fill.animateTo(1f, tween(140))
            drops.forEachIndexed { i, drop ->
                launch {
                    delay(i * 130L)
                    drop.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 260f))
                }
            }
        } else {
            drops.indices.reversed().mapIndexed { order, i ->
                launch {
                    delay(order * 70L)
                    drops[i].animateTo(0f, tween(220, easing = FastOutSlowInEasing))
                }
            }.joinAll()
            fill.animateTo(0f, tween(160))
        }
    }

    MinimalMenuScene(state, 3, "Gooey Menu", "Option bubbles drip out of the ⋮ one below another, joined by a gooey neck.") {
        MinimalPage(state) {
            // The kebab itself is drawn by the canvas below so the bubbles can slide out from under it.
            Box(
                Modifier
                    .size(KebabSize)
                    .clip(CircleShape)
                    .clickable { state.open = !state.open },
            )
        }
        if (state.open) {
            Box(Modifier.fillMaxSize().clickable(remember { MutableInteractionSource() }, indication = null) { state.open = false })
        }
        Canvas(Modifier.fillMaxSize()) {
            val f = fill.value
            val progress = drops.map { it.value }
            val kebabRadius = KebabSize.toPx() / 2
            val radius = BubbleRadius.toPx()
            val spacing = BubbleSpacing.toPx()
            val kebab = Offset(size.width - PagePadding.toPx() - kebabRadius, PagePadding.toPx() + kebabRadius)
            var previous = kebab
            var previousRadius = kebabRadius
            val centers = progress.map { p ->
                val c = Offset(kebab.x, previous.y + spacing * p)
                if (p > 0.001f) {
                    metaballNeck(previous, previousRadius, c, radius, breakGap = radius * 0.9f)?.let { drawPath(it, LightColors.Ink) }
                    drawCircle(LightColors.Ink, radius, c)
                }
                previous = c
                previousRadius = radius
                c
            }
            centers.forEachIndexed { i, c ->
                val glyphAlpha = ((progress[i] - 0.55f) / 0.45f).coerceIn(0f, 1f)
                if (glyphAlpha > 0f) drawOptionGlyph(MenuOption.entries[i], c, radius * 0.95f, LightColors.Page, glyphAlpha)
            }
            drawKebab(kebab, kebabRadius, f)
        }
        MenuOption.entries.forEachIndexed { i, option ->
            val onPick = { scope.launch { state.pick(option) }; Unit }
            val top = PagePadding + KebabSize / 2 + BubbleSpacing * (i + 1) - BubbleRadius
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = top, end = PagePadding + KebabSize / 2 - BubbleRadius)
                    .size(BubbleRadius * 2)
                    .clip(CircleShape)
                    .clickable(enabled = state.open, onClick = onPick),
            )
            Row(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = top + BubbleRadius - 13.dp, end = PagePadding + KebabSize + 10.dp)
                    .graphicsLayer {
                        val reveal = ((drops[i].value - 0.7f) / 0.3f).coerceIn(0f, 1f)
                        alpha = reveal
                        translationX = (1f - reveal) * 12.dp.toPx()
                    }
                    .height(26.dp)
                    .clip(RoundedCornerShape(50))
                    .background(LightColors.Page)
                    .border(1.dp, LightColors.Hairline, RoundedCornerShape(50))
                    .clickable(
                        enabled = state.open,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onPick,
                    )
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (option == state.current) {
                    Box(Modifier.size(5.dp).clip(CircleShape).background(LightColors.Ink))
                    Spacer(Modifier.width(8.dp))
                }
                Caps(option.label)
            }
        }
    }
}

/**
 * The bridge two circles grow while they pull apart (the classic metaball construction). It thins
 * to nothing over [breakGap] past touching, so the neck snaps instead of vanishing in one frame.
 */
private fun metaballNeck(c1: Offset, r1: Float, c2: Offset, r2: Float, breakGap: Float): Path? {
    val d = hypot(c2.x - c1.x, c2.y - c1.y)
    if (d <= abs(r1 - r2) || d >= r1 + r2 + breakGap) return null
    val v = 0.5f * (1f - ((d - (r1 + r2)) / breakGap).coerceIn(0f, 1f))
    val handleSize = 2.4f
    val overlap = d < r1 + r2
    val u1 = if (overlap) acos(((r1 * r1 + d * d - r2 * r2) / (2 * r1 * d)).coerceIn(-1f, 1f)) else 0f
    val u2 = if (overlap) acos(((r2 * r2 + d * d - r1 * r1) / (2 * r2 * d)).coerceIn(-1f, 1f)) else 0f
    val between = atan2(c2.y - c1.y, c2.x - c1.x)
    val maxSpread = acos(((r1 - r2) / d).coerceIn(-1f, 1f))
    val pi = PI.toFloat()
    val angle1 = between + u1 + (maxSpread - u1) * v
    val angle2 = between - u1 - (maxSpread - u1) * v
    val angle3 = between + pi - u2 - (pi - u2 - maxSpread) * v
    val angle4 = between - pi + u2 + (pi - u2 - maxSpread) * v
    fun polar(c: Offset, a: Float, r: Float) = Offset(c.x + r * cos(a), c.y + r * sin(a))
    val p1 = polar(c1, angle1, r1)
    val p2 = polar(c1, angle2, r1)
    val p3 = polar(c2, angle3, r2)
    val p4 = polar(c2, angle4, r2)
    val total = r1 + r2
    val handle = min(v * handleSize, hypot(p1.x - p3.x, p1.y - p3.y) / total) * min(1f, d * 2 / total)
    val h1 = polar(p1, angle1 - pi / 2, r1 * handle)
    val h2 = polar(p2, angle2 + pi / 2, r1 * handle)
    val h3 = polar(p3, angle3 + pi / 2, r2 * handle)
    val h4 = polar(p4, angle4 - pi / 2, r2 * handle)
    return Path().apply {
        moveTo(p1.x, p1.y)
        cubicTo(h1.x, h1.y, h3.x, h3.y, p3.x, p3.y)
        lineTo(p4.x, p4.y)
        cubicTo(h4.x, h4.y, h2.x, h2.y, p2.x, p2.y)
        close()
    }
}

/** Thin-line icons for the three options, drawn inside a circle of [radius]. */
private fun DrawScope.drawOptionGlyph(option: MenuOption, center: Offset, radius: Float, color: Color, alpha: Float) {
    val u = radius * 0.5f
    val stroke = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    fun at(x: Float, y: Float) = Offset(center.x + u * x, center.y + u * y)
    when (option) {
        MenuOption.Home -> {
            val house = Path().apply {
                moveTo(at(-0.8f, -0.05f).x, at(-0.8f, -0.05f).y)
                lineTo(at(0f, -0.8f).x, at(0f, -0.8f).y)
                lineTo(at(0.8f, -0.05f).x, at(0.8f, -0.05f).y)
                moveTo(at(-0.55f, -0.25f).x, at(-0.55f, -0.25f).y)
                lineTo(at(-0.55f, 0.75f).x, at(-0.55f, 0.75f).y)
                lineTo(at(0.55f, 0.75f).x, at(0.55f, 0.75f).y)
                lineTo(at(0.55f, -0.25f).x, at(0.55f, -0.25f).y)
            }
            drawPath(house, color, alpha, stroke)
        }
        MenuOption.Explore -> {
            drawCircle(color, u * 0.85f, center, alpha, stroke)
            val needle = Path().apply {
                moveTo(at(0.4f, -0.4f).x, at(0.4f, -0.4f).y)
                lineTo(at(0.12f, 0.12f).x, at(0.12f, 0.12f).y)
                lineTo(at(-0.4f, 0.4f).x, at(-0.4f, 0.4f).y)
                lineTo(at(-0.12f, -0.12f).x, at(-0.12f, -0.12f).y)
                close()
            }
            drawPath(needle, color, alpha, stroke)
        }
        MenuOption.Library -> {
            drawRect(color, at(-0.75f, -0.65f), Size(u * 0.38f, u * 1.4f), alpha, stroke)
            drawRect(color, at(-0.25f, -0.65f), Size(u * 0.38f, u * 1.4f), alpha, stroke)
            drawLine(color, at(0.3f, -0.6f), at(0.72f, 0.72f), stroke.width, StrokeCap.Round, alpha = alpha)
        }
    }
}
