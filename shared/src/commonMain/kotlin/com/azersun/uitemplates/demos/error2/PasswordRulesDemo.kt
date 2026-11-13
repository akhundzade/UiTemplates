package com.azersun.uitemplates.demos.error2

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

private class Rule(val label: String, val test: (String) -> Boolean)

private val Rules = listOf(
    Rule("At least 8 characters") { it.length >= 8 },
    Rule("Contains a number") { password -> password.any { it.isDigit() } },
    Rule("Contains a symbol") { password -> password.any { !it.isLetterOrDigit() && !it.isWhitespace() } },
)

private val FieldShape = RoundedCornerShape(14.dp)
private val MarkInk = Color(0xFF0B0A18)
private val ButtonBrush = Brush.horizontalGradient(listOf(ErrorColors.Violet, ErrorColors.Blue))

@Composable
fun PasswordRulesDemo() {
    var password by remember { mutableStateOf("sunflower") }
    // Null until checked; editing the password clears it again.
    var results by remember { mutableStateOf<List<Boolean>?>(null) }
    var checks by remember { mutableIntStateOf(0) }
    var autoPressed by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val fieldInteraction = remember { MutableInteractionSource() }
    val focused by fieldInteraction.collectIsFocusedAsState()
    val buttonInteraction = remember { MutableInteractionSource() }
    val pressed by buttonInteraction.collectIsPressedAsState()
    val buttonScale by animateFloatAsState(if (pressed || autoPressed) 0.95f else 1f, tween(110), label = "press")

    fun check() {
        focusManager.clearFocus()
        results = Rules.map { it.test(password) }
        checks++
    }

    val failures = results?.count { !it } ?: 0
    val border by animateColorAsState(
        when {
            results == null -> if (focused) ErrorColors.Violet else ErrorColors.Border
            failures > 0 -> ErrorColors.Red
            else -> ErrorColors.Cyan
        },
        tween(260),
        label = "border",
    )

    val hintAlpha by animateFloatAsState(if (failures > 0) 1f else 0f, tween(200), label = "hint")

    ErrorStage(
        6,
        "Password Rules",
        "Password rules check at once: one cyan ✓, two red ✕ that shake.",
        autoplay = {
            autoPressed = true
            try {
                delay(140)
            } finally {
                autoPressed = false
            }
            check()
        },
    ) {
        Column(Modifier.padding(horizontal = 28.dp).widthIn(max = 340.dp).fillMaxWidth()) {
            Text("Password", color = ErrorColors.Muted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            BasicTextField(
                value = password,
                onValueChange = {
                    password = it
                    results = null
                },
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .clip(FieldShape)
                    .background(ErrorColors.Tile)
                    .border(1.5.dp, border, FieldShape)
                    .padding(horizontal = 16.dp, vertical = 15.dp),
                textStyle = TextStyle(color = ErrorColors.Text, fontSize = 18.sp, letterSpacing = 2.sp),
                singleLine = true,
                cursorBrush = SolidColor(ErrorColors.Cyan),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { check() }),
                interactionSource = fieldInteraction,
            )
            Text(
                if (failures == 1) "1 rule isn't met yet" else "$failures rules aren't met yet",
                color = ErrorColors.Red,
                fontSize = 12.sp,
                modifier = Modifier
                    .padding(start = 4.dp, top = 6.dp)
                    .graphicsLayer { alpha = hintAlpha },
            )
            Column(Modifier.padding(top = 10.dp)) {
                Rules.forEachIndexed { i, rule ->
                    RuleRow(rule.label, passed = results?.get(i), checks = checks)
                }
            }
            Box(
                Modifier
                    .padding(top = 22.dp)
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = buttonScale
                        scaleY = buttonScale
                    }
                    .clip(RoundedCornerShape(50))
                    .background(ButtonBrush)
                    .clickable(interactionSource = buttonInteraction, indication = null, onClick = ::check)
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Create account", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun RuleRow(label: String, passed: Boolean?, checks: Int) {
    val mark = remember { Animatable(0f) }
    val shake = remember { Animatable(0f) }
    // Keeps the last result's colour while the mark fades out after an edit.
    var shown by remember { mutableStateOf(true) }
    LaunchedEffect(checks, passed) {
        if (passed != null) shown = passed
        if (passed == null) {
            mark.animateTo(0f, tween(160))
            return@LaunchedEffect
        }
        mark.snapTo(0f)
        shake.snapTo(0f)
        mark.animateTo(1f, tween(380, easing = FastOutSlowInEasing))
        if (!passed) shake.animateTo(1f, tween(480, easing = LinearEasing))
    }
    val labelColor by animateColorAsState(
        when (passed) {
            null -> ErrorColors.Muted
            true -> ErrorColors.Text
            false -> ErrorColors.Red
        },
        tween(220),
        label = "rule",
    )
    Row(
        Modifier
            .padding(vertical = 7.dp)
            .graphicsLayer { translationX = shakeOffset(shake.value) * 7.dp.toPx() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(22.dp)) {
            val m = mark.value
            val pop = 1f + 0.22f * sin(m * PI.toFloat())
            val r = size.minDimension / 2f
            drawCircle(ErrorColors.Muted, r - 1.dp.toPx(), alpha = 1f - m, style = Stroke(1.5.dp.toPx()))
            if (m > 0f) {
                val color = if (shown) ErrorColors.Cyan else ErrorColors.Red
                drawCircle(color, r * pop, alpha = (m * 2f).coerceAtMost(1f))
                val glyph = ((m - 0.35f) / 0.65f).coerceIn(0f, 1f)
                if (shown) drawTick(glyph, MarkInk) else drawCross(glyph, Color.White)
            }
        }
        Text(label, color = labelColor, fontSize = 14.sp, modifier = Modifier.padding(start = 12.dp))
    }
}

private fun DrawScope.drawTick(progress: Float, color: Color) {
    if (progress <= 0f) return
    val w = size.width
    val a = Offset(w * 0.28f, w * 0.52f)
    val b = Offset(w * 0.44f, w * 0.68f)
    val c = Offset(w * 0.73f, w * 0.36f)
    val stroke = w * 0.12f
    // The short leg takes the first third of the stroke, the long leg the rest.
    val first = (progress * 3f).coerceAtMost(1f)
    drawLine(color, a, a + (b - a) * first, stroke, StrokeCap.Round)
    if (progress > 1f / 3f) {
        val second = (progress - 1f / 3f) * 1.5f
        drawLine(color, b, b + (c - b) * second, stroke, StrokeCap.Round)
    }
}

private fun DrawScope.drawCross(progress: Float, color: Color) {
    if (progress <= 0f) return
    val w = size.width
    val stroke = w * 0.12f
    val first = (progress * 2f).coerceAtMost(1f)
    val a = Offset(w * 0.34f, w * 0.34f)
    val b = Offset(w * 0.66f, w * 0.66f)
    drawLine(color, a, a + (b - a) * first, stroke, StrokeCap.Round)
    if (progress > 0.5f) {
        val second = (progress - 0.5f) * 2f
        val c = Offset(w * 0.66f, w * 0.34f)
        val d = Offset(w * 0.34f, w * 0.66f)
        drawLine(color, c, c + (d - c) * second, stroke, StrokeCap.Round)
    }
}
