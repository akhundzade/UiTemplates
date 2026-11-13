package com.azersun.uitemplates.demos.addtocart

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import kotlinx.coroutines.delay

internal object CartColors {
    val Background = Color(0xFFEFEEEB)
    val Card = Color(0xFFF7F6F3)
    val Thumb = Color(0xFFE7E5E0)
    val Line = Color(0xFFBDBBB6)
    val Ink = Color(0xFF111111)
    val Muted = Color(0xFF77756F)
}

internal val Pill = RoundedCornerShape(50)
internal val ButtonHeight = 38.dp
internal val IconStroke = 1.3.dp

private const val AutoplayDelayMillis = 700L

/** Shared state of one product scene: the cart count and where the parts sit for flights. */
@Stable
internal class CartScene {
    var count by mutableIntStateOf(1)
    var interacted by mutableStateOf(false)
    internal var root: LayoutCoordinates? = null
    internal var thumb: LayoutCoordinates? = null
    internal var button: LayoutCoordinates? = null
    internal var cart: LayoutCoordinates? = null
    internal var badge: LayoutCoordinates? = null

    /** [target]'s bounds in the scene's coordinates, unclipped by any ancestor. */
    fun bounds(target: LayoutCoordinates?): Rect {
        val root = root?.takeIf { it.isAttached } ?: return Rect.Zero
        val node = target?.takeIf { it.isAttached } ?: return Rect.Zero
        return Rect(root.localPositionOf(node, Offset.Zero), node.size.toSize())
    }
}

@Composable
internal fun rememberCartScene(): CartScene = remember { CartScene() }

/** The light counterpart of the dark `MenuStage`: a hairline card with a numbered caption. */
@Composable
internal fun CartStage(
    number: Int,
    title: String,
    caption: String,
    content: @Composable BoxScope.() -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(CartColors.Background)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .weight(1f)
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(CartColors.Card)
                .border(1.dp, CartColors.Line, RoundedCornerShape(26.dp)),
            contentAlignment = Alignment.Center,
            content = content,
        )
        Column(
            Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .padding(top = 14.dp)
                .border(1.dp, CartColors.Line, RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Caps("$number  ${title.uppercase()}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text(caption, color = CartColors.Muted, fontSize = 12.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

/**
 * The product row (thumbnail, label, [button], [cart]) centred in the stage, with [overlay]
 * drawn above it in the same coordinates as [CartScene.bounds]. Autoplays [onAutoplay] once,
 * unless the scene is touched first.
 */
@Composable
internal fun ProductScene(
    scene: CartScene,
    number: Int,
    title: String,
    caption: String,
    onAutoplay: () -> Unit,
    button: @Composable () -> Unit,
    cart: @Composable () -> Unit = { CartWithBadge(scene) },
    overlay: @Composable BoxScope.() -> Unit = {},
) {
    val autoplay by rememberUpdatedState(onAutoplay)
    LaunchedEffect(Unit) {
        delay(AutoplayDelayMillis)
        if (!scene.interacted) autoplay()
    }

    CartStage(number, title, caption) {
        Box(
            Modifier
                .fillMaxSize()
                .onGloballyPositioned { scene.root = it }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        scene.interacted = true
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(76.dp)
                        .onGloballyPositioned { scene.thumb = it }
                        .clip(RoundedCornerShape(14.dp))
                        .background(CartColors.Thumb)
                        .border(1.dp, CartColors.Line, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(Modifier.size(44.dp)) { drawMug(CartColors.Ink, IconStroke.toPx()) }
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Caps("CERAMIC MUG")
                    Box(Modifier.padding(top = 8.dp).fillMaxWidth().height(1.dp).background(CartColors.Line))
                    Box(Modifier.padding(top = 14.dp).onGloballyPositioned { scene.button = it }) { button() }
                }
                Spacer(Modifier.width(14.dp))
                cart()
            }
            overlay()
        }
    }
}

@Composable
internal fun Caps(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = CartColors.Ink,
    fontSize: TextUnit = 10.sp,
    fontWeight: FontWeight = FontWeight.Medium,
) {
    Text(text, color = color, fontSize = fontSize, fontWeight = fontWeight, letterSpacing = 2.sp, maxLines = 1, modifier = modifier)
}

/** Outlined pill that dips slightly while pressed; [onClick] null makes it inert. */
@Composable
internal fun PillButton(
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, spring(stiffness = 800f), label = "press")
    Box(
        modifier
            .height(ButtonHeight)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(Pill)
            .border(1.dp, CartColors.Ink, Pill)
            .then(if (onClick != null) Modifier.clickable(interaction, indication = null, onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

/** The plain "ADD TO CART  +" button. */
@Composable
internal fun AddButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    PillButton(onClick, modifier.fillMaxWidth()) { AddLabel() }
}

@Composable
internal fun AddLabel(modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Caps("ADD TO CART")
        Spacer(Modifier.width(10.dp))
        Canvas(Modifier.size(9.dp)) { drawPlus(CartColors.Ink, IconStroke.toPx()) }
    }
}

/** Thin-line cart with the count badge; registers both with [scene] for flights to aim at. */
@Composable
internal fun CartWithBadge(
    scene: CartScene,
    cartModifier: Modifier = Modifier,
    badgeModifier: Modifier = Modifier,
) {
    Box(Modifier.size(46.dp)) {
        Canvas(
            cartModifier
                .align(Alignment.BottomStart)
                .size(34.dp)
                .onGloballyPositioned { scene.cart = it },
        ) { drawCart(CartColors.Ink, IconStroke.toPx()) }
        CountBadge(
            scene.count,
            badgeModifier
                .align(Alignment.TopEnd)
                .onGloballyPositioned { scene.badge = it },
        )
    }
}

@Composable
private fun CountBadge(count: Int, modifier: Modifier = Modifier) {
    Box(
        modifier
            .defaultMinSize(minWidth = 17.dp, minHeight = 17.dp)
            .clip(Pill)
            .background(CartColors.Ink)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = count,
            transitionSpec = {
                val direction = if (targetState > initialState) 1 else -1
                (slideInVertically(tween(220)) { it * direction } + fadeIn(tween(220))) togetherWith
                    (slideOutVertically(tween(220)) { -it * direction } + fadeOut(tween(160)))
            },
            label = "badge",
        ) { value ->
            Text("$value", color = Color.White, fontSize = 9.sp, lineHeight = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/** Mug with three steam wisps in a [side] square at [topLeft], by default centred in the canvas. */
internal fun DrawScope.drawMug(
    color: Color,
    stroke: Float,
    side: Float = size.minDimension,
    topLeft: Offset = Offset((size.width - side) / 2, (size.height - side) / 2),
) {
    val s = side
    val ox = topLeft.x
    val oy = topLeft.y
    fun x(f: Float) = ox + s * f
    fun y(f: Float) = oy + s * f
    val style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val body = Path().apply {
        moveTo(x(0.2f), y(0.42f))
        lineTo(x(0.2f), y(0.78f))
        quadraticTo(x(0.2f), y(0.88f), x(0.3f), y(0.88f))
        lineTo(x(0.58f), y(0.88f))
        quadraticTo(x(0.68f), y(0.88f), x(0.68f), y(0.78f))
        lineTo(x(0.68f), y(0.42f))
        close()
    }
    drawPath(body, color, style = style)
    drawArc(color, -90f, 180f, false, Offset(x(0.56f), y(0.5f)), Size(s * 0.24f, s * 0.24f), style = style)
    listOf(0.32f, 0.44f, 0.56f).forEach { cx ->
        val wisp = Path().apply {
            moveTo(x(cx), y(0.34f))
            cubicTo(x(cx - 0.05f), y(0.27f), x(cx + 0.05f), y(0.2f), x(cx), y(0.12f))
        }
        drawPath(wisp, color, style = style)
    }
}

/** Where a dropped item lands inside the cart's basket, as fractions of the cart icon. */
internal val BasketCenter = Offset(0.56f, 0.46f)

internal fun DrawScope.drawCart(color: Color, stroke: Float, side: Float = size.minDimension) {
    val s = side
    fun at(x: Float, y: Float) = Offset(s * x, s * y)
    val style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val frame = Path().apply {
        moveTo(s * 0.06f, s * 0.2f)
        lineTo(s * 0.18f, s * 0.2f)
        lineTo(s * 0.3f, s * 0.64f)
        lineTo(s * 0.8f, s * 0.64f)
        lineTo(s * 0.9f, s * 0.3f)
        lineTo(s * 0.22f, s * 0.3f)
    }
    drawPath(frame, color, style = style)
    drawCircle(color, s * 0.06f, at(0.36f, 0.82f), style = style)
    drawCircle(color, s * 0.06f, at(0.74f, 0.82f), style = style)
}

internal fun DrawScope.drawPlus(color: Color, stroke: Float) {
    val c = center
    val r = size.minDimension / 2
    drawLine(color, Offset(c.x - r, c.y), Offset(c.x + r, c.y), stroke, StrokeCap.Round)
    drawLine(color, Offset(c.x, c.y - r), Offset(c.x, c.y + r), stroke, StrokeCap.Round)
}

internal fun DrawScope.drawCheck(color: Color, stroke: Float, side: Float = size.minDimension) {
    val s = side
    val path = Path().apply {
        moveTo(s * 0.14f, s * 0.54f)
        lineTo(s * 0.4f, s * 0.8f)
        lineTo(s * 0.88f, s * 0.24f)
    }
    drawPath(path, color, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

internal fun DrawScope.drawTrashLine(color: Color, stroke: Float) {
    val s = size.minDimension
    fun at(x: Float, y: Float) = Offset(s * x, s * y)
    val style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
    drawLine(color, at(0.14f, 0.26f), at(0.86f, 0.26f), stroke, StrokeCap.Round)
    val handle = Path().apply {
        moveTo(s * 0.38f, s * 0.26f)
        lineTo(s * 0.38f, s * 0.12f)
        lineTo(s * 0.62f, s * 0.12f)
        lineTo(s * 0.62f, s * 0.26f)
    }
    drawPath(handle, color, style = style)
    val body = Path().apply {
        moveTo(s * 0.24f, s * 0.36f)
        lineTo(s * 0.3f, s * 0.9f)
        lineTo(s * 0.7f, s * 0.9f)
        lineTo(s * 0.76f, s * 0.36f)
    }
    drawPath(body, color, style = style)
}

/** Maps [t] onto 0..1 across the window [start]..[end] of a 0..1 timeline. */
internal fun phase(t: Float, start: Float, end: Float): Float = ((t - start) / (end - start)).coerceIn(0f, 1f)

internal fun lerp(start: Float, stop: Float, fraction: Float): Float = start + (stop - start) * fraction

/** Point on the quadratic Bézier from [from] to [to] bending through [control]. */
internal fun quadratic(from: Offset, control: Offset, to: Offset, t: Float): Offset {
    val u = 1f - t
    return from * (u * u) + control * (2 * u * t) + to * (t * t)
}
