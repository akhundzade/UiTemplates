package com.azersun.uitemplates.demos.error1

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azersun.uitemplates.demos.checkbox1.CheckboxStage
import com.azersun.uitemplates.demos.checkbox1.checkPanel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class SaveState { Idle, Saving, Failed }

private val ToastShape = RoundedCornerShape(16.dp)
private val ChipShape = RoundedCornerShape(50)
private val ToastFill = Color(0xFF34152A)
private val NoteLines = listOf(0.92f, 0.78f, 0.86f, 0.5f)

@Composable
fun ErrorToastDemo() {
    var state by remember { mutableStateOf(SaveState.Idle) }
    val toast = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var job by remember { mutableStateOf<Job?>(null) }

    fun save() {
        if (state == SaveState.Saving) return
        job?.cancel()
        state = SaveState.Saving
        job = scope.launch {
            if (toast.value > 0f) toast.animateTo(0f, tween(220, easing = FastOutLinearInEasing))
            delay(1300)
            state = SaveState.Failed
            toast.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 360f))
            delay(2000)
            toast.animateTo(0f, tween(300, easing = FastOutLinearInEasing))
        }
    }

    fun closeToast() {
        job?.cancel()
        job = scope.launch { toast.animateTo(0f, tween(260, easing = FastOutLinearInEasing)) }
    }

    CheckboxStage(
        4,
        "Error Toast",
        "A red \"Couldn't save\" toast drops in from the top, then slides away.",
        autoplay = { save() },
    ) {
        Box(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth()
                    .checkPanel()
                    .padding(18.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Trip notes", color = ErrorColors.Text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    SaveChip(state) { save() }
                }
                Spacer(Modifier.height(18.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    NoteLines.forEach { fraction ->
                        Box(
                            Modifier
                                .fillMaxWidth(fraction)
                                .height(8.dp)
                                .background(ErrorColors.Muted.copy(alpha = 0.35f), ChipShape),
                        )
                    }
                }
            }
            Row(
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
                    .fillMaxWidth()
                    .graphicsLayer {
                        val v = toast.value
                        translationY = -(1f - v) * (size.height + 40.dp.toPx())
                        alpha = if (v > 0.001f) 1f else 0f
                    }
                    .clip(ToastShape)
                    .background(ToastFill)
                    .border(1.dp, ErrorColors.Red.copy(alpha = 0.5f), ToastShape)
                    .padding(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Canvas(Modifier.size(28.dp)) { drawAlertBadge() }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Couldn't save", color = ErrorColors.Text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text("Check your connection", color = ErrorColors.Muted, fontSize = 13.sp)
                }
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable(enabled = state == SaveState.Failed, role = Role.Button) { closeToast() },
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(Modifier.size(12.dp)) {
                        val stroke = 1.8.dp.toPx()
                        drawLine(ErrorColors.Muted, Offset.Zero, Offset(size.width, size.height), stroke, StrokeCap.Round)
                        drawLine(ErrorColors.Muted, Offset(size.width, 0f), Offset(0f, size.height), stroke, StrokeCap.Round)
                    }
                }
            }
        }
    }
}

@Composable
private fun SaveChip(state: SaveState, onClick: () -> Unit) {
    val fill by animateColorAsState(
        if (state == SaveState.Failed) ErrorColors.Red.copy(alpha = 0.16f) else ErrorColors.TileLight,
        tween(220),
        label = "chipFill",
    )
    Box(
        Modifier
            .clip(ChipShape)
            .background(fill)
            .clickable(enabled = state != SaveState.Saving, role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        AnimatedContent(
            state,
            transitionSpec = { fadeIn(tween(180, delayMillis = 60)) togetherWith fadeOut(tween(120)) using SizeTransform(clip = false) },
            label = "chip",
        ) { shown ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                when (shown) {
                    SaveState.Idle -> Canvas(Modifier.size(13.dp)) {
                        val stroke = 1.8.dp.toPx()
                        val mid = size.width / 2
                        drawLine(ErrorColors.Cyan, Offset(mid, size.height * 0.75f), Offset(mid, size.height * 0.1f), stroke, StrokeCap.Round)
                        drawLine(ErrorColors.Cyan, Offset(mid, size.height * 0.1f), Offset(size.width * 0.2f, size.height * 0.4f), stroke, StrokeCap.Round)
                        drawLine(ErrorColors.Cyan, Offset(mid, size.height * 0.1f), Offset(size.width * 0.8f, size.height * 0.4f), stroke, StrokeCap.Round)
                        drawLine(ErrorColors.Cyan, Offset(size.width * 0.1f, size.height * 0.95f), Offset(size.width * 0.9f, size.height * 0.95f), stroke, StrokeCap.Round)
                    }
                    SaveState.Saving -> Spinner(ErrorColors.Cyan, Modifier.size(13.dp))
                    SaveState.Failed -> Canvas(Modifier.size(14.dp)) { drawAlertBadge() }
                }
                Spacer(Modifier.width(7.dp))
                Text(
                    when (shown) {
                        SaveState.Idle -> "Save"
                        SaveState.Saving -> "Saving…"
                        SaveState.Failed -> "Not saved"
                    },
                    color = if (shown == SaveState.Failed) ErrorColors.Red else ErrorColors.Text,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
