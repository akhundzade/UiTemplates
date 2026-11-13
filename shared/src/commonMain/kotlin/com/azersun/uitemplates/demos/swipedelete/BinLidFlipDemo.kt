package com.azersun.uitemplates.demos.swipedelete

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min

private val BarBrush = Brush.horizontalGradient(listOf(DeleteRedLight, DeleteRed))

@Composable
fun BinLidFlipDemo() {
    SwipeDeleteScene(
        number = 1,
        title = "Bin Lid Flip",
        caption = "A red bin is revealed; its lid flips open, then the red bar closes up.",
        spec = SwipeSpec(exitMillis = 480),
    ) { message, state -> BinRow(message, state) }
}

@Composable
private fun BinRow(message: Message, state: SwipeRowState) {
    // The lid snaps shut as the bar starts closing, as if the bin swallowed the row.
    val lidOpen by remember(state) {
        derivedStateOf { if (state.deleting) state.collapse.value == 0f else state.fraction > DeleteThreshold }
    }
    val lid by animateFloatAsState(if (lidOpen) 1f else 0f, spring(dampingRatio = 0.4f, stiffness = 700f), label = "lid")

    Box(Modifier.fillMaxWidth().height(RowHeight).clip(RowShape)) {
        Canvas(Modifier.matchParentSize()) {
            val closing = state.collapse.value
            val travel = rowTravel(state)
            val lidAngle = lid
            // At rest the bar would show as a red fringe around the row's anti-aliased corners.
            if (travel <= 0.5f && !state.deleting) return@Canvas
            val barHeight = size.height * (1f - closing)
            if (barHeight <= 0f) return@Canvas
            drawRoundRect(
                BarBrush,
                size = Size(size.width, barHeight),
                cornerRadius = CornerRadius(min(16.dp.toPx(), barHeight / 2)),
            )
            val revealed = travel.coerceIn(0f, size.width)
            val grow = (revealed / (size.width * DeleteThreshold)).coerceIn(0f, 1f)
            val iconSize = 26.dp.toPx() * (0.7f + 0.3f * grow) * (1f + 0.1f * lidAngle) * (1f - closing)
            val centerX = size.width - max(revealed / 2, 34.dp.toPx())
            drawTrash(Offset(centerX, barHeight / 2), iconSize, Color.White, lidAngle)
        }
        MessageRow(message, Modifier.graphicsLayer { translationX = -rowTravel(state) })
    }
}

/** How far left the row has moved: the finger while dragging, then off the edge once let go. */
private fun rowTravel(state: SwipeRowState): Float {
    if (!state.deleting) return state.drag.value
    val leave = FastOutLinearInEasing.transform(phase(state.exit.value, 0f, 0.55f))
    return lerp(state.releasedDrag, state.width * 1.05f, leave)
}
