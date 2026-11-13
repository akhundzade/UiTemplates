package com.azersun.uitemplates.demos.password

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private const val GrowMillis = 160
private const val FillMillis = 320
private const val PinchMillis = 180
private const val DrainMillis = 150

// Waist of the neck at full thickness, as a fraction of the segment height.
private const val NeckWaist = 0.6f
private val RecoilSpring = spring<Float>(dampingRatio = 0.3f, stiffness = 900f)

@Composable
fun LiquidLinkDemo() {
    val form = remember { PasswordFormState() }
    val fill = remember { List(SegmentCount) { Animatable(0f) } }
    // Per gap (between segment g and g + 1): grow 0→1 sends a tongue across, neck 1→0 thins the bridge
    // until it snaps, then stub 1→0 recoils the broken ends and droplet 1→0 fades the drop left behind.
    val grow = remember { List(SegmentCount - 1) { Animatable(0f) } }
    val neck = remember { List(SegmentCount - 1) { Animatable(0f) } }
    val stub = remember { List(SegmentCount - 1) { Animatable(0f) } }
    val droplet = remember { List(SegmentCount - 1) { Animatable(0f) } }
    val color by animateLevelColor(form.level)
    val path = remember { Path() }

    LaunchedEffect(form.level) {
        val level = form.level
        repeat(SegmentCount - 1) { g ->
            grow[g].snapTo(0f)
            neck[g].snapTo(0f)
            if (stub[g].value != 0f) launch { stub[g].animateTo(0f, RecoilSpring) }
            if (droplet[g].value != 0f) launch { droplet[g].animateTo(0f, tween(200)) }
        }
        for (k in SegmentCount - 1 downTo level) {
            if (fill[k].value != 0f) fill[k].animateTo(0f, tween(DrainMillis))
        }
        for (k in 0 until level) {
            if (fill[k].value == 1f) continue
            if (k == 0) {
                fill[0].animateTo(1f, tween(FillMillis, easing = FastOutSlowInEasing))
                continue
            }
            val g = k - 1
            neck[g].snapTo(1f)
            grow[g].animateTo(1f, tween(GrowMillis, easing = LinearOutSlowInEasing))
            fill[k].animateTo(1f, tween(FillMillis, easing = FastOutSlowInEasing))
            neck[g].animateTo(0f, tween(PinchMillis, easing = FastOutLinearInEasing))
            grow[g].snapTo(0f)
            stub[g].snapTo(1f)
            droplet[g].snapTo(1f)
            launch { stub[g].animateTo(0f, RecoilSpring) }
            launch { droplet[g].animateTo(0f, tween(260)) }
        }
    }

    PasswordStage(6, "Liquid Link", "The fill oozes across the gap like liquid, then the neck snaps.", form) {
        Canvas(Modifier.fillMaxSize()) {
            drawTrack()
            val c = color
            val h = size.height
            val half = h / 2
            val cy = half
            repeat(SegmentCount) { k ->
                val f = fill[k].value
                if (f <= 0f) return@repeat
                drawRoundRect(c, Offset(segmentLeft(k), 0f), Size(segmentWidth() * f, h), CornerRadius(half))
            }
            repeat(SegmentCount - 1) { g ->
                // Anchored where each rounded cap starts to curve, so the neck blends into the segments
                // like a metaball instead of meeting the cap tips, where a capsule has no height.
                val from = segmentLeft(g) + segmentWidth() - half
                val to = segmentLeft(g + 1) + half
                val mid = (from + to) / 2
                val gr = grow[g].value
                val n = neck[g].value
                when {
                    gr > 0f && gr < 1f -> {
                        tongue(path, from, from + half + (to - from - half) * gr, cy, half)
                        drawPath(path, c)
                    }
                    gr >= 1f && n > 0f -> {
                        val waist = h * NeckWaist * n
                        path.reset()
                        path.moveTo(from, cy - half)
                        // Control points chosen so each side's midpoint sits exactly waist/2 from the centre line.
                        path.quadraticTo(mid, cy + half - waist, to, cy - half)
                        path.lineTo(to, cy + half)
                        path.quadraticTo(mid, cy - half + waist, from, cy + half)
                        path.close()
                        drawPath(path, c)
                    }
                }
                val s = stub[g].value
                if (s > 0f) {
                    val reach = (mid - from) * s
                    tongue(path, from, from + reach, cy, half)
                    drawPath(path, c)
                    tongue(path, to, to - reach, cy, half)
                    drawPath(path, c)
                }
                val d = droplet[g].value
                if (d > 0f) {
                    drawCircle(
                        c.copy(alpha = d),
                        radius = 1.4.dp.toPx() * (0.4f + 0.6f * d),
                        center = Offset(mid, cy + (1f - d) * 4.dp.toPx()),
                    )
                }
            }
        }
    }
}

/** A rounded liquid tongue attached at [base] (full segment height) whose nose reaches [tip], either direction. */
private fun tongue(path: Path, base: Float, tip: Float, cy: Float, half: Float) {
    val inside = if (tip >= base) base - half else base + half
    path.reset()
    path.moveTo(inside, cy - half)
    path.lineTo(base, cy - half)
    path.quadraticTo(tip, cy - half, tip, cy)
    path.quadraticTo(tip, cy + half, base, cy + half)
    path.lineTo(inside, cy + half)
    path.close()
}
