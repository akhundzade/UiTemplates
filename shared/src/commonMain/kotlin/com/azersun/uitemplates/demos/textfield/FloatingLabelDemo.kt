package com.azersun.uitemplates.demos.textfield

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private const val FloatedScale = 0.75f
private val FieldHeight = 56.dp
private val TextInset = 16.dp
private val GapPadding = 4.dp

@Composable
fun FloatingLabelDemo() {
    val state = remember { TypingState() }
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val active = focused || state.scriptFocus
    val floated by animateFloatAsState(
        if (active || state.text.isNotEmpty()) 1f else 0f,
        tween(220, easing = FastOutSlowInEasing),
        label = "float",
    )
    val outline by animateColorAsState(if (active) FieldColors.Focus else FieldColors.Border, tween(220), label = "outline")
    val labelColor by animateColorAsState(if (active) FieldColors.Focus else FieldColors.Muted, tween(220), label = "label")
    var labelWidth by remember { mutableFloatStateOf(0f) }

    TextFieldStage(
        1,
        "Floating Label",
        "The placeholder floats up to the border and the outline opens a gap behind it.",
        state,
        script = {
            state.focus()
            delay(400)
            state.type("mia@mail.com")
        },
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(FieldHeight)
                .drawWithCache {
                    val path = Path()
                    val stroke = 1.5.dp.toPx()
                    val radius = 12.dp.toPx()
                    val inset = TextInset.toPx()
                    val pad = GapPadding.toPx()
                    onDrawBehind {
                        val progress = floated
                        val span = labelWidth * FloatedScale
                        val mid = inset + span / 2
                        val half = (span / 2 + pad) * progress
                        path.outlineWithGap(size, radius, stroke / 2, mid - half, mid + half)
                        drawPath(path, outline, style = Stroke(stroke))
                    }
                },
        ) {
            ScriptedTextField(
                state,
                interaction,
                modifier = Modifier.fillMaxSize(),
                keyboardOptions = KeyboardOptions(
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Done,
                ),
                decorationBox = { inner ->
                    Box(Modifier.fillMaxSize().padding(horizontal = TextInset), contentAlignment = Alignment.CenterStart) {
                        inner()
                    }
                },
            )
            Text(
                "Email",
                color = labelColor,
                fontSize = 16.sp,
                onTextLayout = { labelWidth = it.size.width.toFloat() },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = TextInset)
                    .graphicsLayer {
                        val scale = 1f - (1f - FloatedScale) * floated
                        scaleX = scale
                        scaleY = scale
                        transformOrigin = TransformOrigin(0f, 0.5f)
                        translationY = -floated * FieldHeight.toPx() / 2
                    },
            )
        }
        Text(
            "We'll never share it.",
            color = FieldColors.Muted,
            fontSize = 12.sp,
            modifier = Modifier.padding(start = TextInset, top = 8.dp),
        )
    }
}

/** A rounded-rectangle outline traced clockwise from [gapEnd] round to [gapStart], leaving the top edge open between them. */
private fun Path.outlineWithGap(size: Size, radius: Float, inset: Float, gapStart: Float, gapEnd: Float) {
    val left = inset
    val top = inset
    val right = size.width - inset
    val bottom = size.height - inset
    val d = radius * 2
    reset()
    moveTo(gapEnd.coerceAtLeast(left + radius), top)
    lineTo(right - radius, top)
    arcTo(Rect(right - d, top, right, top + d), -90f, 90f, false)
    lineTo(right, bottom - radius)
    arcTo(Rect(right - d, bottom - d, right, bottom), 0f, 90f, false)
    lineTo(left + radius, bottom)
    arcTo(Rect(left, bottom - d, left + d, bottom), 90f, 90f, false)
    lineTo(left, top + radius)
    arcTo(Rect(left, top, left + d, top + d), 180f, 90f, false)
    lineTo(gapStart.coerceAtLeast(left + radius), top)
}
