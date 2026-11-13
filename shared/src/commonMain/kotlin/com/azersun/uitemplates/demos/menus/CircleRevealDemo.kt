package com.azersun.uitemplates.demos.menus

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.max

private val RevealLinks = listOf("Work", "About", "Contact")

@Composable
fun CircleRevealDemo() {
    var open by remember { mutableStateOf(false) }
    val radius = remember { Animatable(0f) }
    val icon = remember { Animatable(0f) }
    var origin by remember { mutableStateOf(Offset.Zero) }
    var screenOrigin by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(open) {
        if (open) {
            launch { icon.animateTo(1f, tween(320, easing = FastOutSlowInEasing)) }
            radius.animateTo(1f, tween(560, easing = FastOutSlowInEasing))
        } else {
            launch { icon.animateTo(0f, tween(320, easing = FastOutSlowInEasing)) }
            radius.animateTo(0f, tween(420, delayMillis = 120, easing = FastOutSlowInEasing))
        }
    }

    MenuStage(6, "Circle Reveal", "A circle grows from the ☰ to fill the screen. Big links fade up.") {
        Box(
            Modifier
                .fillMaxSize()
                .padding(14.dp)
                .clip(RoundedCornerShape(20.dp))
                .onGloballyPositioned { screenOrigin = it.positionInRoot() },
        ) {
            MockScreen(topBar = { MockTopBar(trailing = { Box(Modifier.size(36.dp)) }) })

            Canvas(Modifier.fillMaxSize()) {
                if (radius.value <= 0f) return@Canvas
                val far = max(
                    max(hypot(origin.x, origin.y), hypot(size.width - origin.x, origin.y)),
                    max(hypot(origin.x, size.height - origin.y), hypot(size.width - origin.x, size.height - origin.y)),
                )
                drawCircle(
                    Brush.linearGradient(listOf(MenuColors.Violet, MenuColors.Blue), Offset(size.width, 0f), Offset(0f, size.height)),
                    radius = far * radius.value,
                    center = origin,
                )
            }

            Column(Modifier.align(Alignment.CenterStart).padding(start = 32.dp)) {
                RevealLinks.forEachIndexed { i, label ->
                    StaggerIn(index = i, visible = open, offsetX = 0f, offsetY = 26f, baseDelayMillis = 260, stepMillis = 80) {
                        Text(
                            label,
                            color = Color.White,
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 4.dp).clickable(enabled = open) { open = false },
                        )
                    }
                }
                StaggerIn(index = RevealLinks.size, visible = open, offsetX = 0f, offsetY = 26f, baseDelayMillis = 260, stepMillis = 80) {
                    Text("hello@studio.io", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp, modifier = Modifier.padding(top = 20.dp))
                }
            }

            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(36.dp)
                    .onGloballyPositioned { origin = it.boundsInRoot().center - screenOrigin }
                    .clip(CircleShape)
                    .background(if (open) Color.White.copy(alpha = 0.16f) else Color.Transparent)
                    .clickable { open = !open },
                contentAlignment = Alignment.Center,
            ) {
                MenuMorphIcon(icon.value)
            }
        }
    }
}

/** ☰ at 0, ✕ at 1: the outer bars rotate onto each other while the middle one fades. */
@Composable
private fun MenuMorphIcon(progress: Float) {
    Canvas(Modifier.size(18.dp).graphicsLayer { rotationZ = 90f * progress }) {
        val stroke = size.width * 0.12f
        val c = center
        val half = size.width * 0.42f
        val gap = size.height * 0.28f * (1f - progress)
        fun bar(dy: Float, angle: Float, alpha: Float) {
            val rad = angle * kotlin.math.PI.toFloat() / 180f
            val dir = Offset(kotlin.math.cos(rad), kotlin.math.sin(rad)) * half
            val mid = Offset(c.x, c.y + dy)
            drawLine(Color.White.copy(alpha = alpha), mid - dir, mid + dir, stroke, StrokeCap.Round)
        }
        bar(-gap, 45f * progress, 1f)
        bar(0f, 0f, 1f - progress)
        bar(gap, -45f * progress, 1f)
    }
}
