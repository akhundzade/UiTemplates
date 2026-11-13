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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val CharLimit = 40
private const val WarnAt = 30
private val BioShape = RoundedCornerShape(16.dp)

@Composable
fun CharCounterDemo() {
    val state = remember { TypingState() }
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val active = focused || state.scriptFocus
    val count = state.text.length
    val over = count > CharLimit
    val fill by animateFloatAsState(
        (count / CharLimit.toFloat()).coerceAtMost(1f),
        spring(stiffness = 300f),
        label = "fill",
    )
    val ringColor by animateColorAsState(
        when {
            over -> FieldColors.Error
            count > WarnAt -> FieldColors.Amber
            else -> FieldColors.Focus
        },
        tween(200),
        label = "ring",
    )
    val outline by animateColorAsState(if (active) FieldColors.Focus else FieldColors.Border, tween(220), label = "outline")
    val shake = remember { Animatable(0f) }
    LaunchedEffect(state) {
        var previous = 0
        snapshotFlow { state.text.length }.collect { length ->
            if (length > CharLimit && length > previous) {
                // A sideways kick rather than a pose, so every extra character restarts a short decaying shake.
                launch { shake.animateTo(0f, spring(dampingRatio = 0.2f, stiffness = 1500f), initialVelocity = 260f) }
            }
            previous = length
        }
    }

    TextFieldStage(
        6,
        "Char Counter",
        "A ring fills as you type, turns amber near the limit and pink with a shake once you're over.",
        state,
        script = {
            state.focus()
            delay(400)
            state.type("Coffee first, then code. Building small calm apps for people.")
        },
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(FieldColors.Field, BioShape)
                .border(1.5.dp, outline, BioShape),
        ) {
            ScriptedTextField(
                state,
                interaction,
                modifier = Modifier.fillMaxSize(),
                singleLine = false,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                decorationBox = { inner ->
                    Box(Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 44.dp)) {
                        if (state.text.isEmpty()) {
                            Text("Write a short bio…", style = ValueStyle.copy(color = FieldColors.Muted))
                        }
                        inner()
                    }
                },
            )
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .size(30.dp)
                    .graphicsLayer { translationX = shake.value.dp.toPx() },
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = 3.dp.toPx()
                    val inset = stroke / 2
                    val arcSize = Size(size.width - stroke, size.height - stroke)
                    val topLeft = Offset(inset, inset)
                    drawArc(FieldColors.Text.copy(alpha = 0.12f), 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
                    drawArc(ringColor, -90f, 360f * fill.coerceIn(0f, 1f), false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                }
                if (over) {
                    Text(
                        "${CharLimit - count}",
                        color = FieldColors.Error,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}
