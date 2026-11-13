package com.azersun.uitemplates.demos.otp

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.azersun.uitemplates.demos.menus.MenuColors
import com.azersun.uitemplates.demos.menus.MenuStage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max

internal const val CodeLength = 6
private const val AutoplayCode = "274851"

internal val BoxCorner = 10.dp

internal object OtpColors {
    val BoxFill = Color(0xFF14122B)
    val BoxBorder = MenuColors.CardBorder
    val Focus = Color(0xFF7B83FF)
    val Caret = Color(0xFFC3C6FF)
    val Teal = Color(0xFF2DD4BF)
    val Mint = Color(0xFF6EE7C8)
    val SuccessFill = Color(0xFF0D3634)
    val OnTeal = Color(0xFF053B36)
}

private val DisabledButton = Color(0xFF2A2450)

/** Box size and spacing, shrunk on narrow screens so all six boxes always fit. */
@Stable
internal class OtpMetrics(val boxWidth: Dp, val boxHeight: Dp, val gap: Dp) {
    val rowWidth: Dp get() = boxWidth * CodeLength + gap * (CodeLength - 1)

    fun boxStart(index: Int): Dp = (boxWidth + gap) * index
}

@Stable
internal class OtpState {
    var code by mutableStateOf("")
        private set
    var verified by mutableStateOf(false)
        private set

    val complete: Boolean get() = code.length == CodeLength

    /** The next empty box, or -1 once the code is full or verified. */
    val activeIndex: Int get() = if (verified || complete) -1 else code.length

    fun input(text: String) {
        if (!verified) code = text.filter { it in '0'..'9' }.take(CodeLength)
    }

    fun verify() {
        if (complete) verified = true
    }

    fun reset() {
        code = ""
        verified = false
    }
}

/**
 * The verification card all six demos share: prompt, boxes, Verify button, the hidden text
 * field that takes real keyboard input, the reel's autoplay and a Replay pill.
 */
@Composable
internal fun OtpScene(
    number: Int,
    title: String,
    caption: String,
    state: OtpState,
    boxes: @Composable (OtpMetrics) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val press = remember { Animatable(1f) }
    var run by remember { mutableIntStateOf(0) }
    var userTyped by remember { mutableStateOf(false) }

    fun submit() {
        if (!state.complete || state.verified) return
        focusManager.clearFocus()
        scope.launch {
            press.animateTo(0.95f, tween(90))
            state.verify()
            press.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 600f))
        }
    }

    LaunchedEffect(run) {
        state.reset()
        delay(700)
        AutoplayCode.forEachIndexed { i, digit ->
            if (i > 0) delay(280)
            if (userTyped) return@LaunchedEffect
            state.input(state.code + digit)
        }
        delay(500)
        if (!userTyped) submit()
    }

    MenuStage(number, title, caption) {
        Column(
            Modifier
                .padding(horizontal = 14.dp)
                .widthIn(max = 380.dp)
                .fillMaxWidth()
                .background(MenuColors.Tile, RoundedCornerShape(24.dp))
                .border(1.dp, MenuColors.CardBorder, RoundedCornerShape(24.dp))
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Enter the code we sent", color = MenuColors.Muted, fontSize = 14.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(18.dp))
            BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                val gap = 8.dp
                val boxWidth = min(44.dp, (maxWidth - gap * (CodeLength - 1)) / CodeLength)
                val metrics = remember(boxWidth) { OtpMetrics(boxWidth, boxWidth * (52f / 44f), gap) }
                Box(
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = !state.verified,
                        onClickLabel = "Enter code",
                    ) {
                        focusRequester.requestFocus()
                        keyboard?.show()
                    },
                ) {
                    BasicTextField(
                        value = TextFieldValue(state.code, TextRange(state.code.length)),
                        onValueChange = {
                            if (it.text != state.code && !state.verified) {
                                userTyped = true
                                state.input(it.text)
                            }
                        },
                        modifier = Modifier.size(1.dp).alpha(0f).focusRequester(focusRequester),
                        enabled = !state.verified,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submit() }),
                    )
                    boxes(metrics)
                }
            }
            Spacer(Modifier.height(24.dp))
            VerifyButton(state, scale = { press.value }, onClick = ::submit)
        }
        ReplayPill(
            onClick = {
                focusManager.clearFocus()
                userTyped = false
                run++
            },
            modifier = Modifier.align(Alignment.TopEnd).padding(14.dp),
        )
    }
}

@Composable
private fun VerifyButton(state: OtpState, scale: () -> Float, onClick: () -> Unit) {
    val ready = state.complete && !state.verified
    val gradient by animateFloatAsState(if (ready) 1f else 0f, tween(260), label = "ready")
    val base by animateColorAsState(if (state.verified) OtpColors.Teal else DisabledButton, tween(320), label = "base")
    val label by animateColorAsState(
        when {
            state.verified -> OtpColors.OnTeal
            ready -> Color.White
            else -> MenuColors.Muted
        },
        tween(260),
        label = "label",
    )
    Box(
        Modifier
            .fillMaxWidth()
            .height(50.dp)
            .graphicsLayer {
                scaleX = scale()
                scaleY = scale()
            }
            .clip(CircleShape)
            .drawBehind {
                drawRect(base)
                drawRect(Brush.horizontalGradient(listOf(MenuColors.Violet, MenuColors.Cyan)), alpha = gradient)
            }
            .clickable(enabled = ready, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = state.verified,
            transitionSpec = { (fadeIn(tween(220)) + scaleIn(tween(220), initialScale = 0.85f)).togetherWith(fadeOut(tween(120))) },
            label = "verified",
        ) { verified ->
            if (verified) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(Modifier.size(16.dp)) { drawCheck(label) }
                    Text("Verified", color = label, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
                }
            } else {
                Text("Verify", color = label, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ReplayPill(onClick: () -> Unit, modifier: Modifier) {
    Row(
        modifier
            .clip(CircleShape)
            .background(Color(0x22FFFFFF))
            .clickable(onClickLabel = "Replay", onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(12.dp)) {
            val stroke = 1.6.dp.toPx()
            val radius = size.minDimension / 2 - stroke
            drawArc(
                MenuColors.Text,
                startAngle = 0f,
                sweepAngle = 300f,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
            // Arrowhead at the arc's end (300°), pointing along the clockwise tangent.
            val end = center + Offset(radius * 0.5f, -radius * 0.866f)
            val along = Offset(0.866f, 0.5f)
            val across = Offset(0.5f, -0.866f)
            val head = 3.dp.toPx()
            val arrow = Path().apply {
                moveTo(end.x + along.x * head, end.y + along.y * head)
                lineTo(end.x + across.x * head * 0.9f, end.y + across.y * head * 0.9f)
                lineTo(end.x - across.x * head * 0.9f, end.y - across.y * head * 0.9f)
                close()
            }
            drawPath(arrow, MenuColors.Text)
        }
        Text("Replay", color = MenuColors.Text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 6.dp))
    }
}

internal fun DrawScope.drawCheck(color: Color, strokeWidth: Float = size.width * 0.16f) {
    val w = size.width
    val path = Path().apply {
        moveTo(w * 0.18f, w * 0.52f)
        lineTo(w * 0.42f, w * 0.74f)
        lineTo(w * 0.82f, w * 0.28f)
    }
    drawPath(path, color, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

/**
 * One code box: dark fill, subtle border, a focus outline while it is the next box to fill and a
 * blinking caret. [success] (0–1, read at draw time) blends it into the teal success state.
 */
@Composable
internal fun OtpBox(
    metrics: OtpMetrics,
    active: Boolean,
    modifier: Modifier = Modifier,
    success: () -> Float = { 0f },
    showFocus: Boolean = true,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val focus by animateFloatAsState(if (active && showFocus) 1f else 0f, tween(160), label = "focus")
    Box(
        modifier
            .size(metrics.boxWidth, metrics.boxHeight)
            .drawBehind {
                val done = success().coerceIn(0f, 1f)
                val corner = BoxCorner.toPx()
                if (focus > 0f) {
                    val glow = 3.dp.toPx()
                    drawRoundRect(
                        OtpColors.Focus.copy(alpha = 0.22f * focus),
                        topLeft = Offset(-glow, -glow),
                        size = Size(size.width + glow * 2, size.height + glow * 2),
                        cornerRadius = CornerRadius(corner + glow),
                        style = Stroke(glow),
                    )
                }
                drawRoundRect(lerp(OtpColors.BoxFill, OtpColors.SuccessFill, done), cornerRadius = CornerRadius(corner))
                val stroke = lerp(1.dp, 2.dp, max(focus, done)).toPx()
                drawRoundRect(
                    lerp(lerp(OtpColors.BoxBorder, OtpColors.Focus, focus), OtpColors.Teal, done),
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(size.width - stroke, size.height - stroke),
                    cornerRadius = CornerRadius(corner - stroke / 2),
                    style = Stroke(stroke),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        content()
        if (active) BlinkingCaret()
    }
}

@Composable
internal fun OtpDigit(digit: Char, modifier: Modifier = Modifier) {
    Text(digit.toString(), modifier, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
}

@Composable
internal fun BlinkingCaret(modifier: Modifier = Modifier) {
    val phase by rememberInfiniteTransition(label = "caret").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1060, easing = LinearEasing)),
        label = "blink",
    )
    Box(
        modifier
            .size(2.dp, 22.dp)
            .graphicsLayer { alpha = if (phase < 0.5f) 1f else 0f }
            .background(OtpColors.Caret, RoundedCornerShape(1.dp)),
    )
}

/** A digit that pops in from small on a bouncy spring. */
@Composable
internal fun PopDigit(digit: Char) {
    val pop = remember { Animatable(0.3f) }
    LaunchedEffect(Unit) { pop.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 500f)) }
    OtpDigit(
        digit,
        Modifier.graphicsLayer {
            scaleX = pop.value
            scaleY = pop.value
            alpha = ((pop.value - 0.3f) / 0.5f).coerceIn(0f, 1f)
        },
    )
}
