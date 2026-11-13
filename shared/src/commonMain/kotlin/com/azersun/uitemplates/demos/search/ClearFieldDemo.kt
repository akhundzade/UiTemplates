package com.azersun.uitemplates.demos.search

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private const val LetterStepMillis = 38L
private const val LetterFallMillis = 560

/** Position grows with the square of time, like a dropped object. */
private val Gravity = Easing { it * it }

private class FallingText(val text: String, val layout: TextLayoutResult, val id: Int)

private fun resultCount(query: String): Int = (168 - query.length * 4).coerceAtLeast(3)

@Composable
fun ClearFieldDemo() {
    val autoplay = rememberAutoplay()
    val query = remember { Query("headphones") }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var falling by remember { mutableStateOf<FallingText?>(null) }
    var fallCount by remember { mutableIntStateOf(0) }
    var resultsFor by remember { mutableStateOf(query.text) }
    val hasText = query.text.isNotBlank()
    val resultsAlpha by animateFloatAsState(if (hasText) 1f else 0f, tween(220), label = "results")

    LaunchedEffect(query.text) {
        if (query.text.isNotBlank()) resultsFor = query.text.trim()
    }
    LaunchedEffect(falling) {
        val current = falling ?: return@LaunchedEffect
        delay(current.text.length * LetterStepMillis + LetterFallMillis + 100)
        if (falling === current) falling = null
    }

    fun clear() {
        val current = layout
        if (query.text.isNotEmpty() && current != null) {
            fallCount++
            falling = FallingText(query.text, current, fallCount)
        }
        query.set("")
    }

    AutoplayEffect(autoplay) {
        autoplay.focus = false
        falling = null
        query.set("headphones")
        delay(AutoplayDelayMillis)
        clear()
        delay(1150)
        autoplay.focus = true
        query.type("headphone")
    }

    SearchStage(3, "Clear Field", "Tap ×: the letters fall away, the placeholder fades in, then retype.", autoplay) {
        Column(
            Modifier
                .align(Alignment.TopCenter)
                .padding(top = 96.dp, start = 20.dp, end = 20.dp)
                .widthIn(max = 380.dp)
                .fillMaxWidth(),
        ) {
            SearchField(
                query = query,
                placeholder = "Search",
                modifier = Modifier.zIndex(1f),
                glowing = autoplay.focus,
                caret = autoplay.focus,
                placeholderDelayMillis = 320,
                onTextLayout = { layout = it },
                trailing = { ClearButton(visible = query.text.isNotEmpty(), onClick = ::clear) },
                textOverlay = { falling?.let { FallingLetters(it) } },
            )
            Text(
                "${resultCount(resultsFor)} results for “$resultsFor”",
                Modifier
                    .padding(start = 18.dp, top = 12.dp)
                    .graphicsLayer {
                        alpha = resultsAlpha
                        translationY = (1f - resultsAlpha) * -4.dp.toPx()
                    },
                color = SearchColors.Muted,
                fontSize = 12.sp,
            )
        }
    }
}

/** Copies of the cleared letters, drawn where the field had them, dropping one after another. */
@Composable
private fun BoxScope.FallingLetters(falling: FallingText) {
    key(falling.id) {
        Box(Modifier.matchParentSize()) {
            falling.text.forEachIndexed { index, char ->
                if (!char.isWhitespace()) {
                    val box = falling.layout.getBoundingBox(index)
                    FallingLetter(char, index, box.left, box.top)
                }
            }
        }
    }
}

@Composable
private fun FallingLetter(char: Char, index: Int, x: Float, y: Float) {
    val fall = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index * LetterStepMillis)
        fall.animateTo(1f, tween(LetterFallMillis, easing = Gravity))
    }
    // A fixed pseudo-random tumble per letter, so neighbours spin different ways.
    val spin = ((index * 53) % 9 - 4) * 12f
    val sway = if (index % 2 == 0) -1f else 1f
    Text(
        char.toString(),
        Modifier
            .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
            .graphicsLayer {
                val p = fall.value
                translationY = p * 40.dp.toPx()
                translationX = p * sway * 5.dp.toPx()
                rotationZ = p * spin
                alpha = 1f - p
            },
        style = QueryStyle,
        softWrap = false,
    )
}
