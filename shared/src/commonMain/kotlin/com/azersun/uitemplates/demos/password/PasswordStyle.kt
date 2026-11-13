package com.azersun.uitemplates.demos.password

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.azersun.uitemplates.demos.menus.MenuColors
import com.azersun.uitemplates.demos.menus.MenuStage
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal object PasswordColors {
    val Panel = Color(0xFF221F44)
    val PanelBorder = Color(0xFF332F5E)
    val Field = Color(0xFF15132C)
    val FieldBorder = Color(0xFF3A3668)
    val Track = Color(0xFF2E2A57)
    val Text = MenuColors.Text
    val Muted = MenuColors.Muted
    val FlashWhite = Color(0xFFF4F6FF)
}

internal class StrengthLevel(val word: String, val hint: String, val color: Color)

private val Weak = Color(0xFFF87171)

/** Index 0 is the empty password; it keeps Weak's color so a draining fill doesn't fade to grey. */
internal val StrengthLevels = listOf(
    StrengthLevel("", "", Weak),
    StrengthLevel("Weak", "Add more characters", Weak),
    StrengthLevel("Fair", "Add a number", Color(0xFFFBBF24)),
    StrengthLevel("Good", "Add a symbol", Color(0xFF22D3EE)),
    StrengthLevel("Strong", "Great password", Color(0xFF2DD4BF)),
)

internal const val SegmentCount = 4
internal val SegmentHeight = 6.dp
internal val SegmentGap = 6.dp

internal fun strengthOf(password: String): Int {
    if (password.isEmpty()) return 0
    if (password.length < 8) return 1
    val digit = password.any { it.isDigit() }
    val symbol = password.any { !it.isLetterOrDigit() }
    return when {
        digit && symbol && password.length >= 10 -> 4
        digit -> 3
        else -> 2
    }
}

private val PanelShape = RoundedCornerShape(22.dp)
private val FieldShape = RoundedCornerShape(14.dp)
private val DotStyle = TextStyle(color = Color.White, fontSize = 14.sp, letterSpacing = 3.sp)
private val ScriptChunks = listOf("orb", "itmug", "26", "!")

@Stable
internal class PasswordFormState {
    var value by mutableStateOf(TextFieldValue())
        private set

    /** True while the scripted demo owns the field; any user input hands control back. */
    var autoplay by mutableStateOf(true)

    val text: String get() = value.text
    val level: Int by derivedStateOf { strengthOf(value.text) }

    fun edit(new: TextFieldValue) {
        value = new
    }

    suspend fun play() {
        value = TextFieldValue()
        delay(700)
        ScriptChunks.forEachIndexed { i, chunk ->
            if (i > 0) delay(550)
            chunk.forEach { char ->
                delay(110)
                val next = text + char
                value = TextFieldValue(next, TextRange(next.length))
            }
        }
    }
}

/** The shared card: label, password field, the strength bar slot and the word/hint row, plus autoplay and Replay. */
@Composable
internal fun PasswordStage(
    number: Int,
    title: String,
    caption: String,
    form: PasswordFormState,
    fieldTextModifier: Modifier = Modifier,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    label: @Composable () -> Unit = { StrengthLabel(form.level) },
    overlay: @Composable BoxScope.() -> Unit = {},
    bar: @Composable () -> Unit,
) {
    var replays by remember { mutableIntStateOf(0) }
    val focusManager = LocalFocusManager.current
    LaunchedEffect(replays, form.autoplay) {
        if (form.autoplay) form.play()
    }

    MenuStage(number, title, caption) {
        Box(
            Modifier
                .widthIn(max = 400.dp)
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .background(PasswordColors.Panel, PanelShape)
                .border(1.dp, PasswordColors.PanelBorder, PanelShape),
        ) {
            Column(Modifier.fillMaxWidth().padding(20.dp)) {
                Text("Password", color = PasswordColors.Muted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                // Above the bar so segments can emerge from behind the field.
                Box(Modifier.zIndex(1f)) { PasswordField(form, fieldTextModifier, onTextLayout) }
                Spacer(Modifier.height(18.dp))
                Box(Modifier.fillMaxWidth().height(SegmentHeight)) { bar() }
                Spacer(Modifier.height(12.dp))
                label()
            }
            overlay()
        }
        ReplayPill(Modifier.align(Alignment.TopEnd).padding(14.dp)) {
            focusManager.clearFocus()
            form.autoplay = true
            replays++
        }
    }
}

@Composable
private fun PasswordField(form: PasswordFormState, textModifier: Modifier, onTextLayout: (TextLayoutResult) -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    LaunchedEffect(focused) {
        if (focused) form.autoplay = false
    }
    // The script types without focusing the field (that would raise the keyboard), so it gets the focused look.
    val active = focused || form.autoplay
    val glow by animateFloatAsState(if (active) 1f else 0f, tween(260), label = "fieldGlow")
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val blink = if (form.autoplay && !focused) rememberCaretBlink() else null

    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .drawBehind {
                repeat(4) { k ->
                    val spread = (k + 1) * 2.dp.toPx()
                    drawRoundRect(
                        MenuColors.Violet.copy(alpha = 0.07f * glow),
                        topLeft = Offset(-spread, -spread),
                        size = Size(size.width + spread * 2, size.height + spread * 2),
                        cornerRadius = CornerRadius(14.dp.toPx() + spread),
                    )
                }
            }
            .background(PasswordColors.Field, FieldShape)
            .border(1.5.dp, PasswordColors.FieldBorder, FieldShape)
            .border(
                1.5.dp,
                Brush.horizontalGradient(listOf(MenuColors.Violet.copy(alpha = glow), MenuColors.Blue.copy(alpha = glow))),
                FieldShape,
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val lockColor = lerp(PasswordColors.Muted, Color(0xFFB7A6FF), glow)
        Canvas(Modifier.size(18.dp)) { drawLock(lockColor) }
        Spacer(Modifier.width(12.dp))
        BasicTextField(
            value = form.value,
            onValueChange = {
                if (it.text != form.text) form.autoplay = false
                form.edit(it)
            },
            modifier = Modifier
                .weight(1f)
                .then(textModifier)
                .drawWithContent {
                    drawContent()
                    val result = layout ?: return@drawWithContent
                    if (blink != null && blink.value < 0.5f) {
                        val caret = result.getCursorRect(result.layoutInput.text.length)
                        val x = caret.left + 1.dp.toPx()
                        drawLine(PasswordColors.Text, Offset(x, caret.top), Offset(x, caret.bottom), 2.dp.toPx())
                    }
                },
            textStyle = DotStyle,
            cursorBrush = SolidColor(PasswordColors.Text),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation('●'),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { form.autoplay = false }),
            interactionSource = interaction,
            onTextLayout = {
                layout = it
                onTextLayout(it)
            },
        )
    }
}

@Composable
private fun rememberCaretBlink(): State<Float> =
    rememberInfiniteTransition(label = "caret").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing)),
        label = "caretBlink",
    )

private fun DrawScope.drawLock(color: Color) {
    val w = size.width
    val stroke = w * 0.11f
    drawArc(
        color,
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(w * 0.28f, w * 0.1f),
        size = Size(w * 0.44f, w * 0.5f),
        style = Stroke(stroke),
    )
    drawLine(color, Offset(w * 0.28f, w * 0.35f), Offset(w * 0.28f, w * 0.48f), stroke)
    drawLine(color, Offset(w * 0.72f, w * 0.35f), Offset(w * 0.72f, w * 0.48f), stroke)
    drawRoundRect(color, Offset(w * 0.14f, w * 0.44f), Size(w * 0.72f, w * 0.5f), CornerRadius(w * 0.12f))
    drawCircle(PasswordColors.Field, w * 0.07f, Offset(w * 0.5f, w * 0.65f))
    drawLine(PasswordColors.Field, Offset(w * 0.5f, w * 0.65f), Offset(w * 0.5f, w * 0.8f), w * 0.08f, StrokeCap.Round)
}

/** Word on the left in the level's color, hint on the right. */
@Composable
internal fun StrengthLabel(
    level: Int,
    word: @Composable () -> Unit = {
        val color by animateColorAsState(StrengthLevels[level].color, tween(350), label = "word")
        Text(StrengthLevels[level].word, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    },
) {
    Row(Modifier.fillMaxWidth().heightIn(min = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        word()
        Spacer(Modifier.weight(1f))
        Text(StrengthLevels[level].hint, color = PasswordColors.Muted, fontSize = 12.sp)
    }
}

@Composable
internal fun animateLevelColor(level: Int, durationMillis: Int = 350): State<Color> =
    animateColorAsState(StrengthLevels[level].color, tween(durationMillis), label = "levelColor")

internal fun DrawScope.segmentWidth(): Float = (size.width - SegmentGap.toPx() * (SegmentCount - 1)) / SegmentCount

internal fun DrawScope.segmentLeft(index: Int): Float = index * (segmentWidth() + SegmentGap.toPx())

internal fun DrawScope.drawSegment(index: Int, color: Color, alpha: Float = 1f) {
    drawRoundRect(
        color,
        topLeft = Offset(segmentLeft(index), 0f),
        size = Size(segmentWidth(), size.height),
        cornerRadius = CornerRadius(size.height / 2),
        alpha = alpha,
    )
}

internal fun DrawScope.drawTrack() = repeat(SegmentCount) { drawSegment(it, PasswordColors.Track) }

internal fun DrawScope.segmentsPath(): Path = Path().apply {
    val radius = CornerRadius(size.height / 2)
    repeat(SegmentCount) { i ->
        val left = segmentLeft(i)
        addRoundRect(RoundRect(left, 0f, left + segmentWidth(), size.height, radius))
    }
}

/**
 * Pixel end of a continuous fill measured in segments. Past the first segment it is linear across segments
 * and gaps alike, so a moving fill crosses each gap at the same speed instead of jumping it.
 */
internal fun fillEndPx(width: Float, gap: Float, fill: Float): Float {
    val segment = (width - gap * (SegmentCount - 1)) / SegmentCount
    return if (fill <= 1f) fill * segment else fill * (segment + gap) - gap
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
        Canvas(Modifier.size(12.dp)) { drawReplayGlyph(PasswordColors.Text) }
        Spacer(Modifier.width(6.dp))
        Text("Replay", color = PasswordColors.Text, fontSize = 12.sp, fontWeight = FontWeight.Medium)
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
