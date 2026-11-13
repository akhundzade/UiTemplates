package com.azersun.uitemplates.demos.password

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

private const val FlightNanos = 420_000_000L
private const val PasteStaggerNanos = 60_000_000L
private const val TrailSteps = 6
private const val TrailSpacing = 0.035f
private val ArcLift = 36.dp

/** A spark launched by the character typed at [index]; [target] is the fill it unlocks when it lands. */
private class Spark(val index: Int, val target: Float, val color: Color, val delayNanos: Long) {
    var start = -1L
}

/** Where things are, read at draw time to map the caret and the bar into the overlay's space. */
private class Anchors {
    var root: LayoutCoordinates? = null
    var text: LayoutCoordinates? = null
    var bar: LayoutCoordinates? = null
    var layout: TextLayoutResult? = null
}

/**
 * Fill in segments for [text]: the level's whole segments plus a per-character share of the next one,
 * capped so the bar only reaches a new segment when the level actually changes.
 */
private fun fillFor(text: String): Float {
    val level = strengthOf(text)
    val n = text.length
    val partial = when (level) {
        1 -> (n - 1) * 0.12f
        2 -> (n - 8) * 0.2f
        3 -> (n - 8) * 0.25f
        else -> 0f
    }
    return level + partial.coerceIn(0f, 0.6f)
}

@Composable
fun SparkFeedDemo() {
    val form = remember { PasswordFormState() }
    val sparks = remember { mutableStateListOf<Spark>() }
    val anchors = remember { Anchors() }
    val fill = remember { Animatable(0f) }
    val impact = remember { Animatable(0f) }
    var committed by remember { mutableFloatStateOf(0f) }
    var frameNanos by remember { mutableLongStateOf(0L) }
    val scope = rememberCoroutineScope()
    val shownLevel = committed.toInt().coerceIn(0, SegmentCount)
    val color by animateLevelColor(shownLevel)

    LaunchedEffect(form) {
        var last = form.text
        snapshotFlow { form.text }.collect { text ->
            if (text.length > last.length) {
                for (n in last.length + 1..text.length) {
                    val prefix = text.take(n)
                    sparks += Spark(
                        index = n,
                        target = fillFor(prefix),
                        color = StrengthLevels[strengthOf(prefix)].color,
                        delayNanos = (n - last.length - 1) * PasteStaggerNanos,
                    )
                }
            } else {
                sparks.removeAll { it.index > text.length }
                committed = fillFor(text)
            }
            last = text
        }
    }
    val flying = sparks.isNotEmpty()
    LaunchedEffect(flying) {
        while (sparks.isNotEmpty()) {
            withFrameNanos { now ->
                frameNanos = now
                sparks.forEach { if (it.start < 0) it.start = now + it.delayNanos }
                val landed = sparks.filter { now - it.start >= FlightNanos }
                if (landed.isNotEmpty()) {
                    sparks.removeAll(landed)
                    val reached = min(landed.maxOf { it.target }, fillFor(form.text))
                    committed = max(committed, reached)
                    scope.launch {
                        impact.snapTo(1f)
                        impact.animateTo(0f, tween(320))
                    }
                }
            }
        }
    }
    LaunchedEffect(committed) {
        fill.animateTo(committed, spring(dampingRatio = 0.6f, stiffness = 320f))
    }

    PasswordStage(
        3,
        "Spark Feed",
        "Every typed dot fires a spark that arcs down and fills the bar.",
        form,
        fieldTextModifier = Modifier.onGloballyPositioned { anchors.text = it },
        onTextLayout = { anchors.layout = it },
        label = { StrengthLabel(shownLevel) },
        overlay = {
            Canvas(Modifier.matchParentSize().onGloballyPositioned { anchors.root = it }) {
                val now = frameNanos
                if (sparks.isEmpty()) return@Canvas
                val root = anchors.root ?: return@Canvas
                val text = anchors.text ?: return@Canvas
                val bar = anchors.bar ?: return@Canvas
                val layout = anchors.layout ?: return@Canvas
                if (!root.isAttached || !text.isAttached || !bar.isAttached) return@Canvas
                val barSize = bar.size
                val target = root.localPositionOf(
                    bar,
                    Offset(fillEndPx(barSize.width.toFloat(), SegmentGap.toPx(), fill.value), barSize.height / 2f),
                )
                sparks.forEach { spark ->
                    if (spark.start < 0) return@forEach
                    val p = (now - spark.start).toFloat() / FlightNanos
                    if (p <= 0f) return@forEach
                    val caret = layout.getCursorRect(min(spark.index, layout.layoutInput.text.length))
                    val from = root.localPositionOf(text, Offset(caret.left, caret.center.y))
                    drawSpark(from, target, p.coerceAtMost(1f), spark.color)
                }
            }
        },
    ) {
        Canvas(Modifier.fillMaxSize().onGloballyPositioned { anchors.bar = it }) {
            drawTrack()
            val end = fillEndPx(size.width, SegmentGap.toPx(), fill.value)
            if (end <= 0f) return@Canvas
            val c = color
            clipPath(segmentsPath()) { drawRect(c, size = Size(end, size.height)) }
            val flash = impact.value
            if (flash > 0f) {
                val tip = Offset(end, size.height / 2)
                val radius = 12.dp.toPx()
                drawCircle(
                    Brush.radialGradient(listOf(c.copy(alpha = 0.7f * flash), Color.Transparent), center = tip, radius = radius),
                    radius = radius,
                    center = tip,
                )
            }
        }
    }
}

/**
 * A quadratic Bézier walked at constant parameter speed is a true constant-gravity parabola, so the
 * control point above the start makes the spark pop up out of the field and fall into the bar.
 */
private fun DrawScope.drawSpark(from: Offset, to: Offset, p: Float, color: Color) {
    val control = Offset((from.x + to.x) / 2, min(from.y, to.y) - ArcLift.toPx())
    fun at(t: Float): Offset {
        val u = 1f - t
        return from * (u * u) + control * (2 * u * t) + to * (t * t)
    }
    for (k in TrailSteps downTo 1) {
        val t = p - k * TrailSpacing
        if (t <= 0f) continue
        val fade = 1f - k / (TrailSteps + 1f)
        drawCircle(color.copy(alpha = 0.55f * fade), radius = 2.4.dp.toPx() * fade, center = at(t))
    }
    val head = at(p)
    val halo = 9.dp.toPx()
    drawCircle(
        Brush.radialGradient(listOf(color.copy(alpha = 0.65f), Color.Transparent), center = head, radius = halo),
        radius = halo,
        center = head,
    )
    drawCircle(lerp(color, Color.White, 0.6f), radius = 2.6.dp.toPx(), center = head)
}
