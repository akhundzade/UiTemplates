package com.azersun.uitemplates.demos.menus

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

internal object MenuColors {
    val Background = Brush.verticalGradient(listOf(Color(0xFF14122B), Color(0xFF0B0A18)))
    val Card = Color(0xFF1A1833)
    val CardBorder = Color(0xFF2C2952)
    val Screen = Color(0xFF12112A)
    val Tile = Color(0xFF232046)
    val TileLight = Color(0xFF2E2A5C)
    val Text = Color(0xFFF1F0FF)
    val Muted = Color(0xFF8E8AB8)
    val Violet = Color(0xFF7C5CFF)
    val Blue = Color(0xFF4F7BFF)
    val Cyan = Color(0xFF22D3EE)
    val Pink = Color(0xFFFF5CA8)
    val Yellow = Color(0xFFFFC53D)
    val Red = Color(0xFFFF5C6C)
    val Accent = Brush.linearGradient(listOf(Violet, Cyan))
}

/** Full-screen stage with the reel's numbered caption under the demo. */
@Composable
internal fun MenuStage(
    number: Int,
    title: String,
    caption: String,
    content: @Composable BoxScope.() -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MenuColors.Background)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .weight(1f)
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(MenuColors.Card)
                .border(1.dp, MenuColors.CardBorder, RoundedCornerShape(26.dp)),
            contentAlignment = Alignment.Center,
            content = content,
        )
        Column(
            Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .padding(top = 14.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0x99000000))
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text("$number  $title", color = MenuColors.Text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(caption, color = MenuColors.Muted, fontSize = 12.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

/** A generic phone screen the drawer, sheet and reveal demos sit on top of. */
@Composable
internal fun MockScreen(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit,
) {
    Column(
        modifier
            .fillMaxSize()
            .background(MenuColors.Screen)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        topBar()
        Box(
            Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF3B2F8F), Color(0xFF2A4C9E)))),
        ) {
            Column(Modifier.align(Alignment.BottomStart).padding(14.dp)) {
                Bar(110.dp, 9.dp, Color.White.copy(alpha = 0.85f))
                Spacer(Modifier.height(6.dp))
                Bar(70.dp, 7.dp, Color.White.copy(alpha = 0.45f))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(3) {
                Box(
                    Modifier
                        .weight(1f)
                        .height(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MenuColors.Tile),
                )
            }
        }
        repeat(4) { i ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MenuColors.Tile)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(28.dp).clip(CircleShape).background(MenuColors.TileLight))
                Column(Modifier.padding(start = 10.dp)) {
                    Bar((90 + i * 14).dp, 7.dp, MenuColors.Muted.copy(alpha = 0.6f))
                    Spacer(Modifier.height(5.dp))
                    Bar(54.dp, 6.dp, MenuColors.Muted.copy(alpha = 0.3f))
                }
            }
        }
    }
}

@Composable
internal fun Bar(width: Dp, height: Dp, color: Color) {
    Box(Modifier.width(width).height(height).clip(RoundedCornerShape(50)).background(color))
}

@Composable
internal fun MockTopBar(leading: @Composable () -> Unit = {}, trailing: @Composable () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().heightIn(min = 36.dp), verticalAlignment = Alignment.CenterVertically) {
        leading()
        Spacer(Modifier.weight(1f))
        Bar(80.dp, 8.dp, MenuColors.Muted.copy(alpha = 0.5f))
        Spacer(Modifier.weight(1f))
        trailing()
    }
}

/** Minimal stroked icons so the demos have no icon-library dependency. */
internal enum class Glyph { Image, Pencil, Mic, Pin, Camera, Link, User, Gear, Bookmark, LogOut, Home, Bell }

@Composable
internal fun GlyphIcon(glyph: Glyph, color: Color, size: Dp = 18.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) { drawGlyph(glyph, color) }
}

internal fun DrawScope.drawGlyph(glyph: Glyph, color: Color) {
    val w = size.width
    val s = Stroke(width = w * 0.1f, cap = StrokeCap.Round)
    fun line(x1: Float, y1: Float, x2: Float, y2: Float) =
        drawLine(color, Offset(w * x1, w * y1), Offset(w * x2, w * y2), s.width, StrokeCap.Round)
    fun rect(x: Float, y: Float, rw: Float, rh: Float, r: Float) =
        drawRoundRect(color, Offset(w * x, w * y), Size(w * rw, w * rh), CornerRadius(w * r), style = s)
    when (glyph) {
        Glyph.Image -> {
            rect(0.1f, 0.18f, 0.8f, 0.64f, 0.12f)
            line(0.2f, 0.7f, 0.42f, 0.46f); line(0.42f, 0.46f, 0.58f, 0.6f); line(0.58f, 0.6f, 0.8f, 0.4f)
        }
        Glyph.Pencil -> { line(0.22f, 0.78f, 0.74f, 0.26f); line(0.18f, 0.82f, 0.3f, 0.79f); line(0.64f, 0.16f, 0.84f, 0.36f) }
        Glyph.Mic -> {
            rect(0.36f, 0.1f, 0.28f, 0.48f, 0.14f)
            drawArc(color, 0f, 180f, false, Offset(w * 0.22f, w * 0.3f), Size(w * 0.56f, w * 0.44f), style = s)
            line(0.5f, 0.74f, 0.5f, 0.9f)
        }
        Glyph.Pin -> {
            drawCircle(color, w * 0.26f, Offset(w * 0.5f, w * 0.4f), style = s)
            line(0.32f, 0.58f, 0.5f, 0.9f); line(0.68f, 0.58f, 0.5f, 0.9f)
            drawCircle(color, w * 0.07f, Offset(w * 0.5f, w * 0.4f))
        }
        Glyph.Camera -> {
            rect(0.1f, 0.28f, 0.8f, 0.56f, 0.12f)
            drawCircle(color, w * 0.15f, Offset(w * 0.5f, w * 0.56f), style = s)
            line(0.36f, 0.18f, 0.64f, 0.18f)
        }
        Glyph.Link -> {
            rect(0.08f, 0.36f, 0.48f, 0.28f, 0.14f)
            rect(0.44f, 0.36f, 0.48f, 0.28f, 0.14f)
        }
        Glyph.User -> {
            drawCircle(color, w * 0.18f, Offset(w * 0.5f, w * 0.34f), style = s)
            drawArc(color, 180f, 180f, false, Offset(w * 0.18f, w * 0.6f), Size(w * 0.64f, w * 0.5f), style = s)
        }
        Glyph.Gear -> {
            drawCircle(color, w * 0.16f, Offset(w * 0.5f, w * 0.5f), style = s)
            repeat(8) { i ->
                val a = i * kotlin.math.PI.toFloat() / 4
                val c = Offset(w * 0.5f, w * 0.5f)
                val dir = Offset(kotlin.math.cos(a), kotlin.math.sin(a))
                drawLine(color, c + dir * (w * 0.28f), c + dir * (w * 0.4f), s.width, StrokeCap.Round)
            }
        }
        Glyph.Bookmark -> {
            val p = Path().apply {
                moveTo(w * 0.26f, w * 0.12f); lineTo(w * 0.74f, w * 0.12f); lineTo(w * 0.74f, w * 0.88f)
                lineTo(w * 0.5f, w * 0.68f); lineTo(w * 0.26f, w * 0.88f); close()
            }
            drawPath(p, color, style = s)
        }
        Glyph.LogOut -> {
            line(0.42f, 0.5f, 0.88f, 0.5f); line(0.72f, 0.34f, 0.88f, 0.5f); line(0.72f, 0.66f, 0.88f, 0.5f)
            line(0.56f, 0.14f, 0.14f, 0.14f); line(0.14f, 0.14f, 0.14f, 0.86f); line(0.14f, 0.86f, 0.56f, 0.86f)
        }
        Glyph.Home -> {
            line(0.12f, 0.5f, 0.5f, 0.14f); line(0.5f, 0.14f, 0.88f, 0.5f)
            rect(0.24f, 0.44f, 0.52f, 0.44f, 0.06f)
        }
        Glyph.Bell -> {
            drawArc(color, 180f, 180f, false, Offset(w * 0.22f, w * 0.16f), Size(w * 0.56f, w * 0.56f), style = s)
            line(0.22f, 0.44f, 0.22f, 0.7f); line(0.78f, 0.44f, 0.78f, 0.7f); line(0.12f, 0.72f, 0.88f, 0.72f)
            line(0.44f, 0.86f, 0.56f, 0.86f)
        }
    }
}

/** Three-bar menu icon used as the trigger in the drawer and reveal demos. */
@Composable
internal fun MenuBarsIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(18.dp)) {
        val stroke = size.width * 0.12f
        listOf(0.22f, 0.5f, 0.78f).forEach { y ->
            drawLine(color, Offset(size.width * 0.1f, size.height * y), Offset(size.width * 0.9f, size.height * y), stroke, StrokeCap.Round)
        }
    }
}

/** Fades and slides a row in after a per-index delay, and out all at once. */
@Composable
internal fun StaggerIn(
    index: Int,
    visible: Boolean,
    offsetX: Float = -28f,
    offsetY: Float = 0f,
    baseDelayMillis: Long = 90,
    stepMillis: Long = 45,
    content: @Composable () -> Unit,
) {
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(visible) {
        if (visible) {
            delay(baseDelayMillis + index * stepMillis)
            reveal.animateTo(1f, spring(dampingRatio = 0.75f, stiffness = 420f))
        } else {
            reveal.animateTo(0f, tween(120))
        }
    }
    Box(
        Modifier.graphicsLayer {
            alpha = reveal.value.coerceIn(0f, 1f)
            translationX = (1f - reveal.value) * offsetX.dp.toPx()
            translationY = (1f - reveal.value) * offsetY.dp.toPx()
        },
    ) { content() }
}
