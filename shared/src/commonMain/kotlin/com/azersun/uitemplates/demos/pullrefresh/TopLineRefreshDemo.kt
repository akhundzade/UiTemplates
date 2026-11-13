package com.azersun.uitemplates.demos.pullrefresh

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.azersun.uitemplates.demos.menus.MenuColors

private const val LightPassMillis = 650

@Composable
fun TopLineRefreshDemo() {
    val state = rememberPullRefreshState()
    PullRefreshScene(
        number = 1,
        title = "Top Line",
        caption = "A line grows out from the middle as you pull; while it refreshes a light runs back and forth along it.",
        state = state,
    ) {
        TopLineIndicator(state)
    }
}

@Composable
private fun TopLineIndicator(state: PullRefreshState) {
    val refreshing = state.phase == RefreshPhase.Refreshing
    val armed by remember { derivedStateOf { state.progress >= 1f || state.phase != RefreshPhase.Pulling } }
    val charge by animateFloatAsState(if (armed) 1f else 0f, tween(220), label = "charge")
    val light = remember { Animatable(0f) }

    LaunchedEffect(refreshing) {
        if (!refreshing) return@LaunchedEffect
        light.snapTo(0f)
        while (true) {
            light.animateTo(1f, tween(LightPassMillis, easing = FastOutSlowInEasing))
            light.animateTo(0f, tween(LightPassMillis, easing = FastOutSlowInEasing))
        }
    }

    Canvas(Modifier.fillMaxSize()) {
        val pull = state.pull
        val runner = light.value
        val grow = (pull / state.thresholdPx).coerceIn(0f, 1f)
        if (grow <= 0f) return@Canvas
        val y = pull / 2
        val half = size.width * 0.36f * grow
        val start = Offset(size.width / 2 - half, y)
        val end = Offset(size.width / 2 + half, y)
        val stroke = 3.dp.toPx()

        val dim = MenuColors.Muted.copy(alpha = 0.45f)
        drawLine(
            Brush.horizontalGradient(
                listOf(lerp(dim, MenuColors.Violet, charge), lerp(dim, MenuColors.Cyan, charge)),
                startX = start.x,
                endX = end.x,
            ),
            start,
            end,
            stroke,
            StrokeCap.Round,
        )

        if (refreshing) {
            val x = start.x + (end.x - start.x) * runner
            val reach = 44.dp.toPx()
            drawCircle(
                Brush.radialGradient(listOf(MenuColors.Cyan.copy(alpha = 0.45f), Color.Transparent), Offset(x, y), 18.dp.toPx()),
                radius = 18.dp.toPx(),
                center = Offset(x, y),
            )
            drawLine(
                Brush.horizontalGradient(
                    listOf(Color.Transparent, Color.White, Color.Transparent),
                    startX = x - reach,
                    endX = x + reach,
                ),
                Offset((x - reach).coerceAtLeast(start.x), y),
                Offset((x + reach).coerceAtMost(end.x), y),
                stroke,
                StrokeCap.Round,
            )
        }
    }
}
