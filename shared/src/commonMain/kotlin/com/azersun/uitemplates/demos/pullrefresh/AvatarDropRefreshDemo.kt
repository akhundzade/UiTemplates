package com.azersun.uitemplates.demos.pullrefresh

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.azersun.uitemplates.demos.menus.MenuColors

private val BubbleAvatar = 44.dp
private val BubbleSize = 56.dp
private const val DropMillis = 460

// Ease-in quad: speeds up like a falling object.
private val Gravity = CubicBezierEasing(0.55f, 0.085f, 0.68f, 0.53f)

@Composable
fun AvatarDropRefreshDemo() {
    val state = rememberPullRefreshState()
    val drop = remember { Animatable(1f) }
    var landedId by remember { mutableIntStateOf(-1) }

    // Reset before the row is inserted, so the drop's first frame already starts at the top.
    LaunchedEffect(state.phase) {
        if (state.phase == RefreshPhase.Refreshing) drop.snapTo(0f)
    }
    LaunchedEffect(state.lastInsertedId) {
        val id = state.lastInsertedId
        if (id == -1) return@LaunchedEffect
        drop.animateTo(1f, tween(DropMillis, easing = Gravity))
        landedId = id
    }

    PullRefreshScene(
        number = 5,
        title = "Avatar Drop",
        caption = "The new post's avatar peeks out as you pull, then drops into its row when the refresh lands.",
        state = state,
        rowAvatarHidden = { it.id == state.lastInsertedId && it.id != landedId },
    ) {
        val dropping = state.lastInsertedId != -1 && state.lastInsertedId != landedId
        val post = if (dropping) state.items.firstOrNull { it.id == state.lastInsertedId }?.post ?: state.upcoming else state.upcoming
        AvatarBubble(state, post, dropping, drop = { drop.value })
    }
}

@Composable
private fun AvatarBubble(state: PullRefreshState, post: FeedPost, dropping: Boolean, drop: () -> Float) {
    val phase = state.phase
    val spinner = remember { Animatable(0f) }

    LaunchedEffect(phase) {
        if (phase != RefreshPhase.Refreshing) return@LaunchedEffect
        while (true) spinner.animateTo(spinner.value + 360f, tween(900, easing = LinearEasing))
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val width = constraints.maxWidth.toFloat()
        Box(
            Modifier
                .size(BubbleSize)
                .graphicsLayer {
                    val pull = state.pull
                    val half = BubbleSize.toPx() / 2
                    val center: Offset
                    val scale: Float
                    if (dropping) {
                        val t = drop()
                        val start = Offset(width / 2, state.thresholdPx / 2)
                        // The inserted row is first in the list, which sits `pull` below the top.
                        val end = Offset((RowPadding + AvatarSize / 2).toPx(), pull + (RowHeight / 2).toPx())
                        center = start + (end - start) * t
                        scale = 1f + (AvatarSize / BubbleAvatar - 1f) * t
                    } else {
                        val p = FastOutSlowInEasing.transform(state.progress.coerceIn(0f, 1f))
                        center = Offset(width / 2, pull / 2 - (1f - p) * 14.dp.toPx())
                        scale = if (phase == RefreshPhase.Collapsing) 0f else p
                    }
                    translationX = center.x - half
                    translationY = center.y - half
                    scaleX = scale
                    scaleY = scale
                    alpha = scale.coerceIn(0f, 1f)
                },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val progress = state.progress
                val angle = spinner.value
                if (dropping) return@Canvas
                val stroke = 2.5.dp.toPx()
                val inset = stroke / 2 + 1.dp.toPx()
                val ring = Brush.sweepGradient(listOf(MenuColors.Violet, MenuColors.Cyan, MenuColors.Violet))
                val topLeft = Offset(inset, inset)
                val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
                when (phase) {
                    RefreshPhase.Refreshing, RefreshPhase.Done ->
                        drawArc(ring, angle - 90f, 110f, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                    else ->
                        drawArc(ring, -90f, 360f * progress.coerceIn(0f, 1f), false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                }
            }
            Avatar(post, BubbleAvatar)
        }
    }
}
