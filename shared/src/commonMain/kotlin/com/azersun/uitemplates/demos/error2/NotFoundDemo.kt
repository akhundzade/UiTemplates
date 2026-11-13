package com.azersun.uitemplates.demos.error2

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.sqrt

private const val SearchMillis = 2600
private const val MorphMillis = 700

/** Text-line widths on the placeholder page, as fractions of the line area. */
private val LineWidths = floatArrayOf(0.92f, 0.74f, 0.84f, 0.5f, 0.66f)
private val PageLine = ErrorColors.Muted.copy(alpha = 0.3f)
private val PageBlock = Color(0xFF2A2752)

@Composable
fun NotFoundDemo() {
    val search = remember { Animatable(0f) }
    val morph = remember { Animatable(0f) }
    val fours = remember { Animatable(0f) }
    val caption = remember { Animatable(0f) }
    var job by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()

    fun restart(play: Boolean) {
        job?.cancel()
        job = scope.launch {
            search.snapTo(0f)
            morph.snapTo(0f)
            fours.snapTo(0f)
            caption.snapTo(0f)
            if (!play) return@launch
            search.animateTo(1f, tween(SearchMillis, easing = FastOutSlowInEasing))
            launch {
                delay(MorphMillis / 2L)
                fours.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 380f))
            }
            launch {
                delay(MorphMillis - 100L)
                caption.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
            }
            morph.animateTo(1f, tween(MorphMillis, easing = FastOutSlowInEasing))
        }
    }

    ErrorStage(
        5,
        "404 Not Found",
        "Magnifier searches the page, then its lens becomes the 0 in \"404\".",
        onReplay = { restart(play = false) },
        autoplay = { restart(play = true) },
    ) {
        Column(
            Modifier.padding(horizontal = 24.dp).widthIn(max = 340.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(
                Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .drawWithCache {
                        val lens = Path()
                        val four = Path()
                        onDrawBehind {
                            drawSearch(search.value, morph.value, fours.value, lens, four)
                        }
                    },
            )
            Column(
                Modifier.graphicsLayer {
                    alpha = caption.value
                    translationY = (1f - caption.value) * 10.dp.toPx()
                },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Page not found", color = ErrorColors.Text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "We looked everywhere for this page.",
                    color = ErrorColors.Muted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

private fun DrawScope.drawSearch(search: Float, morph: Float, fours: Float, lens: Path, four: Path) {
    val w = size.width
    val h = size.height
    val pageAlpha = 1f - (morph * 1.6f).coerceIn(0f, 1f)
    if (pageAlpha > 0f) drawPage(pageAlpha, highlight = false)

    if (search <= 0f) return
    // A figure-eight over the page that starts and ends in the middle.
    val turn = 2f * PI.toFloat() * search
    val wander = Offset(w * (0.5f + 0.3f * sin(turn)), h * (0.5f + 0.2f * sin(2f * turn)))
    val home = Offset(w / 2f, h * 0.46f)
    val at = Offset(lerp(wander.x, home.x, morph), lerp(wander.y, home.y, morph))

    val unit = minOf(w, h)
    val glass = unit * 0.15f
    val rx = lerp(glass, unit * 0.11f, morph)
    val ry = lerp(glass, unit * 0.155f, morph)
    val ring = lerp(unit * 0.035f, unit * 0.05f, morph)
    val appear = (search * 10f).coerceIn(0f, 1f)
    val ink = ErrorColors.Text

    if (pageAlpha > 0f) {
        lens.reset()
        lens.addOval(Rect(at.x - rx, at.y - ry, at.x + rx, at.y + ry))
        clipPath(lens) {
            // Covers the page underneath so only the magnified copy shows through the glass.
            drawRect(ErrorColors.Card, alpha = pageAlpha * appear)
            drawRect(ErrorColors.Tile, alpha = 0.6f * pageAlpha * appear)
            drawCircle(Color.White, glass * 2f, at, alpha = 0.06f * pageAlpha * appear)
            scale(1.35f, at) { drawPage(pageAlpha * appear, highlight = true) }
        }
    }

    val handleAlpha = (1f - morph * 2.2f).coerceIn(0f, 1f) * appear
    if (handleAlpha > 0f) {
        val dir = Offset(sqrt(0.5f), sqrt(0.5f))
        drawLine(ink, at + dir * (glass + ring * 0.4f), at + dir * (glass + unit * 0.13f), unit * 0.05f, StrokeCap.Round, alpha = handleAlpha)
    }
    drawOval(ink, Offset(at.x - rx, at.y - ry), Size(rx * 2, ry * 2), alpha = appear, style = Stroke(ring))

    if (fours > 0f) {
        val digitW = unit * 0.17f
        val digitH = ry * 2
        val gap = unit * 0.06f
        four.reset()
        four.moveTo(digitW * 0.72f, digitH)
        four.lineTo(digitW * 0.72f, 0f)
        four.lineTo(0f, digitH * 0.66f)
        four.lineTo(digitW, digitH * 0.66f)
        val alpha = fours.coerceIn(0f, 1f)
        repeat(2) { k ->
            val side = k * 2 - 1
            val finalCenter = Offset(home.x + side * (rx + gap + digitW / 2), home.y)
            val c = at + (finalCenter - at) * fours
            translate(c.x - digitW / 2, c.y - digitH / 2) {
                scale(0.4f + 0.6f * alpha, Offset(digitW / 2, digitH / 2)) {
                    drawPath(four, ink, alpha = alpha, style = Stroke(ring, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
            }
        }
    }
}

/** A generic article: avatar and title, a hero block and a few text lines. [highlight] is the magnified copy. */
private fun DrawScope.drawPage(alpha: Float, highlight: Boolean) {
    val w = size.width
    val h = size.height
    val boost = if (highlight) 1.8f else 1f
    val line = PageLine.copy(alpha = (PageLine.alpha * boost).coerceAtMost(1f))
    val radius = CornerRadius(h * 0.015f)
    if (!highlight) {
        drawRoundRect(ErrorColors.Tile, cornerRadius = CornerRadius(18.dp.toPx()), alpha = alpha * 0.6f)
    }
    drawCircle(line, w * 0.05f, Offset(w * 0.13f, h * 0.1f), alpha = alpha)
    drawRoundRect(line, Offset(w * 0.22f, h * 0.085f), Size(w * 0.4f, h * 0.03f), radius, alpha = alpha)
    drawRoundRect(
        if (highlight) ErrorColors.Violet.copy(alpha = 0.45f) else PageBlock,
        Offset(w * 0.08f, h * 0.19f),
        Size(w * 0.84f, h * 0.3f),
        CornerRadius(12.dp.toPx()),
        alpha = alpha,
    )
    LineWidths.forEachIndexed { i, fraction ->
        drawRoundRect(line, Offset(w * 0.08f, h * (0.57f + i * 0.08f)), Size(w * 0.84f * fraction, h * 0.03f), radius, alpha = alpha)
    }
}
