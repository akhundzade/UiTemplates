package com.azersun.uitemplates.showcase

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState

private val ShowcaseBackground = Color(0xFF0B0B14)
private val CardBackground = Color(0xFF16162A)
private val CardBorder = Color(0xFF26264A)
private val Muted = Color(0xFF9A9AC0)
private val Accent = Color(0xFF8B7CFF)

private val allDemos: List<Demo> get() = demoGroups.flatMap { it.demos }

@Composable
fun Showcase() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        var openDemoId by rememberSaveable { mutableStateOf<String?>(null) }
        val openDemo = allDemos.firstOrNull { it.id == openDemoId }
        // Hoisted so the list keeps its scroll position when coming back from a demo.
        val listState = rememberLazyListState()

        NavigationBackHandler(
            state = rememberNavigationEventState(NavigationEventInfo.None),
            isBackEnabled = openDemo != null,
            onBackCompleted = { openDemoId = null },
        )

        AnimatedContent(
            targetState = openDemo,
            modifier = Modifier.fillMaxSize().background(ShowcaseBackground),
            transitionSpec = {
                val forward = targetState != null
                val slide = tween<androidx.compose.ui.unit.IntOffset>(320)
                (slideInHorizontally(slide) { if (forward) it / 3 else -it / 3 } + fadeIn(tween(220)))
                    .togetherWith(slideOutHorizontally(slide) { if (forward) -it / 3 else it / 3 } + fadeOut(tween(180)))
            },
            label = "showcase",
        ) { demo ->
            if (demo == null) {
                HomeScreen(listState, onOpen = { openDemoId = it.id })
            } else {
                DemoScreen(demo, onBack = { openDemoId = null })
            }
        }
    }
}

@Composable
private fun HomeScreen(listState: LazyListState, onOpen: (Demo) -> Unit) {
    val numbers = allDemos.withIndex().associate { (index, demo) -> demo.id to index + 1 }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            Column(
                Modifier
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .padding(top = 20.dp, bottom = 8.dp),
            ) {
                Text("Animation Showcase", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Text("Tap a demo to play it", color = Muted, fontSize = 14.sp)
            }
        }
        demoGroups.forEach { group ->
            item(key = group.title) {
                Text(
                    group.title.uppercase(),
                    color = Accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth().padding(top = 18.dp, bottom = 2.dp),
                )
            }
            items(group.demos, key = { it.id }) { demo ->
                DemoButton(numbers.getValue(demo.id), demo, onClick = { onOpen(demo) })
            }
        }
        item { Spacer(Modifier.windowInsetsPadding(WindowInsets.navigationBars).height(24.dp)) }
    }
}

@Composable
private fun DemoButton(number: Int, demo: Demo, onClick: () -> Unit) {
    Row(
        Modifier
            .widthIn(max = 640.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CardBackground)
            .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(34.dp).clip(CircleShape).background(Accent.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Text("$number", color = Accent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(demo.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(demo.subtitle, color = Muted, fontSize = 13.sp, lineHeight = 17.sp)
        }
        PlayGlyph()
    }
}

@Composable
private fun PlayGlyph() {
    Canvas(Modifier.size(12.dp)) {
        val stroke = 2.dp.toPx()
        drawLine(Muted, Offset(size.width * 0.3f, 0f), Offset(size.width * 0.8f, size.height / 2), stroke, StrokeCap.Round)
        drawLine(Muted, Offset(size.width * 0.8f, size.height / 2), Offset(size.width * 0.3f, size.height), stroke, StrokeCap.Round)
    }
}

@Composable
private fun DemoScreen(demo: Demo, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(ShowcaseBackground)
                .windowInsetsPadding(WindowInsets.statusBars)
                .height(52.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.size(16.dp)) {
                    val stroke = 2.2.dp.toPx()
                    drawLine(Color.White, Offset(size.width * 0.65f, 0f), Offset(size.width * 0.2f, size.height / 2), stroke, StrokeCap.Round)
                    drawLine(Color.White, Offset(size.width * 0.2f, size.height / 2), Offset(size.width * 0.65f, size.height), stroke, StrokeCap.Round)
                }
            }
            Text(demo.title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        }
        Box(Modifier.weight(1f).fillMaxWidth().clipToBounds()) {
            demo.content()
        }
    }
}
