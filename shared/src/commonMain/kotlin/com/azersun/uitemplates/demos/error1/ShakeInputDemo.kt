package com.azersun.uitemplates.demos.error1

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azersun.uitemplates.demos.checkbox1.CheckboxStage
import kotlinx.coroutines.launch

private const val SampleEmail = "name@mail"
private val EmailPattern = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]{2,}$")
private val FieldShape = RoundedCornerShape(14.dp)
private val ButtonBrush = Brush.horizontalGradient(listOf(ErrorColors.Violet, ErrorColors.Blue))
private val InputStyle = TextStyle(color = ErrorColors.Text, fontSize = 16.sp)
private val ErrorFill = Color(0xFF2A1426)

private fun sampleValue() = TextFieldValue(SampleEmail, TextRange(SampleEmail.length))

@Composable
fun ShakeInputDemo() {
    var value by remember { mutableStateOf(sampleValue()) }
    var error by remember { mutableStateOf(false) }
    var valid by remember { mutableStateOf(false) }
    val shake = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val border by animateColorAsState(
        when {
            error -> ErrorColors.Red
            valid -> ErrorColors.Cyan
            focused -> ErrorColors.Violet
            else -> ErrorColors.FieldBorder
        },
        tween(200),
        label = "border",
    )
    val fill by animateColorAsState(if (error) ErrorFill else ErrorColors.Field, tween(200), label = "fill")

    fun submit() {
        focusManager.clearFocus()
        if (EmailPattern.matches(value.text)) {
            error = false
            valid = true
            return
        }
        valid = false
        error = true
        scope.launch { shake.shake(14f) }
    }

    CheckboxStage(
        1,
        "Shake Input",
        "Email field shakes, its border turns red, and \"Enter a valid email\" slides in.",
        onReplay = {
            focusManager.clearFocus()
            value = sampleValue()
            error = false
            valid = false
        },
        autoplay = { submit() },
    ) {
        Column(Modifier.padding(horizontal = 28.dp).fillMaxWidth()) {
            Text("Email", color = ErrorColors.Muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            BasicTextField(
                value = value,
                onValueChange = {
                    if (it.text != value.text) {
                        error = false
                        valid = false
                    }
                    value = it
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .graphicsLayer { translationX = shake.value.dp.toPx() }
                    .background(fill, FieldShape)
                    .border(1.5.dp, border, FieldShape),
                textStyle = InputStyle,
                cursorBrush = SolidColor(ErrorColors.Violet),
                singleLine = true,
                interactionSource = interaction,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                decorationBox = { inner ->
                    Box(Modifier.fillMaxSize().padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) { inner() }
                },
            )
            AnimatedVisibility(
                error,
                enter = expandVertically(tween(220)) + slideInVertically(tween(260)) { -it } + fadeIn(tween(220, delayMillis = 60)),
                exit = shrinkVertically(tween(180)) + fadeOut(tween(120)),
            ) {
                Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Canvas(Modifier.size(16.dp)) { drawAlertBadge() }
                    Spacer(Modifier.width(8.dp))
                    Text("Enter a valid email", color = ErrorColors.Red, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
            Spacer(Modifier.height(22.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(FieldShape)
                    .background(ButtonBrush)
                    .clickable(role = Role.Button) { submit() },
                contentAlignment = Alignment.Center,
            ) {
                Text("Continue", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
