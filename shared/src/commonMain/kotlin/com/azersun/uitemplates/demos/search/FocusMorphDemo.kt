package com.azersun.uitemplates.demos.search

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azersun.uitemplates.demos.menus.MenuColors
import com.azersun.uitemplates.demos.menus.StaggerIn
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private enum class Scene(val label: String) { Peaks("Peaks"), Ocean("Ocean"), Garden("Garden") }

private val RecentSearches = listOf("lamp", "desk chair", "rug")
private val BarTop = 52.dp
private val CancelSlot = 76.dp
private val TilesTop = BarTop + FieldHeight + 24.dp
private val RecentTop = FieldHeight + 24.dp

@Composable
fun FocusMorphDemo() {
    val autoplay = rememberAutoplay()
    val query = remember { Query() }
    var open by remember { mutableStateOf(false) }
    val morph = remember { Animatable(0f) }
    val showRecent by remember { derivedStateOf { morph.value > 0.02f } }
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(focused) {
        if (focused) open = true
    }
    LaunchedEffect(open) {
        morph.animateTo(if (open) 1f else 0f, spring(dampingRatio = 0.8f, stiffness = 260f))
    }

    fun cancel() {
        open = false
        autoplay.focus = false
        query.set("")
        focusManager.clearFocus()
    }

    AutoplayEffect(autoplay) {
        open = false
        autoplay.focus = false
        query.set("")
        delay(AutoplayDelayMillis)
        open = true
        autoplay.focus = true
        delay(2000)
        cancel()
    }

    SearchStage(5, "Focus Morph", "Tap the bar: the title slides away, the bar rises, Cancel slides in.", autoplay, showReplay = false) {
        BoxWithConstraints(
            Modifier
                .align(Alignment.TopCenter)
                .padding(top = 28.dp, start = 20.dp, end = 20.dp)
                .widthIn(max = 380.dp)
                .fillMaxWidth(),
        ) {
            val density = LocalDensity.current
            val fullPx = constraints.maxWidth.toFloat()
            val cancelPx = with(density) { CancelSlot.toPx() }

            Text(
                "Explore",
                Modifier
                    .height(40.dp)
                    .graphicsLayer {
                        val f = morph.value
                        translationY = -f * 34.dp.toPx()
                        alpha = (1f - f * 1.6f).coerceIn(0f, 1f)
                    },
                color = SearchColors.Text,
                fontSize = 30.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.Bold,
            )
            Row(
                Modifier
                    .padding(top = TilesTop)
                    .fillMaxWidth()
                    .graphicsLayer {
                        val f = morph.value
                        alpha = (1f - f * 1.5f).coerceIn(0f, 1f)
                        translationY = f * 30.dp.toPx()
                    },
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Scene.entries.forEach { scene -> SceneTile(scene, Modifier.weight(1f)) }
            }
            if (open || showRecent) {
                Column(Modifier.padding(top = RecentTop)) {
                    StaggerIn(0, open, offsetX = 0f, offsetY = 10f) {
                        Text("Recent", color = SearchColors.Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RecentSearches.forEachIndexed { i, label ->
                            StaggerIn(i + 1, open, offsetX = 0f, offsetY = 10f, stepMillis = 60) {
                                RecentChip(label) { query.set(label) }
                            }
                        }
                    }
                }
            }
            SearchField(
                query = query,
                placeholder = "Search places, ideas…",
                modifier = Modifier
                    .graphicsLayer { translationY = (1f - morph.value) * BarTop.toPx() }
                    .layout { measurable, constraints ->
                        val width = (fullPx - cancelPx * morph.value).coerceAtLeast(0f).roundToInt()
                        val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
                        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                    }
                    .clickable(enabled = !open, interactionSource = null, indication = null) {
                        open = true
                        focusRequester.requestFocus()
                    },
                glowing = autoplay.focus,
                caret = autoplay.focus,
                interactionSource = interaction,
                focusRequester = focusRequester,
            )
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .width(CancelSlot)
                    .height(FieldHeight)
                    .graphicsLayer {
                        val f = morph.value
                        translationX = (1f - f) * 90.dp.toPx()
                        alpha = f.coerceIn(0f, 1f)
                    }
                    .clickable(enabled = open, interactionSource = null, indication = null, onClick = ::cancel),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Text("Cancel", color = MenuColors.Violet, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun RecentChip(label: String, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(Pill)
            .background(SearchColors.Row)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(12.dp)) { drawClock(SearchColors.Muted) }
        Spacer(Modifier.width(6.dp))
        Text(label, color = SearchColors.Text, fontSize = 13.sp)
    }
}

@Composable
private fun SceneTile(scene: Scene, modifier: Modifier) {
    Column(modifier) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .aspectRatio(0.82f)
                .clip(RoundedCornerShape(16.dp)),
        ) {
            when (scene) {
                Scene.Peaks -> drawPeaks()
                Scene.Ocean -> drawOcean()
                Scene.Garden -> drawGarden()
            }
        }
        Text(scene.label, Modifier.padding(top = 6.dp, start = 2.dp), color = SearchColors.Text, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

private fun DrawScope.drawPeaks() {
    val w = size.width
    val h = size.height
    drawRect(Brush.verticalGradient(listOf(Color(0xFF5B4BD6), Color(0xFF8C7BF0))))
    drawCircle(MenuColors.Yellow, w * 0.11f, Offset(w * 0.72f, h * 0.26f))
    val back = Path().apply {
        moveTo(-w * 0.1f, h)
        lineTo(w * 0.34f, h * 0.4f)
        lineTo(w * 0.78f, h)
        close()
    }
    drawPath(back, Color(0xFF2E2470))
    val snow = Path().apply {
        moveTo(w * 0.34f, h * 0.4f)
        lineTo(w * 0.25f, h * 0.52f)
        lineTo(w * 0.34f, h * 0.49f)
        lineTo(w * 0.43f, h * 0.52f)
        close()
    }
    drawPath(snow, Color.White.copy(alpha = 0.9f))
    val front = Path().apply {
        moveTo(w * 0.22f, h)
        lineTo(w * 0.68f, h * 0.52f)
        lineTo(w * 1.1f, h)
        close()
    }
    drawPath(front, Color(0xFF1C1650))
}

private fun DrawScope.drawOcean() {
    val w = size.width
    val h = size.height
    val horizon = h * 0.52f
    drawRect(Brush.verticalGradient(listOf(Color(0xFF3B82F6), Color(0xFF7DD3FC)), endY = horizon), size = size.copy(height = horizon))
    drawCircle(Color(0xFFFFD68A), w * 0.13f, Offset(w * 0.5f, horizon))
    drawRect(
        Brush.verticalGradient(listOf(Color(0xFF1E5BB8), Color(0xFF123A7A)), startY = horizon, endY = h),
        topLeft = Offset(0f, horizon),
        size = size.copy(height = h - horizon),
    )
    val stroke = Stroke(w * 0.025f, cap = StrokeCap.Round)
    repeat(3) { row ->
        val y = horizon + (h - horizon) * (0.25f + row * 0.25f)
        val wave = Path().apply {
            moveTo(w * 0.08f, y)
            var x = w * 0.08f
            while (x < w * 0.92f) {
                x += w * 0.02f
                lineTo(x, y + sin((x / w) * 4f * PI.toFloat() + row) * h * 0.02f)
            }
        }
        drawPath(wave, Color.White.copy(alpha = 0.45f - row * 0.1f), style = stroke)
    }
}

private fun DrawScope.drawGarden() {
    val w = size.width
    val h = size.height
    drawRect(Brush.verticalGradient(listOf(Color(0xFF34D399), Color(0xFF0F766E))))
    drawCircle(Color(0xFF0B5A50), w * 0.9f, Offset(w * 0.5f, h * 1.62f))
    val stem = w * 0.035f
    val flowers = listOf(
        Triple(0.26f, 0.5f, MenuColors.Pink),
        Triple(0.52f, 0.38f, MenuColors.Yellow),
        Triple(0.76f, 0.56f, Color.White),
    )
    flowers.forEach { (x, top, color) ->
        drawLine(Color(0xFF14532D), Offset(w * x, h * top), Offset(w * x, h * 0.92f), stem, StrokeCap.Round)
        val center = Offset(w * x, h * top)
        repeat(5) { i ->
            val a = i * 2f * PI.toFloat() / 5f
            drawCircle(color, w * 0.055f, center + Offset(cos(a), sin(a)) * (w * 0.06f))
        }
        drawCircle(Color(0xFFF59E0B), w * 0.04f, center)
    }
}
