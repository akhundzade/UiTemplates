package com.azersun.uitemplates.demos.textfield

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos

private const val FlipMillis = 260
private const val FlipStaggerMillis = 40L

// Monospace keeps every dot and letter in the same slot, so the hidden text sits evenly spaced and the
// real (transparent) glyphs still place the cursor and selection exactly where they're drawn.
private val SecretStyle = ValueStyle.copy(fontFamily = FontFamily.Monospace, fontSize = 18.sp, letterSpacing = 1.sp)

@Composable
fun PasswordRevealDemo() {
    val state = remember { TypingState() }
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val active = focused || state.scriptFocus
    val scope = rememberCoroutineScope()
    var shown by remember { mutableStateOf(false) }
    // One flip per character: 0 is a dot, 1 is the letter.
    val flips = remember { mutableStateListOf<Animatable<Float, AnimationVector1D>>() }
    var flipJob by remember { mutableStateOf<Job?>(null) }
    val eyePress = remember { Animatable(1f) }
    val measurer = rememberTextMeasurer()
    val glyphs = remember { HashMap<Char, TextLayoutResult>() }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }

    LaunchedEffect(state.text.length) {
        while (flips.size < state.text.length) flips += Animatable(if (shown) 1f else 0f)
    }

    fun toggle() {
        shown = !shown
        val target = if (shown) 1f else 0f
        val count = state.text.length
        flipJob?.cancel()
        flipJob = scope.launch {
            flips.toList().forEachIndexed { i, flip ->
                if (i >= count) {
                    flip.snapTo(target)
                } else {
                    launch {
                        delay(i * FlipStaggerMillis)
                        flip.animateTo(target, tween(FlipMillis, easing = FastOutSlowInEasing))
                    }
                }
            }
        }
        scope.launch {
            eyePress.animateTo(0.82f, tween(80))
            eyePress.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 600f))
        }
    }

    val outline by animateColorAsState(if (active) FieldColors.Focus else FieldColors.Border, tween(220), label = "outline")
    val slash by animateFloatAsState(if (shown) 0f else 1f, tween(220), label = "slash")

    TextFieldStage(
        5,
        "Password Reveal",
        "Tap the eye and each dot flips over into its letter, left to right, then back.",
        state,
        onReplay = {
            flipJob?.cancel()
            shown = false
            scope.launch { flips.forEach { it.snapTo(0f) } }
        },
        script = {
            state.focus()
            delay(400)
            state.type("sunset42")
            delay(600)
            toggle()
            delay(1500)
            toggle()
        },
    ) {
        FieldLabel("Password")
        Row(
            Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(FieldColors.Field, FieldShape)
                .border(1.5.dp, outline, FieldShape),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ScriptedTextField(
                state,
                interaction,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                textStyle = SecretStyle.copy(color = Color.Transparent),
                keyboardOptions = KeyboardOptions(
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                onTextLayout = { layout = it },
                decorationBox = { inner ->
                    Box(Modifier.fillMaxHeight().padding(start = 16.dp, end = 8.dp), contentAlignment = Alignment.CenterStart) {
                        Box(
                            Modifier.drawWithContent {
                                drawContent()
                                val result = layout ?: return@drawWithContent
                                val text = result.layoutInput.text.text
                                val fallback = if (shown) 1f else 0f
                                val dotRadius = 3.5.dp.toPx()
                                val dotY = result.firstBaseline - SecretStyle.fontSize.toPx() * 0.3f
                                for (i in text.indices) {
                                    val flip = flips.getOrNull(i)?.value ?: fallback
                                    val box = result.getBoundingBox(i)
                                    scale(1f, abs(cos(flip * PI.toFloat())), Offset(box.center.x, dotY)) {
                                        if (flip < 0.5f) {
                                            drawCircle(FieldColors.Text, dotRadius, Offset(box.center.x, dotY))
                                        } else {
                                            val glyph = glyphs.getOrPut(text[i]) { measurer.measure(text[i].toString(), SecretStyle) }
                                            drawText(glyph, topLeft = box.topLeft)
                                        }
                                    }
                                }
                            },
                        ) { inner() }
                    }
                },
            )
            Column(
                Modifier
                    .padding(end = 6.dp)
                    .width(52.dp)
                    .fillMaxHeight()
                    .clickable(onClickLabel = if (shown) "Hide password" else "Show password", role = Role.Button) { toggle() }
                    .graphicsLayer {
                        scaleX = eyePress.value
                        scaleY = eyePress.value
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Canvas(Modifier.size(22.dp)) { drawEye(if (shown) FieldColors.Text else FieldColors.Muted, slash) }
                Text(
                    if (shown) "Hide" else "Show",
                    color = FieldColors.Muted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

/** An eye with a slash across it drawn [slash] of the way. */
private fun DrawScope.drawEye(color: Color, slash: Float) {
    val w = size.width
    val h = size.height
    val stroke = Stroke(w * 0.08f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val lid = Path().apply {
        moveTo(w * 0.06f, h * 0.5f)
        quadraticTo(w * 0.5f, h * 0.06f, w * 0.94f, h * 0.5f)
        quadraticTo(w * 0.5f, h * 0.94f, w * 0.06f, h * 0.5f)
        close()
    }
    drawPath(lid, color, style = stroke)
    drawCircle(color, w * 0.13f, Offset(w * 0.5f, h * 0.5f))
    if (slash > 0f) {
        val from = Offset(w * 0.14f, h * 0.14f)
        val to = Offset(w * 0.86f, h * 0.86f)
        drawLine(color, from, from + (to - from) * slash, w * 0.08f, StrokeCap.Round)
    }
}
