package com.azersun.uitemplates.demos.textfield

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private enum class Validity { Empty, Invalid, Valid }

private val EmailPattern = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$")

private fun validityOf(text: String): Validity = when {
    text.isEmpty() -> Validity.Empty
    EmailPattern.matches(text.trim()) -> Validity.Valid
    else -> Validity.Invalid
}

private fun hintFor(text: String): String =
    if ('@' !in text) "Include an @, like mia@mail.com" else "Add a domain, like .com"

@Composable
fun LiveValidationDemo() {
    val state = remember { TypingState() }
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val active = focused || state.scriptFocus
    val validity = validityOf(state.text)
    val accent by animateColorAsState(
        when (validity) {
            Validity.Invalid -> FieldColors.Error
            Validity.Valid -> FieldColors.Valid
            Validity.Empty -> if (active) FieldColors.Focus else FieldColors.Border
        },
        tween(200),
        label = "accent",
    )
    val tint by animateColorAsState(
        when (validity) {
            Validity.Invalid -> FieldColors.Error.copy(alpha = 0.07f)
            Validity.Valid -> FieldColors.Valid.copy(alpha = 0.07f)
            Validity.Empty -> Color.Transparent
        },
        tween(200),
        label = "tint",
    )
    val labelColor by animateColorAsState(
        when (validity) {
            Validity.Invalid -> FieldColors.Error
            Validity.Valid -> FieldColors.Valid
            Validity.Empty -> FieldColors.Muted
        },
        tween(200),
        label = "label",
    )
    // Keep the last hint while it fades out, so it doesn't blank mid-fade.
    var hint by remember { mutableStateOf("") }
    if (validity == Validity.Invalid) hint = hintFor(state.text)
    val hintShown by animateFloatAsState(if (validity == Validity.Invalid) 1f else 0f, tween(200), label = "hint")

    TextFieldStage(
        4,
        "Live Validation",
        "The field checks as you type: pink with a hint while invalid, teal with a ✓ once it's a real email.",
        state,
        script = {
            state.focus()
            delay(400)
            state.type("mia@mail")
            delay(900)
            state.type(".com")
        },
    ) {
        FieldLabel("Email", labelColor)
        Row(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(FieldColors.Field, FieldShape)
                .background(tint, FieldShape)
                .border(1.5.dp, accent, FieldShape)
                .padding(end = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ScriptedTextField(
                state,
                interaction,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                keyboardOptions = KeyboardOptions(
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Done,
                ),
                decorationBox = { inner ->
                    Box(Modifier.fillMaxHeight().padding(start = 16.dp, end = 10.dp), contentAlignment = Alignment.CenterStart) {
                        inner()
                    }
                },
            )
            ValidityBadge(validity, Modifier.size(22.dp))
        }
        Box(Modifier.fillMaxWidth().height(28.dp), contentAlignment = Alignment.CenterStart) {
            Text(
                hint,
                color = FieldColors.Error,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .graphicsLayer {
                        alpha = hintShown
                        translationY = (1f - hintShown) * -5.dp.toPx()
                    },
            )
        }
    }
}

/** The trailing ! or ✓, popping in on a spring each time the verdict flips and shrinking away when cleared. */
@Composable
private fun ValidityBadge(validity: Validity, modifier: Modifier = Modifier) {
    val scale = remember { Animatable(0f) }
    var shown by remember { mutableStateOf(Validity.Empty) }
    LaunchedEffect(validity) {
        if (validity == Validity.Empty) {
            scale.animateTo(0f, tween(140))
        } else {
            shown = validity
            scale.snapTo(0f)
            scale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 600f))
        }
    }
    Canvas(
        modifier.graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        },
    ) { drawBadge(shown) }
}

private fun DrawScope.drawBadge(validity: Validity) {
    val color = when (validity) {
        Validity.Invalid -> FieldColors.Error
        Validity.Valid -> FieldColors.Valid
        Validity.Empty -> return
    }
    val w = size.minDimension
    val stroke = w * 0.12f
    drawCircle(color)
    if (validity == Validity.Invalid) {
        drawLine(FieldColors.Ink, Offset(w * 0.5f, w * 0.25f), Offset(w * 0.5f, w * 0.56f), stroke, StrokeCap.Round)
        drawCircle(FieldColors.Ink, stroke * 0.65f, Offset(w * 0.5f, w * 0.75f))
    } else {
        val check = Path().apply {
            moveTo(w * 0.29f, w * 0.52f)
            lineTo(w * 0.44f, w * 0.66f)
            lineTo(w * 0.72f, w * 0.37f)
        }
        drawPath(check, FieldColors.Ink, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}
