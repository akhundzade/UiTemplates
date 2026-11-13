package com.azersun.uitemplates.demos.search

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azersun.uitemplates.demos.menus.MenuColors
import com.azersun.uitemplates.demos.menus.MenuStage
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal object SearchColors {
    val Field = Color(0xFF15132C)
    val FieldBorder = Color(0xFF34305F)
    val Placeholder = Color(0xFF6C6896)
    val Text = MenuColors.Text
    val Muted = MenuColors.Muted
    val Violet = MenuColors.Violet
    val Blue = MenuColors.Blue
    val Cyan = MenuColors.Cyan
    val Row = MenuColors.Tile
    val RowLight = MenuColors.TileLight
}

internal val FieldHeight = 52.dp
internal val Pill = RoundedCornerShape(50)
internal val QueryStyle = TextStyle(color = SearchColors.Text, fontSize = 16.sp)
internal const val AutoplayDelayMillis = 700L
internal const val TypeDelayMillis = 80L

private val GlowBrush = Brush.horizontalGradient(listOf(SearchColors.Violet, SearchColors.Blue))

@Stable
internal class Query(initial: String = "") {
    var value by mutableStateOf(TextFieldValue(initial, TextRange(initial.length)))

    val text: String get() = value.text

    fun set(text: String) {
        value = TextFieldValue(text, TextRange(text.length))
    }

    suspend fun type(chars: String) {
        chars.forEach { char ->
            delay(TypeDelayMillis)
            set(text + char)
        }
    }
}

/** The scripted run each demo plays once on open, which stops at the first real touch. */
@Stable
internal class Autoplay {
    var running by mutableStateOf(true)
        private set
    var runs by mutableIntStateOf(0)
        private set

    /** Stands in for focus while the script types, since really focusing a field would raise the keyboard. */
    var focus by mutableStateOf(false)

    fun stop() {
        running = false
        focus = false
    }

    fun replay() {
        runs++
        running = true
    }

    fun finish() {
        running = false
    }
}

@Composable
internal fun rememberAutoplay(): Autoplay = remember { Autoplay() }

/** Runs [script] once per (re)play; it should start by resetting the demo, since Replay reruns it. */
@Composable
internal fun AutoplayEffect(autoplay: Autoplay, script: suspend () -> Unit) {
    LaunchedEffect(autoplay.runs, autoplay.running) {
        if (!autoplay.running) return@LaunchedEffect
        script()
        autoplay.finish()
    }
}

@Composable
internal fun SearchStage(
    number: Int,
    title: String,
    caption: String,
    autoplay: Autoplay,
    showReplay: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val focusManager = LocalFocusManager.current
    MenuStage(number, title, caption) {
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(autoplay) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        autoplay.stop()
                    }
                },
        ) {
            content()
            if (showReplay) {
                ReplayPill(Modifier.align(Alignment.TopEnd).padding(14.dp)) {
                    focusManager.clearFocus()
                    autoplay.replay()
                }
            }
        }
    }
}

/**
 * The dark pill every demo shares: magnifier, a real [BasicTextField] with a placeholder, and a
 * violet-to-blue border and halo while focused or [glowing]. [caret] draws a blinking caret
 * while the script types without real focus.
 */
@Composable
internal fun SearchField(
    query: Query,
    placeholder: String,
    modifier: Modifier = Modifier,
    glowing: Boolean = false,
    caret: Boolean = false,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    focusRequester: FocusRequester = remember { FocusRequester() },
    onSearch: (() -> Unit)? = null,
    placeholderDelayMillis: Int = 0,
    textAlpha: () -> Float = { 1f },
    onTextLayout: (TextLayoutResult) -> Unit = {},
    leading: @Composable () -> Unit = { MagnifierIcon(SearchColors.Muted) },
    trailing: @Composable RowScope.() -> Unit = {},
    textOverlay: @Composable BoxScope.() -> Unit = {},
) {
    val focusManager = LocalFocusManager.current
    val focused by interactionSource.collectIsFocusedAsState()
    val glow by animateFloatAsState(if (glowing || focused) 1f else 0f, tween(260), label = "glow")
    val empty = query.text.isEmpty()
    val placeholderAlpha by animateFloatAsState(
        if (empty) 1f else 0f,
        if (empty) tween(280, delayMillis = placeholderDelayMillis) else tween(90),
        label = "placeholder",
    )
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val blink = if (caret && !focused) rememberCaretBlink() else null

    Row(
        modifier
            .height(FieldHeight)
            .fieldChrome { glow }
            .padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading()
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f).graphicsLayer { alpha = textAlpha() }) {
            BasicTextField(
                value = query.value,
                onValueChange = { query.value = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .drawWithContent {
                        drawContent()
                        val visible = blink != null && blink.value < 0.5f
                        val result = layout
                        if (visible && result != null) {
                            val rect = result.getCursorRect(result.layoutInput.text.length)
                            val x = rect.left + 1.dp.toPx()
                            drawLine(SearchColors.Violet, Offset(x, rect.top), Offset(x, rect.bottom), 2.dp.toPx())
                        }
                    },
                textStyle = QueryStyle,
                cursorBrush = SolidColor(SearchColors.Violet),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch?.invoke() ?: focusManager.clearFocus() }),
                interactionSource = interactionSource,
                onTextLayout = {
                    layout = it
                    onTextLayout(it)
                },
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        Text(
                            placeholder,
                            Modifier.graphicsLayer { alpha = placeholderAlpha },
                            style = QueryStyle.copy(color = SearchColors.Placeholder),
                            maxLines = 1,
                            softWrap = false,
                        )
                        inner()
                        Box(Modifier.matchParentSize()) { textOverlay() }
                    }
                },
            )
        }
        trailing()
    }
}

private fun Modifier.fieldChrome(glow: () -> Float): Modifier = drawWithCache {
    val radius = size.height / 2
    val thin = Stroke(1.dp.toPx())
    val thick = Stroke(1.5.dp.toPx())
    val halo = 2.5.dp.toPx()
    onDrawBehind {
        val g = glow()
        if (g > 0f) {
            for (i in 1..4) {
                val out = halo * i
                drawRoundRect(
                    GlowBrush,
                    topLeft = Offset(-out, -out),
                    size = Size(size.width + out * 2, size.height + out * 2),
                    cornerRadius = CornerRadius(radius + out),
                    alpha = 0.1f * g * (5 - i) / 4f,
                )
            }
        }
        drawRoundRect(SearchColors.Field, cornerRadius = CornerRadius(radius))
        if (g < 1f) drawRoundRect(SearchColors.FieldBorder, cornerRadius = CornerRadius(radius), style = thin, alpha = 1f - g)
        if (g > 0f) drawRoundRect(GlowBrush, cornerRadius = CornerRadius(radius), style = thick, alpha = g)
    }
}

@Composable
private fun rememberCaretBlink() = rememberInfiniteTransition(label = "caret").animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing)),
    label = "caretBlink",
)

/** A × in a small disc that springs in while there is text to clear. */
@Composable
internal fun ClearButton(visible: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(
        if (visible) 1f else 0f,
        if (visible) spring(dampingRatio = 0.5f, stiffness = 600f) else tween(140),
        label = "clear",
    )
    Box(
        Modifier
            .size(36.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = scale.coerceIn(0f, 1f)
            }
            .clip(CircleShape)
            .clickable(enabled = visible, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(20.dp)) {
            drawCircle(SearchColors.RowLight)
            val stroke = 1.6.dp.toPx()
            val a = size.width * 0.34f
            val b = size.width * 0.66f
            drawLine(SearchColors.Text, Offset(a, a), Offset(b, b), stroke, StrokeCap.Round)
            drawLine(SearchColors.Text, Offset(b, a), Offset(a, b), stroke, StrokeCap.Round)
        }
    }
}

@Composable
internal fun MagnifierIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(20.dp)) { drawMagnifier(color) }
}

internal fun DrawScope.drawMagnifier(color: Color) {
    val w = size.minDimension
    val stroke = w * 0.11f
    val radius = w * 0.29f
    val c = Offset(w * 0.43f, w * 0.43f)
    drawCircle(color, radius, c, style = Stroke(stroke))
    val d = radius * 0.7071f + stroke * 0.4f
    drawLine(color, Offset(c.x + d, c.y + d), Offset(w * 0.86f, w * 0.86f), stroke, StrokeCap.Round)
}

internal fun DrawScope.drawArrowUpRight(color: Color) {
    val w = size.minDimension
    val stroke = w * 0.11f
    drawLine(color, Offset(w * 0.28f, w * 0.72f), Offset(w * 0.72f, w * 0.28f), stroke, StrokeCap.Round)
    drawLine(color, Offset(w * 0.36f, w * 0.28f), Offset(w * 0.72f, w * 0.28f), stroke, StrokeCap.Round)
    drawLine(color, Offset(w * 0.72f, w * 0.28f), Offset(w * 0.72f, w * 0.64f), stroke, StrokeCap.Round)
}

internal fun DrawScope.drawArrowRight(color: Color) {
    val w = size.minDimension
    val stroke = w * 0.11f
    drawLine(color, Offset(w * 0.2f, w * 0.5f), Offset(w * 0.8f, w * 0.5f), stroke, StrokeCap.Round)
    drawLine(color, Offset(w * 0.54f, w * 0.24f), Offset(w * 0.8f, w * 0.5f), stroke, StrokeCap.Round)
    drawLine(color, Offset(w * 0.8f, w * 0.5f), Offset(w * 0.54f, w * 0.76f), stroke, StrokeCap.Round)
}

internal fun DrawScope.drawClock(color: Color) {
    val w = size.minDimension
    val stroke = w * 0.11f
    drawCircle(color, w * 0.4f, center, style = Stroke(stroke))
    drawLine(color, center, Offset(center.x, w * 0.26f), stroke, StrokeCap.Round)
    drawLine(color, center, Offset(w * 0.68f, center.y), stroke, StrokeCap.Round)
}

@Composable
private fun ReplayPill(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier
            .clip(Pill)
            .background(Color.White.copy(alpha = 0.08f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(12.dp)) { drawReplayGlyph(SearchColors.Text) }
        Spacer(Modifier.width(6.dp))
        Text("Replay", color = SearchColors.Text, fontSize = 12.sp, fontWeight = FontWeight.Medium)
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
