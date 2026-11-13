package com.azersun.uitemplates.demos.fab2

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azersun.uitemplates.demos.menus.Glyph
import com.azersun.uitemplates.demos.menus.GlyphIcon
import com.azersun.uitemplates.demos.menus.MenuColors
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

private val MergedRadius = 32.dp
private val SplitRadius = 27.dp
private val HalfSpread = 64.dp
private val ZoneWidth = 260.dp
private val ZoneHeight = 132.dp
private val ZoneCenterY = ZoneHeight - 38.dp
private val LabelLift = 20.dp
private val TouchSlack = 8.dp

/** The neck snaps once the centres are this many radii apart. */
private const val BreakRadii = 3.2f
private const val AutoplayOpenMillis = 1800L

@Composable
fun SplitActionsFabDemo() {
    val autoplay = rememberAutoplay()
    var open by remember { mutableStateOf(false) }
    val split = remember { Animatable(0f) }
    val neck = remember { Path() }

    LaunchedEffect(open) {
        // Under-damped both ways, so the halves wobble like a liquid as they part and rejoin.
        val spec = if (open) spring<Float>(dampingRatio = 0.5f, stiffness = 200f) else spring(dampingRatio = 0.6f, stiffness = 260f)
        split.animateTo(if (open) 1f else 0f, spec)
    }

    AutoplayEffect(autoplay) {
        open = true
        delay(AutoplayOpenMillis)
        open = false
    }

    NotesScene(
        6,
        "Split Actions",
        "Tap the button: it splits into Voice and Write, pulled apart with a gooey neck. Tap again and they merge back.",
        autoplay,
    ) {
        Column(Modifier.fillMaxSize()) {
            NotesHeader()
            NoteList()
        }
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
                .size(ZoneWidth, ZoneHeight)
                .semantics {
                    role = Role.Button
                    onClick(if (open) "Merge actions" else "Split actions") {
                        open = !open
                        true
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { pos ->
                        val p = split.value
                        val reach = radius(p) + TouchSlack.toPx()
                        val hit = (pos - leftCenter(p)).getDistance() <= reach || (pos - rightCenter(p)).getDistance() <= reach
                        if (hit) open = !open
                    }
                },
        ) {
            Canvas(Modifier.fillMaxSize()) { drawGoo(split.value, neck) }
            Canvas(
                Modifier
                    .centeredAt { zoneCenter() }
                    .size(24.dp)
                    .graphicsLayer {
                        val p = split.value
                        alpha = 1f - smoothstep(0f, 0.3f, p)
                        rotationZ = 90f * p
                    },
            ) { drawPlus(Color.White) }
            SplitHalf(Glyph.Mic, "Voice", center = { leftCenter(split.value) }, progress = { split.value })
            SplitHalf(Glyph.Pencil, "Write", center = { rightCenter(split.value) }, progress = { split.value })
        }
    }
}

@Composable
private fun SplitHalf(glyph: Glyph, label: String, center: Density.() -> Offset, progress: () -> Float) {
    GlyphIcon(
        glyph,
        Color.White,
        size = 22.dp,
        modifier = Modifier
            .centeredAt(center)
            .graphicsLayer { alpha = smoothstep(0.4f, 0.85f, progress()) },
    )
    Text(
        label,
        color = MenuColors.Text,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .centeredAt {
                val c = center()
                Offset(c.x, c.y - radius(progress()) - LabelLift.toPx())
            }
            .graphicsLayer {
                val shown = smoothstep(0.6f, 1f, progress())
                alpha = shown
                translationY = (1f - shown) * 6.dp.toPx()
            },
    )
}

private fun Density.zoneCenter() = Offset(ZoneWidth.toPx() / 2, ZoneCenterY.toPx())

/** Spring overshoot below 0 just keeps the halves merged instead of crossing over. */
private fun Density.spread(p: Float) = HalfSpread.toPx() * p.coerceAtLeast(0f)

private fun Density.radius(p: Float) = mix(MergedRadius.toPx(), SplitRadius.toPx(), p.coerceIn(0f, 1f))

private fun Density.leftCenter(p: Float) = zoneCenter() - Offset(spread(p), 0f)

private fun Density.rightCenter(p: Float) = zoneCenter() + Offset(spread(p), 0f)

/**
 * Both circles and the neck share one horizontal gradient: merged it is the button's blue→violet,
 * split it hardens into a solid pink half and a solid blue half with the blend only in the neck.
 */
private fun DrawScope.drawGoo(p: Float, neck: Path) {
    val settled = p.coerceIn(0f, 1f)
    val c = zoneCenter()
    val r = radius(p)
    val c1 = leftCenter(p)
    val c2 = rightCenter(p)
    val left = lerp(MenuColors.Blue, MenuColors.Pink, settled)
    val right = lerp(MenuColors.Violet, MenuColors.Blue, settled)
    val solid = 2f * r / (c2.x - c1.x + 2f * r) * settled
    val brush = Brush.horizontalGradient(
        0f to left,
        solid to left,
        1f - solid to right,
        1f to right,
        startX = c1.x - r,
        endX = c2.x + r,
    )
    drawCircle(brush, r, c1)
    drawCircle(brush, r, c2)

    val distance = c2.x - c1.x
    val breakAt = r * BreakRadii
    if (distance <= 0f || distance >= breakAt) return
    val t = distance / breakAt
    // Half-thickness of the neck at its waist, thinning to nothing as it reaches the break.
    val waist = r * 0.85f * (1f - t).pow(0.9f)
    // Where the neck leaves each circle, measured from the line between the centres.
    val angle = (60f - 35f * t) * PI.toFloat() / 180f
    val ax = r * cos(angle)
    val ay = r * sin(angle)
    // A quadratic's midpoint is halfway between its chord and control point, so this puts the
    // waist exactly [waist] from the centre line.
    val control = 2f * waist - ay
    neck.reset()
    neck.moveTo(c1.x + ax, c.y - ay)
    neck.quadraticTo(c.x, c.y - control, c2.x - ax, c.y - ay)
    neck.lineTo(c2.x - ax, c.y + ay)
    neck.quadraticTo(c.x, c.y + control, c1.x + ax, c.y + ay)
    neck.close()
    drawPath(neck, brush)
}
