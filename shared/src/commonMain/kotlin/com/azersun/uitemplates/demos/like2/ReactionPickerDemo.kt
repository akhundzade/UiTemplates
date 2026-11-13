package com.azersun.uitemplates.demos.like2

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.util.lerp
import com.azersun.uitemplates.demos.like.LikeColors
import com.azersun.uitemplates.demos.like.LikeStage
import com.azersun.uitemplates.demos.like.heart
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class Face { Love, Haha, Wow, Sad, Star }

private class Reaction(val label: String, val color: Color, val face: Face)

private val Reactions = listOf(
    Reaction("Love", LikeColors.Pink, Face.Love),
    Reaction("Haha", LikeColors.Yellow, Face.Haha),
    Reaction("Wow", Color(0xFFFFA53D), Face.Wow),
    Reaction("Sad", Color(0xFF6C9BFF), Face.Sad),
    Reaction("Star", LikeColors.Violet, Face.Star),
)

private val FaceInk = Color(0xFF4A2A0A)
private val Pill = RoundedCornerShape(50)

// The picker and the button sit at fixed spots in a fixed-size area, so a finger's position maps
// straight onto a reaction without measuring anything.
private val AreaWidth = 300.dp
private val AreaHeight = 240.dp
private val ItemSize = 44.dp
private val ItemGap = 8.dp
private val PickerPad = 8.dp
private val PickerWidth = ItemSize * 5 + ItemGap * 4 + PickerPad * 2
private val PickerHeight = ItemSize + PickerPad * 2
private val PickerLeft = (AreaWidth - PickerWidth) / 2
private val PickerTop = 94.dp
private val ButtonHeight = 48.dp
private val ButtonPad = 18.dp
private val ButtonIcon = 22.dp
private val ButtonBottom = 24.dp
private val HoverLift = 14.dp
private const val HoverScale = 1.45f
private const val FlyMillis = 480
private val AutoHoverPath = intArrayOf(0, 1, 2, 3, 2, 1, 0)

@Composable
fun ReactionPickerDemo() {
    var selected by remember { mutableIntStateOf(-1) }
    var open by remember { mutableStateOf(false) }
    var hovered by remember { mutableIntStateOf(-1) }
    var pressed by remember { mutableStateOf(false) }
    var flying by remember { mutableIntStateOf(-1) }
    var flyFrom by remember { mutableStateOf(Offset.Zero) }
    var flySize by remember { mutableFloatStateOf(0f) }
    var buttonBounds by remember { mutableStateOf(Rect.Zero) }
    val fly = remember { Animatable(0f) }
    val bump = remember { Animatable(1f) }
    val reveal = remember { Animatable(0f) }
    val pops = remember { List(Reactions.size) { Animatable(0f) } }
    val flyPath = remember { Path() }
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(open) {
        coroutineScope {
            if (open) {
                launch { reveal.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = 500f)) }
                pops.forEachIndexed { i, pop ->
                    launch {
                        delay(40L + i * 40L)
                        pop.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 520f))
                    }
                }
            } else {
                pops.forEachIndexed { i, pop ->
                    launch {
                        delay((pops.size - 1 - i) * 20L)
                        pop.animateTo(0f, tween(130))
                    }
                }
                launch {
                    delay(80)
                    reveal.animateTo(0f, tween(160))
                }
            }
        }
    }

    fun select(index: Int, fromHover: Boolean) {
        with(density) {
            val scale = if (fromHover) HoverScale else 1f
            val center = itemCenter(index)
            val bottom = center.y + ItemSize / 2
            flyFrom = Offset(
                center.x.toPx(),
                bottom.toPx() - ItemSize.toPx() * scale / 2 - if (fromHover) HoverLift.toPx() else 0f,
            )
            flySize = ItemSize.toPx() * scale
        }
        flying = index
        selected = index
        hovered = -1
        open = false
        scope.launch {
            fly.snapTo(0f)
            fly.animateTo(1f, tween(FlyMillis, easing = FastOutSlowInEasing))
            flying = -1
            bump.bounce()
        }
    }

    fun tapLike() {
        selected = if (selected == -1) 0 else -1
        scope.launch { bump.bounce() }
    }

    LikeStage(
        4,
        "Reaction Picker",
        "Hold Like: reactions pop up, slide to one, it flies into the button.",
        autoplay = {
            try {
                pressed = true
                delay(450)
                open = true
                delay(550)
                AutoHoverPath.forEach {
                    hovered = it
                    delay(230)
                }
                delay(150)
                select(0, fromHover = true)
            } finally {
                pressed = false
                hovered = -1
            }
        },
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(AreaWidth, AreaHeight)
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            if (open) {
                                val i = itemUnder(down.position)
                                if (i >= 0) select(i, fromHover = false) else open = false
                                return@awaitEachGesture
                            }
                            if (flying >= 0 || !buttonBounds.contains(down.position)) return@awaitEachGesture
                            pressed = true
                            var lifted = false
                            val early = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                                lifted = waitForUpOrCancellation() != null
                                true
                            }
                            if (early != null) {
                                pressed = false
                                if (lifted) tapLike()
                                return@awaitEachGesture
                            }
                            open = true
                            try {
                                while (true) {
                                    val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                                    val at = hoverIndex(change.position)
                                    if (!change.pressed) {
                                        when {
                                            at >= 0 -> select(at, fromHover = hovered == at)
                                            buttonBounds.contains(change.position) -> Unit
                                            else -> open = false
                                        }
                                        break
                                    }
                                    hovered = at
                                    change.consume()
                                }
                            } finally {
                                pressed = false
                                hovered = -1
                            }
                        }
                    },
            ) {
                Box(
                    Modifier
                        .offset(PickerLeft, PickerTop)
                        .size(PickerWidth, PickerHeight)
                        .graphicsLayer {
                            val r = reveal.value
                            alpha = r.coerceIn(0f, 1f)
                            scaleX = 0.7f + 0.3f * r
                            scaleY = 0.7f + 0.3f * r
                            transformOrigin = TransformOrigin(0.5f, 1f)
                        }
                        .clip(Pill)
                        .background(Color(0xFF2A2752))
                        .border(1.dp, Color(0xFF3A3670), Pill),
                )
                Reactions.forEachIndexed { i, reaction ->
                    ReactionItem(reaction, itemCenter(i), pops[i], hovered = hovered == i, dimmed = hovered >= 0, hidden = flying == i)
                }
                LikeButton(
                    reaction = Reactions.getOrNull(selected),
                    pressed = pressed,
                    iconHidden = flying >= 0,
                    bump = bump,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = ButtonBottom)
                        .onGloballyPositioned { buttonBounds = Rect(it.positionInParent(), it.size.toSize()) },
                )
                if (flying >= 0) {
                    val reaction = Reactions[flying]
                    Canvas(Modifier.fillMaxSize()) {
                        val t = fly.value
                        val bounds = buttonBounds
                        val target = Offset(bounds.left + (ButtonPad + ButtonIcon / 2).toPx(), bounds.center.y)
                        val control = Offset(lerp(flyFrom.x, target.x, 0.2f), minOf(flyFrom.y, target.y) - 70.dp.toPx())
                        val u = 1f - t
                        val at = flyFrom * (u * u) + control * (2f * u * t) + target * (t * t)
                        val radius = lerp(flySize, ButtonIcon.toPx(), t) / 2f
                        drawReaction(reaction, at, radius, flyPath)
                    }
                }
            }
            Text("Hold Like, slide to a reaction", color = LikeColors.Outline, fontSize = 13.sp)
        }
    }
}

@Composable
private fun ReactionItem(
    reaction: Reaction,
    position: DpOffset,
    pop: Animatable<Float, AnimationVector1D>,
    hovered: Boolean,
    dimmed: Boolean,
    hidden: Boolean,
) {
    val hover = animateFloatAsState(
        when {
            hovered -> HoverScale
            dimmed -> 0.88f
            else -> 1f
        },
        spring(dampingRatio = 0.55f, stiffness = 520f),
        label = "hover",
    )
    val path = remember { Path() }
    Box(Modifier.offset(position.x - ItemSize / 2, position.y - ItemSize / 2).size(ItemSize)) {
        Text(
            reaction.label,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .wrapContentSize(unbounded = true)
                .graphicsLayer {
                    val lift = lift(hover.value)
                    alpha = if (hidden) 0f else lift
                    translationY = -ItemSize.toPx() * (hover.value - 1f).coerceAtLeast(0f) - lift * HoverLift.toPx() - 26.dp.toPx()
                }
                .clip(Pill)
                .background(Color(0xE6000000))
                .padding(horizontal = 8.dp, vertical = 3.dp),
        )
        Canvas(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val p = pop.value
                    val s = p * hover.value
                    scaleX = s
                    scaleY = s
                    translationY = -lift(hover.value) * HoverLift.toPx() + (1f - p) * 12.dp.toPx()
                    alpha = if (hidden) 0f else p.coerceIn(0f, 1f)
                    transformOrigin = TransformOrigin(0.5f, 1f)
                },
        ) {
            drawReaction(reaction, center, size.minDimension / 2f, path)
        }
    }
}

@Composable
private fun LikeButton(
    reaction: Reaction?,
    pressed: Boolean,
    iconHidden: Boolean,
    bump: Animatable<Float, AnimationVector1D>,
    modifier: Modifier = Modifier,
) {
    val press by animateFloatAsState(if (pressed) 0.94f else 1f, tween(120), label = "press")
    val accent by animateColorAsState(reaction?.color ?: LikeColors.Outline, tween(240), label = "accent")
    val path = remember { Path() }
    Row(
        modifier
            .graphicsLayer {
                val s = press * bump.value
                scaleX = s
                scaleY = s
            }
            .height(ButtonHeight)
            .clip(Pill)
            .background(accent.copy(alpha = 0.14f))
            .border(1.5.dp, accent.copy(alpha = 0.7f), Pill)
            .animateContentSize()
            .padding(horizontal = ButtonPad),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(ButtonIcon).graphicsLayer { alpha = if (iconHidden) 0f else 1f }) {
            if (reaction == null) {
                path.heart(Offset.Zero, size.width)
                drawPath(path, LikeColors.Text, style = Stroke(2.dp.toPx(), join = StrokeJoin.Round))
            } else {
                drawReaction(reaction, center, size.minDimension / 2f, path)
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            reaction?.label ?: "Like",
            color = reaction?.color ?: LikeColors.Text,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private fun itemCenter(index: Int): DpOffset = DpOffset(
    PickerLeft + PickerPad + ItemSize / 2 + (ItemSize + ItemGap) * index,
    PickerTop + PickerHeight / 2,
)

private fun lift(hover: Float): Float = ((hover - 1f) / (HoverScale - 1f)).coerceIn(0f, 1f)

/** The reaction a sliding finger points at: generous above the row, so the finger can stay low. */
private fun Density.hoverIndex(position: Offset): Int {
    val top = PickerTop.toPx()
    val bottom = (PickerTop + PickerHeight).toPx()
    if (position.y < top - 80.dp.toPx() || position.y > bottom + 16.dp.toPx()) return -1
    val step = (ItemSize + ItemGap).toPx()
    val x = position.x - (PickerLeft + PickerPad).toPx() + ItemGap.toPx() / 2
    if (x < -ItemGap.toPx() || x > step * Reactions.size + ItemGap.toPx()) return -1
    return (x / step).toInt().coerceIn(0, Reactions.lastIndex)
}

/** The reaction a tap lands on while the picker stays open. */
private fun Density.itemUnder(position: Offset): Int {
    val reach = (ItemSize / 2 + 6.dp).toPx()
    Reactions.indices.forEach { i ->
        val c = itemCenter(i)
        if ((position - Offset(c.x.toPx(), c.y.toPx())).getDistance() <= reach) return i
    }
    return -1
}

private suspend fun Animatable<Float, AnimationVector1D>.bounce() {
    animateTo(1.1f, tween(120, easing = FastOutSlowInEasing))
    animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 500f))
}

/** A reaction: a coloured disc with a white heart or star, or a simple face. */
private fun DrawScope.drawReaction(reaction: Reaction, center: Offset, radius: Float, path: Path) {
    drawCircle(reaction.color, radius, center)
    val stroke = radius * 0.12f
    fun at(x: Float, y: Float) = Offset(center.x + x * radius, center.y + y * radius)
    fun line(x1: Float, y1: Float, x2: Float, y2: Float) = drawLine(FaceInk, at(x1, y1), at(x2, y2), stroke, StrokeCap.Round)
    when (reaction.face) {
        Face.Love -> {
            path.heart(at(-0.55f, -0.5f), radius * 1.1f)
            drawPath(path, Color.White)
        }
        Face.Star -> {
            path.star(at(0f, 0.05f), radius * 0.62f, radius * 0.3f, radius * 0.07f)
            drawPath(path, Color.White)
        }
        Face.Haha -> {
            line(-0.5f, -0.4f, -0.24f, -0.26f)
            line(-0.24f, -0.26f, -0.5f, -0.12f)
            line(0.5f, -0.4f, 0.24f, -0.26f)
            line(0.24f, -0.26f, 0.5f, -0.12f)
            drawArc(FaceInk, 0f, 180f, useCenter = true, topLeft = at(-0.5f, -0.1f), size = Size(radius, radius * 0.8f))
        }
        Face.Wow -> {
            line(-0.5f, -0.5f, -0.22f, -0.6f)
            line(0.22f, -0.6f, 0.5f, -0.5f)
            drawOval(FaceInk, at(-0.42f, -0.38f), Size(radius * 0.18f, radius * 0.26f))
            drawOval(FaceInk, at(0.24f, -0.38f), Size(radius * 0.18f, radius * 0.26f))
            drawOval(FaceInk, at(-0.17f, 0.16f), Size(radius * 0.34f, radius * 0.44f))
        }
        Face.Sad -> {
            line(-0.5f, -0.38f, -0.2f, -0.5f)
            line(0.2f, -0.5f, 0.5f, -0.38f)
            drawCircle(FaceInk, radius * 0.1f, at(-0.3f, -0.18f))
            drawCircle(FaceInk, radius * 0.1f, at(0.3f, -0.18f))
            drawArc(
                FaceInk,
                startAngle = 200f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = at(-0.34f, 0.26f),
                size = Size(radius * 0.68f, radius * 0.5f),
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
            val tear = Color(0xFFD6F0FF)
            val tip = at(-0.44f, -0.04f)
            val left = at(-0.53f, 0.14f)
            val right = at(-0.35f, 0.14f)
            path.reset()
            path.moveTo(tip.x, tip.y)
            path.lineTo(left.x, left.y)
            path.lineTo(right.x, right.y)
            path.close()
            drawPath(path, tear)
            drawCircle(tear, radius * 0.1f, at(-0.44f, 0.17f))
        }
    }
}
