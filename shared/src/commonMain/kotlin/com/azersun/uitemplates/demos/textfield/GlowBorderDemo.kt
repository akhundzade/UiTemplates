package com.azersun.uitemplates.demos.textfield

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azersun.uitemplates.demos.menus.MenuColors
import kotlinx.coroutines.delay

private const val LapMillis = 2400
private const val TailFraction = 0.3f
private const val TailSteps = 24
private val PillShape = RoundedCornerShape(50)

@Composable
fun GlowBorderDemo() {
    val state = remember { TypingState(transform = String::uppercase) }
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val active = focused || state.scriptFocus
    val focusManager = LocalFocusManager.current
    var applied by remember { mutableStateOf<String?>(null) }
    val filled = state.text.isNotEmpty()

    val glow by animateFloatAsState(if (active) 1f else 0f, tween(300), label = "glow")
    val phase = remember { Animatable(0f) }
    LaunchedEffect(active) {
        if (!active) return@LaunchedEffect
        while (true) {
            phase.snapTo(phase.value % 1f)
            phase.animateTo(phase.value + 1f, tween(LapMillis, easing = LinearEasing))
        }
    }
    val buttonColor by animateColorAsState(if (filled) MenuColors.Blue else FieldColors.Border, tween(220), label = "apply")
    val buttonText by animateColorAsState(if (filled) FieldColors.Text else FieldColors.Muted, tween(220), label = "applyText")
    val showApplied by animateFloatAsState(if (applied != null && applied == state.text) 1f else 0f, tween(220), label = "applied")

    TextFieldStage(
        3,
        "Glow Border",
        "A comet of light circles the pill while it's focused; the code is uppercased as you type.",
        state,
        onReplay = { applied = null },
        script = {
            state.focus()
            delay(400)
            state.type("spring25")
        },
    ) {
        FieldLabel("Promo code")
        Row(
            Modifier
                .fillMaxWidth()
                .height(58.dp)
                .background(FieldColors.Field, PillShape)
                .drawWithCache {
                    val stroke = 1.5.dp.toPx()
                    val inset = 1.5.dp.toPx()
                    val outline = Path().apply {
                        addRoundRect(
                            RoundRect(inset, inset, size.width - inset, size.height - inset, CornerRadius(size.height / 2 - inset)),
                        )
                    }
                    val measure = PathMeasure().apply { setPath(outline, true) }
                    val length = measure.length
                    val segment = Path()
                    val thin = 1.dp.toPx()
                    val thick = 3.dp.toPx()
                    val halo = 7.dp.toPx()
                    val haloInner = 3.5.dp.toPx()
                    val core = 2.dp.toPx()
                    onDrawBehind {
                        val g = glow
                        val head = (phase.value % 1f) * length
                        drawPath(outline, FieldColors.Border, style = Stroke(stroke))
                        if (g <= 0f) return@onDrawBehind
                        drawPath(outline, MenuColors.Violet, alpha = 0.18f * g, style = Stroke(halo))
                        val tail = length * TailFraction
                        for (i in 0 until TailSteps) {
                            val f = (i + 1f) / TailSteps
                            segment.reset()
                            measure.wrappedSegment(head - tail + tail * i / TailSteps, head - tail + tail * f, length, segment)
                            drawPath(
                                segment,
                                lerp(MenuColors.Violet, MenuColors.Cyan, f),
                                alpha = g * f * f,
                                style = Stroke(thin + (thick - thin) * f, cap = StrokeCap.Butt),
                            )
                        }
                        val tip = measure.getPosition(head)
                        drawCircle(MenuColors.Cyan, halo, tip, alpha = 0.18f * g)
                        drawCircle(MenuColors.Cyan, haloInner, tip, alpha = 0.4f * g)
                        drawCircle(FieldColors.Text, core, tip, alpha = g)
                    }
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ScriptedTextField(
                state,
                interaction,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                textStyle = ValueStyle.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Done,
                ),
                decorationBox = { inner ->
                    Box(Modifier.fillMaxHeight().padding(start = 22.dp, end = 8.dp), contentAlignment = Alignment.CenterStart) {
                        inner()
                    }
                },
            )
            Box(
                Modifier
                    .padding(end = 7.dp)
                    .height(44.dp)
                    .clip(PillShape)
                    .background(buttonColor)
                    .clickable(enabled = filled) {
                        applied = state.text
                        focusManager.clearFocus()
                    }
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Apply", color = buttonText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Text(
            "✓  ${applied.orEmpty()} applied",
            color = FieldColors.Valid,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .padding(start = 22.dp, top = 10.dp)
                .graphicsLayer {
                    alpha = showApplied
                    translationY = (1f - showApplied) * -4.dp.toPx()
                },
        )
    }
}

/** Appends the stretch [start]..[end] of the closed path to [dst], wrapping past the start point. */
private fun PathMeasure.wrappedSegment(start: Float, end: Float, length: Float, dst: Path) {
    var from = start % length
    if (from < 0f) from += length
    val to = from + (end - start)
    if (to <= length) {
        getSegment(from, to, dst, true)
    } else {
        getSegment(from, length, dst, true)
        getSegment(0f, to - length, dst, true)
    }
}
