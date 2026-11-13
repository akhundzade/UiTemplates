package com.azersun.uitemplates.demos.inputerror

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
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

internal object InputColors {
    val Panel = Color(0xFF221F44)
    val PanelBorder = Color(0xFF332F5E)
    val Field = Color(0xFF15132C)
    val FieldBorder = Color(0xFF3A3668)
    val Text = MenuColors.Text
    val Muted = MenuColors.Muted
    val Red = Color(0xFFF87171)
    val RedFlash = Color(0xFFFFDCDC)
    val Teal = Color(0xFF2DD4BF)
    val Ink = Color(0xFF14122B)
}

internal enum class FieldStatus { Neutral, Error, Valid }

internal val FieldCorner = 12.dp
internal val FieldShape = RoundedCornerShape(FieldCorner)
internal val HelperRowHeight = 30.dp
internal val ValueStyle = TextStyle(fontSize = 16.sp)

private val PanelShape = RoundedCornerShape(22.dp)
private val EmailPattern = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$")

@Stable
internal class EmailFormState {
    var value by mutableStateOf(TextFieldValue())
        private set
    var status by mutableStateOf(FieldStatus.Neutral)
        private set

    /** Bumped on every failed submit, so a demo replays its error animation even when already in error. */
    var errorTick by mutableIntStateOf(0)
        private set
    var submitTick by mutableIntStateOf(0)
        private set

    /** True while the scripted demo owns the field; any user input hands control back. */
    var autoplay by mutableStateOf(true)

    val text: String get() = value.text

    fun edit(new: TextFieldValue) {
        if (new.text != value.text) status = FieldStatus.Neutral
        value = new
    }

    fun submit() {
        submitTick++
        if (EmailPattern.matches(text.trim())) {
            status = FieldStatus.Valid
        } else {
            status = FieldStatus.Error
            errorTick++
        }
    }

    suspend fun play() {
        value = TextFieldValue()
        status = FieldStatus.Neutral
        delay(700)
        type("mia@orbi")
        delay(300)
        submit()
        delay(1600)
        type(".app")
        delay(300)
        submit()
    }

    private suspend fun type(chars: String) {
        chars.forEach { char ->
            delay(70)
            val next = text + char
            edit(TextFieldValue(next, TextRange(next.length)))
        }
    }
}

/** The shared card: email field, a fixed-height helper row and the Continue button, plus autoplay and Replay. */
@Composable
internal fun InputErrorStage(
    number: Int,
    title: String,
    caption: String,
    form: EmailFormState,
    helper: @Composable () -> Unit = { ErrorHelper(form.status == FieldStatus.Error) },
    field: @Composable () -> Unit,
) {
    var replays by remember { mutableIntStateOf(0) }
    val focusManager = LocalFocusManager.current
    LaunchedEffect(replays, form.autoplay) {
        if (form.autoplay) form.play()
    }

    MenuStage(number, title, caption) {
        Column(
            Modifier
                .widthIn(max = 400.dp)
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .background(InputColors.Panel, PanelShape)
                .border(1.dp, InputColors.PanelBorder, PanelShape)
                .padding(20.dp),
        ) {
            // Above the helper row so a message can hide behind the field.
            Box(Modifier.zIndex(1f)) { field() }
            Box(Modifier.fillMaxWidth().height(HelperRowHeight), contentAlignment = Alignment.CenterStart) { helper() }
            ContinueButton(form)
        }
        ReplayPill(Modifier.align(Alignment.TopEnd).padding(14.dp)) {
            focusManager.clearFocus()
            form.autoplay = true
            replays++
        }
    }
}

@Composable
internal fun EmailField(
    form: EmailFormState,
    modifier: Modifier = Modifier,
    borderColor: Color? = null,
    textColor: Color = InputColors.Text,
    trailing: @Composable () -> Unit = { StatusBadge(form.status) },
    textOverlay: @Composable (TextLayoutResult) -> Unit = {},
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    LaunchedEffect(focused) {
        if (focused) form.autoplay = false
    }
    val status = form.status
    val accent by animateColorAsState(
        when (status) {
            FieldStatus.Error -> InputColors.Red
            FieldStatus.Valid -> InputColors.Teal
            FieldStatus.Neutral -> if (focused) MenuColors.Violet else InputColors.FieldBorder
        },
        tween(220),
        label = "fieldAccent",
    )
    val label by animateColorAsState(
        when (status) {
            FieldStatus.Error -> InputColors.Red
            FieldStatus.Valid -> InputColors.Teal
            FieldStatus.Neutral -> InputColors.Muted
        },
        tween(220),
        label = "fieldLabel",
    )
    val tint by animateColorAsState(
        when (status) {
            FieldStatus.Error -> InputColors.Red.copy(alpha = 0.08f)
            FieldStatus.Valid -> InputColors.Teal.copy(alpha = 0.08f)
            FieldStatus.Neutral -> Color.Transparent
        },
        tween(220),
        label = "fieldTint",
    )
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    // The script types without focusing the field (that would raise the keyboard), so draw the caret ourselves.
    val blink = if (form.autoplay && !focused) rememberCaretBlink() else null

    Box(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(InputColors.Field, FieldShape)
            .background(tint, FieldShape)
            .border(1.5.dp, borderColor ?: accent, FieldShape)
            .padding(start = 16.dp, end = 14.dp),
    ) {
        Column(Modifier.align(Alignment.CenterStart).padding(end = 34.dp)) {
            Text("Email", color = label, fontSize = 11.sp, lineHeight = 13.sp, fontWeight = FontWeight.Medium)
            BasicTextField(
                value = form.value,
                onValueChange = {
                    if (it.text != form.text) form.autoplay = false
                    form.edit(it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp)
                    .drawWithContent {
                        drawContent()
                        val result = layout ?: return@drawWithContent
                        if (blink != null && blink.value < 0.5f) {
                            val caret = result.getCursorRect(result.layoutInput.text.length)
                            val x = caret.left + 1.dp.toPx()
                            drawLine(InputColors.Text, Offset(x, caret.top), Offset(x, caret.bottom), 2.dp.toPx())
                        }
                    },
                textStyle = ValueStyle.copy(color = textColor),
                cursorBrush = SolidColor(InputColors.Text),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    form.autoplay = false
                    form.submit()
                }),
                interactionSource = interaction,
                onTextLayout = { layout = it },
                decorationBox = { inner ->
                    Box {
                        inner()
                        layout?.let { textOverlay(it) }
                    }
                },
            )
        }
        Box(Modifier.align(Alignment.CenterEnd).size(22.dp)) { trailing() }
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

/** The trailing ! or ✓ circle, popping in on a spring and shrinking away when the field returns to neutral. */
@Composable
internal fun StatusBadge(status: FieldStatus) {
    val scale = remember { Animatable(0f) }
    var shown by remember { mutableStateOf(FieldStatus.Neutral) }
    LaunchedEffect(status) {
        if (status == FieldStatus.Neutral) {
            scale.animateTo(0f, tween(140))
        } else {
            shown = status
            scale.snapTo(0f)
            scale.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 600f))
        }
    }
    Canvas(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            },
    ) { drawStatusBadge(shown) }
}

internal fun DrawScope.drawStatusBadge(status: FieldStatus) {
    val color = when (status) {
        FieldStatus.Error -> InputColors.Red
        FieldStatus.Valid -> InputColors.Teal
        FieldStatus.Neutral -> return
    }
    val w = size.minDimension
    val stroke = w * 0.12f
    drawCircle(color)
    if (status == FieldStatus.Error) {
        drawLine(InputColors.Ink, Offset(w * 0.5f, w * 0.25f), Offset(w * 0.5f, w * 0.56f), stroke, StrokeCap.Round)
        drawCircle(InputColors.Ink, stroke * 0.65f, Offset(w * 0.5f, w * 0.75f))
    } else {
        val check = Path().apply {
            moveTo(w * 0.29f, w * 0.52f)
            lineTo(w * 0.44f, w * 0.66f)
            lineTo(w * 0.72f, w * 0.37f)
        }
        drawPath(check, InputColors.Ink, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
internal fun HelperText(modifier: Modifier = Modifier) {
    Text(
        "Enter a valid email",
        color = InputColors.Red,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        modifier = modifier.padding(start = 4.dp),
    )
}

@Composable
private fun ErrorHelper(visible: Boolean) {
    val shown by animateFloatAsState(if (visible) 1f else 0f, tween(200), label = "helper")
    HelperText(
        Modifier.graphicsLayer {
            alpha = shown
            translationY = (1f - shown) * -6.dp.toPx()
        },
    )
}

@Composable
private fun ContinueButton(form: EmailFormState) {
    val press = remember { Animatable(1f) }
    LaunchedEffect(form.submitTick) {
        if (form.submitTick == 0) return@LaunchedEffect
        press.animateTo(0.95f, tween(70))
        press.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 700f))
    }
    Box(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .graphicsLayer {
                scaleX = press.value
                scaleY = press.value
            }
            .clip(RoundedCornerShape(50))
            .background(Brush.horizontalGradient(listOf(MenuColors.Violet, MenuColors.Cyan)))
            .clickable {
                form.autoplay = false
                form.submit()
            },
        contentAlignment = Alignment.Center,
    ) {
        Text("Continue →", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
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
        Canvas(Modifier.size(12.dp)) { drawReplayGlyph(InputColors.Text) }
        Spacer(Modifier.width(6.dp))
        Text("Replay", color = InputColors.Text, fontSize = 12.sp, fontWeight = FontWeight.Medium)
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
