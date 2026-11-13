package com.azersun.uitemplates.demos.textfield

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azersun.uitemplates.demos.menus.MenuColors
import com.azersun.uitemplates.demos.menus.MenuStage
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal object FieldColors {
    val Panel = Color(0xFF221F44)
    val PanelBorder = Color(0xFF332F5E)
    val Field = Color(0xFF15132C)
    val Border = Color(0xFF3A3668)
    val Text = MenuColors.Text
    val Muted = MenuColors.Muted
    val Focus = MenuColors.Violet
    val Error = Color(0xFFFF5C7A)
    val Valid = Color(0xFF2DD4BF)
    val Amber = Color(0xFFFBBF24)
    val Ink = Color(0xFF14122B)
}

internal val ValueStyle = TextStyle(color = FieldColors.Text, fontSize = 16.sp)
internal val FieldShape = RoundedCornerShape(12.dp)

private const val AutoplayDelayMillis = 700L
private const val TypeMillis = 80L
private val PanelShape = RoundedCornerShape(22.dp)

@Stable
internal class TypingState(private val transform: (String) -> String = { it }) {
    var value by mutableStateOf(TextFieldValue())
        private set

    /** True while the script owns the field; any real touch hands it back until Replay. */
    var autoplay by mutableStateOf(true)
        private set

    /** The script types without focusing the real field (that would raise the keyboard), so it fakes focus. */
    var scriptFocus by mutableStateOf(false)
        private set

    val text: String get() = value.text

    fun edit(new: TextFieldValue) {
        value = new.copy(text = transform(new.text))
    }

    fun stopAutoplay() {
        autoplay = false
        scriptFocus = false
    }

    fun restart() {
        value = TextFieldValue()
        scriptFocus = false
        autoplay = true
    }

    fun focus() {
        scriptFocus = true
    }

    suspend fun type(chars: String) {
        chars.forEach { char ->
            delay(TypeMillis)
            val next = text + char
            edit(TextFieldValue(next, TextRange(next.length)))
        }
    }
}

/**
 * The shared stage: a panel holding [content], the autoplay [script] (started 700 ms after opening and
 * stopped by any touch) and a Replay pill that clears the field and runs the script again.
 */
@Composable
internal fun TextFieldStage(
    number: Int,
    title: String,
    caption: String,
    state: TypingState,
    onReplay: () -> Unit = {},
    script: suspend () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    var replays by remember { mutableIntStateOf(0) }
    val focusManager = LocalFocusManager.current
    val currentScript by rememberUpdatedState(script)
    LaunchedEffect(replays, state.autoplay) {
        if (!state.autoplay) return@LaunchedEffect
        delay(AutoplayDelayMillis)
        currentScript()
    }

    MenuStage(number, title, caption) {
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(state) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        state.stopAutoplay()
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Column(
                Modifier
                    .widthIn(max = 400.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
                    .background(FieldColors.Panel, PanelShape)
                    .border(1.dp, FieldColors.PanelBorder, PanelShape)
                    .padding(20.dp),
                content = content,
            )
            ReplayPill(Modifier.align(Alignment.TopEnd).padding(14.dp)) {
                focusManager.clearFocus()
                onReplay()
                state.restart()
                replays++
            }
        }
    }
}

/**
 * A [BasicTextField] bound to [state] that hands control back from the script on focus and draws a
 * blinking caret while the script is typing. [decorationBox] receives the text with that caret attached,
 * so the caret lines up however the decoration pads it.
 */
@Composable
internal fun ScriptedTextField(
    state: TypingState,
    interaction: MutableInteractionSource,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = ValueStyle,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    decorationBox: @Composable (innerTextField: @Composable () -> Unit) -> Unit = { it() },
    onTextLayout: (TextLayoutResult) -> Unit = {},
) {
    val focused by interaction.collectIsFocusedAsState()
    LaunchedEffect(focused) {
        if (focused) state.stopAutoplay()
    }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val blink = if (state.scriptFocus && !focused) rememberCaretBlink() else null

    BasicTextField(
        value = state.value,
        onValueChange = state::edit,
        modifier = modifier,
        textStyle = textStyle,
        cursorBrush = SolidColor(FieldColors.Text),
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        interactionSource = interaction,
        onTextLayout = {
            layout = it
            onTextLayout(it)
        },
        decorationBox = { inner ->
            decorationBox {
                Box(
                    Modifier.drawWithContent {
                        drawContent()
                        val visible = blink != null && blink.value < 0.5f
                        val result = layout
                        if (!visible || result == null) return@drawWithContent
                        val end = state.value.selection.end.coerceIn(0, result.layoutInput.text.length)
                        val caret = result.getCursorRect(end)
                        val x = caret.left + 1.dp.toPx()
                        drawLine(FieldColors.Text, Offset(x, caret.top), Offset(x, caret.bottom), 2.dp.toPx())
                    },
                ) { inner() }
            }
        },
    )
}

@Composable
private fun rememberCaretBlink(): State<Float> =
    rememberInfiniteTransition(label = "caret").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing)),
        label = "caretBlink",
    )

@Composable
internal fun FieldLabel(text: String, color: Color = FieldColors.Muted) {
    Text(
        text,
        color = color,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
    )
}

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
        Canvas(Modifier.size(12.dp)) { drawReplayGlyph(FieldColors.Text) }
        Spacer(Modifier.width(6.dp))
        Text("Replay", color = FieldColors.Text, fontSize = 12.sp, fontWeight = FontWeight.Medium)
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
