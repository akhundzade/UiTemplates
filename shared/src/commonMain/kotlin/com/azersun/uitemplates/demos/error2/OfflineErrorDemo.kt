package com.azersun.uitemplates.demos.error2

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private enum class Link { Idle, Searching, Offline }

/** One scan lights the dot and three arcs in turn (a unit each), holds, then fades them all by [ScanEnd]. */
private const val ScanEnd = 5f
private const val ScanMillis = 1100
private const val ScanCycles = 3

private val Dim = Color.White.copy(alpha = 0.12f)
private val Grey = Color(0xFF5F5B85)

@Composable
fun OfflineErrorDemo() {
    var link by remember { mutableStateOf(Link.Idle) }
    var attempt by remember { mutableIntStateOf(0) }
    val scan = remember { Animatable(0f) }
    val grey = remember { Animatable(0f) }
    val slash = remember { Animatable(0f) }

    LaunchedEffect(attempt) {
        if (attempt == 0) return@LaunchedEffect
        link = Link.Searching
        coroutineScope {
            launch { slash.animateTo(0f, tween(200)) }
            launch { grey.animateTo(0f, tween(200)) }
        }
        repeat(ScanCycles) {
            scan.snapTo(0f)
            scan.animateTo(ScanEnd, tween(ScanMillis, easing = LinearEasing))
        }
        link = Link.Offline
        grey.animateTo(1f, tween(260))
        slash.animateTo(1f, tween(380, easing = FastOutSlowInEasing))
    }

    ErrorStage(
        1,
        "Offline",
        "Wi-Fi arcs scan, then a red slash cuts through: \"No connection\".",
        autoplay = { attempt++ },
    ) {
        Column(
            Modifier.clickable(
                interactionSource = null,
                indication = null,
                enabled = link != Link.Searching,
                onClickLabel = "Try again",
                role = Role.Button,
                onClick = { attempt++ },
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Canvas(Modifier.size(170.dp)) {
                val s = scan.value
                val g = grey.value
                val cut = slash.value
                val searching = link == Link.Searching
                val base = lerp(Dim, Grey, g)
                repeat(4) { i ->
                    val lit = if (searching) litOf(i, s) else 0f
                    drawWifiPart(i, base, lit)
                }
                if (cut > 0f) drawSlash(cut)
            }
            AnimatedContent(
                targetState = link,
                contentAlignment = Alignment.TopCenter,
                transitionSpec = {
                    (fadeIn(tween(240, delayMillis = 80)) + slideInVertically(tween(280)) { it / 3 }) togetherWith
                        (fadeOut(tween(120)) + slideOutVertically(tween(200)) { -it / 3 })
                },
                label = "status",
            ) { state ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        when (state) {
                            Link.Idle -> "Wi-Fi"
                            Link.Searching -> "Searching…"
                            Link.Offline -> "No connection"
                        },
                        color = if (state == Link.Offline) ErrorColors.Red else ErrorColors.Text,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        when (state) {
                            Link.Idle -> "Tap to connect"
                            Link.Searching -> "Looking for networks nearby"
                            Link.Offline -> "Tap to try again"
                        },
                        color = ErrorColors.Muted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

private fun litOf(index: Int, scan: Float): Float =
    ((scan - index) * 2f).coerceIn(0f, 1f) * ((ScanEnd - scan) / 0.8f).coerceIn(0f, 1f)

/** Part 0 is the dot; parts 1–3 are the arcs, smallest first. */
private fun DrawScope.drawWifiPart(index: Int, base: Color, lit: Float) {
    val w = size.width
    val origin = Offset(w / 2, size.height * 0.78f)
    if (index == 0) {
        val r = w * 0.055f
        if (lit > 0f) drawCircle(ErrorColors.Cyan, r * 2.2f, origin, alpha = lit * 0.22f)
        drawCircle(base, r, origin)
        if (lit > 0f) drawCircle(ErrorColors.Cyan, r, origin, alpha = lit)
        return
    }
    val radius = w * (0.08f + index * 0.16f)
    val topLeft = Offset(origin.x - radius, origin.y - radius)
    val arcSize = Size(radius * 2, radius * 2)
    val width = w * 0.075f
    fun arc(color: Color, alpha: Float, strokeWidth: Float) = drawArc(
        color,
        startAngle = 225f,
        sweepAngle = 90f,
        useCenter = false,
        topLeft = topLeft,
        size = arcSize,
        alpha = alpha,
        style = Stroke(strokeWidth, cap = StrokeCap.Round),
    )
    if (lit > 0f) arc(ErrorColors.Cyan, lit * 0.22f, width * 2.2f)
    arc(base, 1f, width)
    if (lit > 0f) arc(ErrorColors.Cyan, lit, width)
}

/** The red slash, drawn over a card-coloured gap so it reads as cutting through the arcs. */
private fun DrawScope.drawSlash(progress: Float) {
    val start = Offset(size.width * 0.16f, size.height * 0.16f)
    val end = Offset(size.width * 0.84f, size.height * 0.86f)
    val tip = start + (end - start) * progress
    val width = size.width * 0.07f
    drawLine(ErrorColors.Card, start, tip, width * 2.2f, StrokeCap.Round)
    drawLine(ErrorColors.Red, start, tip, width, StrokeCap.Round)
}
