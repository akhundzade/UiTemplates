package com.azersun.uitemplates.demos.checkbox1

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private enum class Scene(val label: String) { Peaks("Peaks"), Ocean("Ocean"), Garden("Garden") }

private val CardCorner = 16.dp

@Composable
fun CardSelectDemo() {
    val selected = remember { mutableStateListOf(false, false, false) }
    val count = selected.count { it }

    CheckboxStage(
        5,
        "Card Select",
        "Tap a photo: it shrinks a little, a ✓ badge pops, a gradient frame glows.",
        onReplay = { selected.indices.forEach { selected[it] = false } },
        autoplay = {
            selected[0] = true
            delay(700)
            selected[2] = true
        },
    ) {
        Column(Modifier.padding(horizontal = 20.dp).fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Choose photos", color = CheckColors.Text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text(
                    if (count == 0) "None selected" else "$count selected",
                    color = if (count == 0) CheckColors.Muted else CheckColors.Cyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Scene.entries.forEachIndexed { i, scene ->
                    PhotoCard(scene, selected[i], { selected[i] = !selected[i] }, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun PhotoCard(scene: Scene, selected: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val press by animateFloatAsState(
        if (selected) 0.94f else 1f,
        spring(dampingRatio = 0.55f, stiffness = 380f),
        label = "press",
    )
    val frame by animateFloatAsState(if (selected) 1f else 0f, tween(280), label = "frame")
    val tick by animateFloatAsState(
        if (selected) 1f else 0f,
        tween(if (selected) 260 else 120, delayMillis = if (selected) 140 else 0),
        label = "tick",
    )
    val badge = remember { Animatable(if (selected) 1f else 0f) }
    LaunchedEffect(selected) {
        if (selected) {
            badge.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = 420f))
        } else {
            badge.animateTo(0f, tween(160))
        }
    }

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(0.78f)
                .graphicsLayer {
                    scaleX = press
                    scaleY = press
                }
                .drawWithContent {
                    drawContent()
                    drawGlowFrame(frame)
                }
                .clip(RoundedCornerShape(CardCorner))
                .clickable(interactionSource = null, indication = null, role = Role.Checkbox, onClick = onToggle)
                .drawBehind { drawScene(scene) },
        ) {
            Spacer(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(22.dp)
                    .drawWithCache {
                        val trim = PathTrim(
                            Path().apply { checkMark(Offset(size.width * 0.12f, size.height * 0.14f), size.minDimension * 0.76f) },
                        )
                        val ring = Stroke(1.6.dp.toPx())
                        val checkStyle = checkStroke(size.minDimension * 0.76f)
                        onDrawBehind {
                            val pop = badge.value
                            val t = tick
                            val r = size.minDimension / 2
                            drawCircle(Color.Black.copy(alpha = 0.25f), r)
                            drawCircle(Color.White.copy(alpha = 0.9f), r - ring.width / 2, style = ring)
                            if (pop > 0f) drawCircle(CheckColors.Blue, r * pop)
                            if (t > 0f) drawPath(trim.fraction(0f, t), Color.White, style = checkStyle)
                        }
                    },
            )
        }
        Text(
            scene.label,
            color = if (selected) CheckColors.Text else CheckColors.Muted,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

private fun DrawScope.drawGlowFrame(frame: Float) {
    if (frame <= 0f) return
    val brush = Brush.linearGradient(listOf(CheckColors.Violet, CheckColors.Cyan), Offset.Zero, Offset(size.width, size.height))
    val gap = 4.dp.toPx()
    // Wide faint strokes under a crisp one read as a soft glow without a blur effect.
    listOf(12.dp to 0.12f, 7.dp to 0.22f, 2.5.dp to 1f).forEach { (width, alpha) ->
        drawRoundRect(
            brush,
            Offset(-gap, -gap),
            Size(size.width + gap * 2, size.height + gap * 2),
            CornerRadius((CardCorner + 4.dp).toPx()),
            alpha = alpha * frame,
            style = Stroke(width.toPx()),
        )
    }
}

private fun DrawScope.drawScene(scene: Scene) {
    val w = size.width
    val h = size.height
    fun poly(vararg points: Float): Path = Path().apply {
        moveTo(points[0] * w, points[1] * h)
        for (i in 2 until points.size step 2) lineTo(points[i] * w, points[i + 1] * h)
        close()
    }
    when (scene) {
        Scene.Peaks -> {
            drawRect(Brush.verticalGradient(listOf(Color(0xFF4B3FA8), Color(0xFFE59BC0))))
            drawCircle(Color(0xFFFFD89A), w * 0.13f, Offset(w * 0.7f, h * 0.27f))
            drawPath(poly(-0.1f, 1f, 0.38f, 0.38f, 0.82f, 1f), Color(0xFF6E5AC8))
            drawPath(poly(0.25f, 1f, 0.7f, 0.48f, 1.15f, 1f), Color(0xFF3A2F86))
            drawPath(poly(0.38f, 0.38f, 0.3f, 0.48f, 0.35f, 0.47f, 0.4f, 0.52f, 0.46f, 0.46f), Color.White.copy(alpha = 0.9f))
            drawPath(poly(0.7f, 0.48f, 0.63f, 0.56f, 0.69f, 0.55f, 0.73f, 0.59f, 0.77f, 0.56f), Color.White.copy(alpha = 0.85f))
        }
        Scene.Ocean -> {
            drawRect(Brush.verticalGradient(listOf(Color(0xFF3F8CFF), Color(0xFFA9DCFF))))
            drawCircle(Color(0xFFFFF1B8), w * 0.12f, Offset(w * 0.32f, h * 0.3f))
            drawRect(
                Brush.verticalGradient(listOf(Color(0xFF1F5FD6), Color(0xFF173A8C)), startY = h * 0.55f, endY = h),
                Offset(0f, h * 0.55f),
                Size(w, h * 0.45f),
            )
            val wave = Stroke(2.dp.toPx())
            listOf(0.64f, 0.76f, 0.88f).forEachIndexed { i, y ->
                val path = Path().apply {
                    val shift = if (i % 2 == 0) 0f else w * 0.12f
                    moveTo(-w * 0.1f + shift, h * y)
                    var x = -w * 0.1f + shift
                    while (x < w) {
                        quadraticTo(x + w * 0.06f, h * y - 4.dp.toPx(), x + w * 0.12f, h * y)
                        quadraticTo(x + w * 0.18f, h * y + 4.dp.toPx(), x + w * 0.24f, h * y)
                        x += w * 0.24f
                    }
                }
                drawPath(path, Color.White.copy(alpha = 0.35f), style = wave)
            }
        }
        Scene.Garden -> {
            drawRect(Brush.verticalGradient(listOf(Color(0xFF7FD8C0), Color(0xFFE6FFF4))))
            drawCircle(Color(0xFF3FAE7A), w * 0.75f, Offset(w * 0.15f, h * 1.25f))
            drawCircle(Color(0xFF2E8F62), w * 0.8f, Offset(w * 0.95f, h * 1.35f))
            val stem = 1.6.dp.toPx()
            listOf(
                Triple(0.22f, 0.7f, Color(0xFFFF5CA8)),
                Triple(0.45f, 0.78f, Color(0xFFFFC53D)),
                Triple(0.68f, 0.72f, Color(0xFFFF8A5C)),
                Triple(0.84f, 0.84f, Color(0xFFFF5CA8)),
            ).forEach { (x, y, color) ->
                drawLine(Color(0xFF1F6B47), Offset(w * x, h * y), Offset(w * x, h * (y + 0.12f)), stem)
                drawCircle(color, w * 0.06f, Offset(w * x, h * y))
                drawCircle(Color(0xFFFFF4C2), w * 0.022f, Offset(w * x, h * y))
            }
        }
    }
}
