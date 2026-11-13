package com.azersun.uitemplates.demos.like2

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azersun.uitemplates.demos.like.LikeColors
import com.azersun.uitemplates.demos.like.LikeStage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val ToastHoldMillis = 1700L
private val ToastShape = RoundedCornerShape(50)

@Composable
fun BookmarkSaveDemo() {
    var saved by remember { mutableStateOf(false) }
    val stretch = remember { Animatable(1f) }
    val fill = remember { Animatable(0f) }
    val toast = remember { Animatable(0f) }
    var toastJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()

    fun toggle() {
        saved = !saved
        toastJob?.cancel()
        if (saved) {
            scope.launch {
                stretch.animateTo(1.2f, tween(150, easing = FastOutSlowInEasing))
                stretch.animateTo(1f, spring(dampingRatio = 0.3f, stiffness = 450f))
            }
            scope.launch {
                delay(60)
                fill.animateTo(1f, tween(320, easing = FastOutSlowInEasing))
            }
            toastJob = scope.launch {
                delay(200)
                toast.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 500f))
                delay(ToastHoldMillis)
                toast.animateTo(0f, tween(220))
            }
        } else {
            scope.launch {
                stretch.animateTo(0.9f, tween(110))
                stretch.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 500f))
            }
            scope.launch { fill.animateTo(0f, tween(220)) }
            toastJob = scope.launch { toast.animateTo(0f, tween(150)) }
        }
    }

    LikeStage(
        3,
        "Bookmark Save",
        "Tap: the bookmark stretches, fills violet→cyan, a \"Saved\" toast pops.",
        autoplay = { toggle() },
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(
                Modifier
                    .size(96.dp, 124.dp)
                    .graphicsLayer {
                        val s = stretch.value
                        scaleY = s
                        scaleX = 1f - (s - 1f) * 0.5f
                        transformOrigin = TransformOrigin(0.5f, 0f)
                    }
                    .clickable(
                        interactionSource = null,
                        indication = null,
                        onClickLabel = if (saved) "Remove bookmark" else "Save",
                        role = Role.Button,
                        onClick = ::toggle,
                    )
                    .drawWithCache {
                        val inset = 12.dp.toPx()
                        val left = inset
                        val right = size.width - inset
                        val top = inset
                        val bottom = size.height - inset
                        val r = 10.dp.toPx()
                        val notch = (bottom - top) * 0.26f
                        val bookmark = Path().apply {
                            moveTo(left, top + r)
                            quadraticTo(left, top, left + r, top)
                            lineTo(right - r, top)
                            quadraticTo(right, top, right, top + r)
                            lineTo(right, bottom)
                            lineTo(size.width / 2, bottom - notch)
                            lineTo(left, bottom)
                            close()
                        }
                        val brush = Brush.verticalGradient(listOf(LikeColors.Violet, LikeColors.Cyan), top, bottom)
                        val outline = Stroke(4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        onDrawBehind {
                            val f = fill.value
                            drawPath(bookmark, LikeColors.Outline, alpha = 1f - f, style = outline)
                            if (f > 0f) {
                                // Pours in from the top, the way the stretch pulls the bookmark down.
                                clipRect(bottom = size.height * f) {
                                    drawPath(bookmark, brush)
                                    drawPath(bookmark, brush, style = outline)
                                }
                            }
                        }
                    },
            )
            SavedToast(
                Modifier
                    .padding(top = 18.dp)
                    .graphicsLayer {
                        val t = toast.value
                        alpha = t.coerceIn(0f, 1f)
                        val s = 0.85f + 0.15f * t
                        scaleX = s
                        scaleY = s
                        translationY = (1f - t) * 12.dp.toPx()
                        transformOrigin = TransformOrigin(0.5f, 0f)
                    },
            )
        }
    }
}

@Composable
private fun SavedToast(modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(ToastShape)
            .background(Color(0xFF2A2752))
            .border(1.dp, Color(0xFF3A3670), ToastShape)
            .padding(start = 10.dp, end = 16.dp, top = 9.dp, bottom = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(20.dp)) {
            drawCircle(LikeColors.Cyan)
            val w = size.width
            val stroke = w * 0.12f
            val ink = Color(0xFF0B0A18)
            drawLine(ink, Offset(w * 0.3f, w * 0.52f), Offset(w * 0.45f, w * 0.66f), stroke, StrokeCap.Round)
            drawLine(ink, Offset(w * 0.45f, w * 0.66f), Offset(w * 0.72f, w * 0.38f), stroke, StrokeCap.Round)
        }
        Spacer(Modifier.width(9.dp))
        Text("Saved to collection", color = LikeColors.Text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.width(12.dp))
        Text("View", color = LikeColors.Cyan, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}
