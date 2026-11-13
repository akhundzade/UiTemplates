package com.azersun.uitemplates.demos.error1

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azersun.uitemplates.demos.checkbox1.CheckboxStage
import com.azersun.uitemplates.demos.checkbox1.checkPanel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private enum class UploadPhase { Uploading, Failed, Done }

private const val StallAt = 0.68f
private val UploadBrush = Brush.horizontalGradient(listOf(ErrorColors.Violet, ErrorColors.Cyan))
// Quick start, then a long crawl into the stall point.
private val StallEasing = CubicBezierEasing(0.15f, 0.7f, 0.3f, 1f)
private val ChipShape = RoundedCornerShape(50)

@Composable
fun UploadFailedDemo() {
    var phase by remember { mutableStateOf(UploadPhase.Uploading) }
    val progress = remember { Animatable(0f) }
    val red = remember { Animatable(0f) }
    val green = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var job by remember { mutableStateOf<Job?>(null) }

    fun upload(fail: Boolean, startDelay: Long = 0) {
        job?.cancel()
        phase = UploadPhase.Uploading
        job = scope.launch {
            launch { red.animateTo(0f, tween(220)) }
            green.snapTo(0f)
            if (fail) {
                progress.snapTo(0f)
                delay(startDelay)
                progress.animateTo(StallAt, tween(2100, easing = StallEasing))
                // The bar twitches in place without the percentage moving.
                repeat(7) { i -> progress.animateTo(if (i % 2 == 0) StallAt + 0.009f else StallAt + 0.001f, tween(110)) }
                delay(120)
                phase = UploadPhase.Failed
                red.animateTo(1f, tween(260))
            } else {
                progress.animateTo(0f, tween(260, easing = FastOutSlowInEasing))
                progress.animateTo(1f, tween(1700, easing = FastOutSlowInEasing))
                phase = UploadPhase.Done
                green.animateTo(1f, tween(280))
            }
        }
    }

    // The upload is the scene, not the autoplay, so it runs even if a touch stops the scripted Retry tap.
    LaunchedEffect(Unit) { upload(fail = true, startDelay = 700) }

    CheckboxStage(
        6,
        "Upload Failed",
        "Upload bar stalls at 68%, turns red: \"Upload failed\" with a Retry chip.",
        onReplay = { upload(fail = true, startDelay = 300) },
        autoplay = {
            snapshotFlow { phase }.first { it == UploadPhase.Failed }
            delay(1400)
            upload(fail = false)
        },
    ) {
        Column(
            Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .checkPanel()
                .padding(18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ErrorColors.Violet.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(Modifier.size(22.dp)) {
                        val w = size.width
                        val stroke = Stroke(w * 0.09f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        val page = Path().apply {
                            moveTo(w * 0.2f, w * 0.08f)
                            lineTo(w * 0.58f, w * 0.08f)
                            lineTo(w * 0.8f, w * 0.3f)
                            lineTo(w * 0.8f, w * 0.92f)
                            lineTo(w * 0.2f, w * 0.92f)
                            close()
                            moveTo(w * 0.58f, w * 0.08f)
                            lineTo(w * 0.58f, w * 0.3f)
                            lineTo(w * 0.8f, w * 0.3f)
                        }
                        drawPath(page, ErrorColors.Violet, style = stroke)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("design-assets.zip", color = ErrorColors.Text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text("24.6 MB", color = ErrorColors.Muted, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth().height(30.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { UploadStatus(phase) { progress.value } }
                RetryChip(visible = phase == UploadPhase.Failed) { upload(fail = false) }
            }
            Spacer(Modifier.height(10.dp))
            Spacer(
                Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .drawBehind {
                        val p = progress.value.coerceIn(0f, 1f)
                        val r = red.value
                        val g = green.value
                        val corner = CornerRadius(size.height / 2)
                        drawRoundRect(ErrorColors.Track, cornerRadius = corner)
                        if (p <= 0f) return@drawBehind
                        val bar = Size(size.width * p, size.height)
                        drawRoundRect(UploadBrush, Offset.Zero, bar, corner)
                        if (r > 0f) drawRoundRect(ErrorColors.Red, Offset.Zero, bar, corner, alpha = r)
                        if (g > 0f) drawRoundRect(ErrorColors.Green, Offset.Zero, bar, corner, alpha = g)
                    },
            )
        }
    }
}

@Composable
private fun UploadStatus(phase: UploadPhase, progress: () -> Float) {
    AnimatedContent(
        phase,
        transitionSpec = { (fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 3 }) togetherWith fadeOut(tween(120)) },
        label = "status",
    ) { shown ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            when (shown) {
                UploadPhase.Uploading -> {
                    val percent = (progress() * 100f + 0.001f).toInt().coerceIn(0, 100)
                    Text("Uploading… $percent%", color = ErrorColors.Text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
                UploadPhase.Failed -> {
                    Canvas(Modifier.size(16.dp)) { drawAlertBadge() }
                    Spacer(Modifier.width(8.dp))
                    Text("Upload failed", color = ErrorColors.Red, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
                UploadPhase.Done -> {
                    Canvas(Modifier.size(16.dp)) { drawDoneBadge(ErrorColors.Green) }
                    Spacer(Modifier.width(8.dp))
                    Text("Upload complete", color = ErrorColors.Green, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun RetryChip(visible: Boolean, onClick: () -> Unit) {
    val pop by animateFloatAsState(
        if (visible) 1f else 0f,
        if (visible) spring(dampingRatio = 0.5f, stiffness = 500f) else tween(120),
        label = "retry",
    )
    Row(
        Modifier
            .graphicsLayer {
                scaleX = pop
                scaleY = pop
                alpha = pop.coerceIn(0f, 1f)
            }
            .clip(ChipShape)
            .border(1.dp, ErrorColors.Red, ChipShape)
            .clickable(enabled = visible, role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(13.dp)) { drawRetryGlyph(ErrorColors.Red) }
        Spacer(Modifier.width(6.dp))
        Text("Retry", color = ErrorColors.Red, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}
