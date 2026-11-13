package com.azersun.uitemplates.demos.textfield

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azersun.uitemplates.demos.menus.MenuColors
import kotlinx.coroutines.delay

@Composable
fun UnderlineGrowDemo() {
    val state = remember { TypingState() }
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val active = focused || state.scriptFocus
    // Where the line grows from, as a fraction of the field width.
    var origin by remember { mutableFloatStateOf(0.5f) }
    val grow by animateFloatAsState(
        if (active) 1f else 0f,
        tween(if (active) 480 else 280, easing = FastOutSlowInEasing),
        label = "grow",
    )
    val labelColor by animateColorAsState(if (active) MenuColors.Cyan else FieldColors.Muted, tween(220), label = "label")

    TextFieldStage(
        2,
        "Underline Grow",
        "A gradient underline grows out both ways from wherever you tap.",
        state,
        script = {
            origin = 0.3f
            state.focus()
            delay(500)
            state.type("Mia Park")
        },
    ) {
        FieldLabel("Full name", labelColor)
        Box(
            Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        // Once the line is full a new origin changes nothing until it shrinks back to it.
                        origin = (down.position.x / size.width).coerceIn(0f, 1f)
                    }
                }
                .drawWithCache {
                    val hairline = 1.dp.toPx()
                    val thick = 2.dp.toPx()
                    val gradient = Brush.horizontalGradient(listOf(MenuColors.Cyan, MenuColors.Violet), 0f, size.width)
                    onDrawBehind {
                        val progress = grow
                        val w = size.width
                        drawLine(FieldColors.Border, Offset(0f, size.height - hairline / 2), Offset(w, size.height - hairline / 2), hairline)
                        if (progress <= 0f) return@onDrawBehind
                        val from = origin * w
                        val y = size.height - thick / 2
                        drawLine(gradient, Offset(from * (1f - progress), y), Offset(from + (w - from) * progress, y), thick)
                    }
                },
        ) {
            ScriptedTextField(
                state,
                interaction,
                modifier = Modifier.fillMaxWidth(),
                textStyle = ValueStyle.copy(fontSize = 18.sp),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done,
                ),
                decorationBox = { inner ->
                    Box(Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 12.dp)) { inner() }
                },
            )
        }
    }
}
