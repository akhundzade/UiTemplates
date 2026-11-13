package com.azersun.uitemplates.demos.error1

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azersun.uitemplates.demos.checkbox1.CheckboxStage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val PinLength = 4
private const val Backspace = "⌫"
private const val ScriptedPin = "2580"
private val KeyRows = listOf(
    listOf("1", "2", "3"),
    listOf("4", "5", "6"),
    listOf("7", "8", "9"),
    listOf("", "0", Backspace),
)
private val KeySize = 62.dp
private val DotSize = 16.dp

@Composable
fun PinShakeDemo() {
    val dots = remember { List(PinLength) { Animatable(0f) } }
    var entered by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    val red = remember { Animatable(0f) }
    val shake = remember { Animatable(0f) }
    var wrong by remember { mutableStateOf(false) }
    var flashKey by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val messageAlpha by animateFloatAsState(if (wrong) 1f else 0f, tween(200), label = "message")

    fun reject() {
        busy = true
        scope.launch {
            delay(180)
            launch { red.animateTo(1f, tween(160)) }
            wrong = true
            shake.shake(16f)
            delay(450)
            for (i in PinLength - 1 downTo 0) {
                dots[i].animateTo(0f, tween(110))
                delay(50)
            }
            red.animateTo(0f, tween(200))
            entered = 0
            busy = false
        }
    }

    fun press(key: String) {
        if (busy) return
        when (key) {
            "" -> Unit
            Backspace -> if (entered > 0) {
                entered--
                val index = entered
                scope.launch { dots[index].animateTo(0f, tween(110)) }
            }
            else -> if (entered < PinLength) {
                wrong = false
                val index = entered
                entered++
                scope.launch { dots[index].animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 700f)) }
                // There is no right code here: the demo is about the rejection.
                if (entered == PinLength) reject()
            }
        }
    }

    CheckboxStage(
        5,
        "PIN Shake",
        "Passcode dots fill in, turn red and shake, then clear one by one.",
        autoplay = {
            try {
                ScriptedPin.forEach { digit ->
                    delay(320)
                    val key = digit.toString()
                    flashKey = key
                    press(key)
                    delay(130)
                    flashKey = null
                }
            } finally {
                flashKey = null
            }
        },
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Enter passcode", color = ErrorColors.Text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(20.dp))
            Row(
                Modifier.graphicsLayer { translationX = shake.value.dp.toPx() },
                horizontalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                dots.forEach { fill ->
                    Spacer(
                        Modifier
                            .size(DotSize)
                            .drawBehind {
                                val f = fill.value
                                val r = red.value
                                val radius = size.minDimension / 2
                                val stroke = 2.dp.toPx()
                                drawCircle(
                                    lerp(ErrorColors.Outline, ErrorColors.Red, r),
                                    radius - stroke / 2,
                                    style = Stroke(stroke),
                                )
                                if (f > 0f) drawCircle(lerp(ErrorColors.Blue, ErrorColors.Red, r), radius * f)
                            },
                    )
                }
            }
            Box(Modifier.height(52.dp), contentAlignment = Alignment.Center) {
                Text(
                    "Wrong passcode",
                    color = ErrorColors.Red,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.graphicsLayer {
                        alpha = messageAlpha
                        translationY = (1f - messageAlpha) * -6.dp.toPx()
                    },
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                KeyRows.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                        row.forEach { key -> PinKey(key, flashing = flashKey == key) { press(key) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun PinKey(key: String, flashing: Boolean, onClick: () -> Unit) {
    if (key.isEmpty()) {
        Spacer(Modifier.size(KeySize))
        return
    }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val idle = if (key == Backspace) Color.Transparent else ErrorColors.Tile
    val fill by animateColorAsState(if (pressed || flashing) ErrorColors.TileLight else idle, tween(90), label = "key")
    Box(
        Modifier
            .size(KeySize)
            .clip(CircleShape)
            .background(fill)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (key == Backspace) {
            Canvas(Modifier.size(24.dp)) {
                val w = size.width
                val stroke = Stroke(w * 0.08f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                val outline = Path().apply {
                    moveTo(w * 0.32f, w * 0.22f)
                    lineTo(w * 0.9f, w * 0.22f)
                    lineTo(w * 0.9f, w * 0.78f)
                    lineTo(w * 0.32f, w * 0.78f)
                    lineTo(w * 0.08f, w * 0.5f)
                    close()
                }
                drawPath(outline, ErrorColors.Text, style = stroke)
                drawLine(ErrorColors.Text, Offset(w * 0.48f, w * 0.38f), Offset(w * 0.72f, w * 0.62f), stroke.width, StrokeCap.Round)
                drawLine(ErrorColors.Text, Offset(w * 0.72f, w * 0.38f), Offset(w * 0.48f, w * 0.62f), stroke.width, StrokeCap.Round)
            }
        } else {
            Text(key, color = ErrorColors.Text, fontSize = 24.sp, fontWeight = FontWeight.Medium)
        }
    }
}
