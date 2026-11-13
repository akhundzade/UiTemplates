package com.azersun.uitemplates.demos.search

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val Places = listOf(
    "sunset", "sunglasses", "sunflower seeds", "sushi bar", "surf shop", "summer market",
    "museum of art", "mountain trails", "beach club", "botanical garden", "bookshop",
    "night market", "coffee roasters", "city park", "cinema", "old town",
)

private const val MaxSuggestions = 4
private const val DebounceMillis = 220L
private const val DropStepMillis = 90L

@Composable
fun LiveSuggestionsDemo() {
    val autoplay = rememberAutoplay()
    val query = remember { Query() }
    // Suggestions follow the query only once typing pauses, like a real search-as-you-type box.
    var settled by remember { mutableStateOf("") }
    LaunchedEffect(query.text) {
        delay(DebounceMillis)
        settled = query.text.trim()
    }
    val suggestions = remember(settled) {
        if (settled.isEmpty()) emptyList() else Places.filter { it.startsWith(settled, ignoreCase = true) }.take(MaxSuggestions)
    }

    AutoplayEffect(autoplay) {
        autoplay.focus = false
        query.set("")
        delay(AutoplayDelayMillis)
        autoplay.focus = true
        query.type("sun")
    }

    SearchStage(2, "Live Suggestions", "Type “sun”: suggestions drop in one by one, with the match in bold.", autoplay) {
        Column(
            Modifier
                .align(Alignment.TopCenter)
                .padding(top = 72.dp, start = 20.dp, end = 20.dp)
                .widthIn(max = 380.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            SearchField(query, "Search places", glowing = autoplay.focus, caret = autoplay.focus)
            Spacer(Modifier.height(4.dp))
            suggestions.forEachIndexed { index, place ->
                key(place) {
                    DropIn(index) {
                        SuggestionRow(place, settled.length, highlighted = index == 0) { query.set(place) }
                    }
                }
            }
        }
    }
}

@Composable
private fun DropIn(index: Int, content: @Composable () -> Unit) {
    val drop = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index * DropStepMillis)
        drop.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = 420f))
    }
    Box(
        Modifier.graphicsLayer {
            val p = drop.value
            alpha = p.coerceIn(0f, 1f)
            translationY = (1f - p) * -16.dp.toPx()
            rotationX = (1f - p) * -24f
        },
    ) { content() }
}

@Composable
private fun SuggestionRow(place: String, matched: Int, highlighted: Boolean, onClick: () -> Unit) {
    val label = remember(place, matched) {
        buildAnnotatedString {
            withStyle(SpanStyle(color = SearchColors.Text, fontWeight = FontWeight.Bold)) { append(place.take(matched)) }
            withStyle(SpanStyle(color = SearchColors.Muted)) { append(place.drop(matched)) }
        }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .then(if (highlighted) Modifier.background(SearchColors.Row) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MagnifierIcon(if (highlighted) SearchColors.Text else SearchColors.Muted, Modifier.size(16.dp))
        Spacer(Modifier.width(14.dp))
        Text(label, Modifier.weight(1f), fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Canvas(Modifier.size(14.dp)) { drawArrowUpRight(SearchColors.Muted) }
    }
}
