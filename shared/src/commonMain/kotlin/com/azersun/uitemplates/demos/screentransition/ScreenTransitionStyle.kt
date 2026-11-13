package com.azersun.uitemplates.demos.screentransition

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.DrawResult
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azersun.uitemplates.demos.menus.MenuColors
import com.azersun.uitemplates.demos.menus.MenuStage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

internal class Place(
    val name: String,
    val trip: String,
    val rating: String,
    val art: CacheDrawScope.() -> DrawResult,
)

internal val Places = listOf(
    Place("Peaks", "Day trip", "4.9") { peaksArt() },
    Place("Ocean", "Weekend", "4.8") { oceanArt() },
    Place("Garden", "Half day", "4.7") { gardenArt() },
)

internal val PhoneShape = RoundedCornerShape(24.dp)
private const val PhoneAspect = 0.8f
private val PhoneBackdrop = Color(0xFF07061A)
private val BookGradient = Brush.horizontalGradient(listOf(MenuColors.Blue, Color(0xFF38BDF8)))
private val LabelScrim = Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f)))

private const val AutoplayDelayMillis = 700L
private const val AutoplayDetailMillis = 1500L
private const val AutoplayPauseMillis = 700L
private const val AutoplayRounds = 2

/**
 * Explore ↔ detail navigation as a single [progress]: 0 shows Explore, 1 the detail screen.
 * Animations run in the scene's own scope, so a cancelled autoplay never strands a transition
 * half-way.
 */
@Stable
internal class Navigator(private val scope: CoroutineScope, private val spec: AnimationSpec<Float>) {
    val progress = Animatable(0f)

    var place by mutableStateOf(Places.first())
        private set

    fun open(target: Place): Job? {
        if (progress.isRunning || progress.value != 0f) return null
        place = target
        return animateTo(1f)
    }

    fun back(): Job? {
        if (progress.isRunning || progress.value != 1f) return null
        return animateTo(0f)
    }

    fun animateTo(target: Float, spec: AnimationSpec<Float> = this.spec, velocity: Float = progress.velocity): Job =
        scope.launch { progress.animateTo(target, spec, velocity) }

    fun snapTo(value: Float): Job = scope.launch { progress.snapTo(value) }

    fun launch(block: suspend CoroutineScope.() -> Unit): Job = scope.launch(block = block)
}

@Composable
internal fun rememberNavigator(spec: AnimationSpec<Float>): Navigator {
    val scope = rememberCoroutineScope()
    return remember { Navigator(scope, spec) }
}

private enum class Stage { Explore, Detail, Between }

/**
 * The numbered stage with the phone-like card. At rest it shows one screen; while [Navigator.progress]
 * is between 0 and 1 it hands both screens to [transition] with the progress, read lazily so
 * the transition can animate in the draw phase. The reel's autoplay (open Peaks, back 1.5 s
 * later, twice) stops at the first real touch.
 */
@Composable
internal fun TransitionScene(
    number: Int,
    title: String,
    caption: String,
    nav: Navigator,
    modifier: Modifier = Modifier,
    autoplayBack: suspend () -> Unit = { nav.back()?.join() },
    overlay: @Composable BoxScope.() -> Unit = {},
    transition: @Composable BoxScope.(
        explore: @Composable () -> Unit,
        detail: @Composable () -> Unit,
        progress: () -> Float,
    ) -> Unit,
) {
    var stopped by remember { mutableStateOf(false) }
    LaunchedEffect(stopped) {
        if (stopped) return@LaunchedEffect
        delay(AutoplayDelayMillis)
        repeat(AutoplayRounds) { round ->
            if (round > 0) delay(AutoplayPauseMillis)
            nav.open(Places.first())?.join()
            delay(AutoplayDetailMillis)
            autoplayBack()
        }
    }

    val stage by remember(nav) {
        derivedStateOf {
            val value = nav.progress.value
            when {
                nav.progress.isRunning -> Stage.Between
                value <= 0f -> Stage.Explore
                value >= 1f -> Stage.Detail
                else -> Stage.Between
            }
        }
    }
    val progress = remember(nav) { { nav.progress.value.coerceIn(0f, 1f) } }
    val explore: @Composable () -> Unit = { ExploreScreen(onOpen = { nav.open(it) }) }
    val detail: @Composable () -> Unit = { DetailScreen(nav.place, onBack = { nav.back() }) }

    MenuStage(number, title, caption) {
        Box(
            Modifier
                .padding(14.dp)
                .fillMaxWidth()
                .aspectRatio(PhoneAspect)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        stopped = true
                    }
                }
                .clip(PhoneShape)
                .background(PhoneBackdrop)
                .border(1.dp, MenuColors.CardBorder, PhoneShape)
                .then(modifier),
        ) {
            when (stage) {
                Stage.Explore -> explore()
                Stage.Detail -> detail()
                Stage.Between -> transition(explore, detail, progress)
            }
            overlay()
        }
    }
}

@Composable
private fun ExploreScreen(onOpen: (Place) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MenuColors.Screen)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Explore", color = MenuColors.Text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Three escapes for the weekend", color = MenuColors.Muted, fontSize = 12.sp, maxLines = 1)
            }
            Box(
                Modifier
                    .size(18.dp)
                    .background(MenuColors.Blue.copy(alpha = 0.22f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(8.dp).background(MenuColors.Blue, CircleShape))
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Places.forEach { place ->
                PlaceCard(
                    place,
                    Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                ) { onOpen(place) }
            }
        }
    }
}

@Composable
private fun PlaceCard(place: Place, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .drawWithCache(place.art)
            .clickable(onClickLabel = "Open ${place.name}", role = Role.Button, onClick = onClick),
    ) {
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(LabelScrim)
                .padding(start = 10.dp, end = 10.dp, top = 18.dp, bottom = 10.dp),
        ) {
            Text(place.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text("★ ${place.rating}", color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp)
        }
    }
}

@Composable
private fun DetailScreen(place: Place, onBack: () -> Unit) {
    Row(
        Modifier
            .fillMaxSize()
            .background(MenuColors.Screen),
    ) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .drawWithCache(place.art),
        ) {
            Box(
                Modifier
                    .padding(12.dp)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.35f))
                    .clickable(onClickLabel = "Back", role = Role.Button, onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.size(14.dp)) {
                    val w = size.width
                    val stroke = 2.dp.toPx()
                    drawLine(Color.White, Offset(w * 0.64f, w * 0.14f), Offset(w * 0.3f, w * 0.5f), stroke, StrokeCap.Round)
                    drawLine(Color.White, Offset(w * 0.3f, w * 0.5f), Offset(w * 0.64f, w * 0.86f), stroke, StrokeCap.Round)
                }
            }
        }
        Column(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(place.name, color = MenuColors.Text, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Row(Modifier.padding(top = 4.dp)) {
                Text("${place.trip} · ", color = MenuColors.Muted, fontSize = 12.sp)
                Text("★", color = MenuColors.Yellow, fontSize = 12.sp)
                Text(" ${place.rating}", color = MenuColors.Muted, fontSize = 12.sp)
            }
            Spacer(Modifier.height(18.dp))
            listOf(1f, 0.86f, 0.58f).forEach { fraction ->
                Box(
                    Modifier
                        .padding(bottom = 8.dp)
                        .fillMaxWidth(fraction)
                        .height(7.dp)
                        .clip(CircleShape)
                        .background(MenuColors.Muted.copy(alpha = 0.35f)),
                )
            }
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(CircleShape)
                    .background(BookGradient)
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Book now", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

internal fun mix(start: Float, stop: Float, fraction: Float): Float = start + (stop - start) * fraction

internal fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
    val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

private fun CacheDrawScope.peaksArt(): DrawResult {
    val w = size.width
    val h = size.height
    val sky = Brush.verticalGradient(listOf(Color(0xFF2B1867), Color(0xFF6D4FD8), Color(0xFFC3A6FF)))
    val sun = Offset(w * 0.64f, h * 0.3f)
    val sunRadius = min(w, h) * 0.15f
    val glow = Brush.radialGradient(listOf(Color(0x55FFF1D6), Color.Transparent), sun, sunRadius * 2.8f)
    val far = Path().apply {
        moveTo(0f, h * 0.66f)
        lineTo(w * 0.2f, h * 0.5f)
        lineTo(w * 0.36f, h * 0.6f)
        lineTo(w * 0.6f, h * 0.42f)
        lineTo(w * 0.82f, h * 0.58f)
        lineTo(w, h * 0.5f)
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }
    val leftPeak = Offset(w * 0.28f, h * 0.6f)
    val valley = Offset(w * 0.48f, h * 0.74f)
    val rightPeak = Offset(w * 0.72f, h * 0.56f)
    val near = Path().apply {
        moveTo(0f, h * 0.8f)
        lineTo(leftPeak.x, leftPeak.y)
        lineTo(valley.x, valley.y)
        lineTo(rightPeak.x, rightPeak.y)
        lineTo(w, h * 0.76f)
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }
    val snow = Path().apply {
        snowCap(leftPeak, Offset(0f, h * 0.8f), valley)
        snowCap(rightPeak, valley, Offset(w, h * 0.76f))
    }
    return onDrawBehind {
        drawRect(sky)
        drawCircle(glow, sunRadius * 2.8f, sun)
        drawCircle(Color(0xFFFFF1D6), sunRadius, sun)
        drawPath(far, Color(0xFF4A2F9E))
        drawPath(near, Color(0xFF231457))
        drawPath(snow, Color(0xFFE9DEFF), alpha = 0.85f)
    }
}

/** A triangle of snow from [peak] a fifth of the way down each slope. */
private fun Path.snowCap(peak: Offset, left: Offset, right: Offset) {
    val f = 0.2f
    moveTo(peak.x, peak.y)
    lineTo(mix(peak.x, right.x, f), mix(peak.y, right.y, f))
    lineTo(mix(peak.x, left.x, f), mix(peak.y, left.y, f))
    close()
}

private fun CacheDrawScope.oceanArt(): DrawResult {
    val w = size.width
    val h = size.height
    val m = min(w, h)
    val sky = Brush.verticalGradient(listOf(Color(0xFF0A1A4A), Color(0xFF1E46B8), Color(0xFF4F8DF5)))
    val moon = Offset(w * 0.3f, h * 0.24f)
    val moonRadius = m * 0.12f
    val glow = Brush.radialGradient(listOf(Color(0x44EAF1FF), Color.Transparent), moon, moonRadius * 3f)
    val stars = listOf(0.62f to 0.12f, 0.82f to 0.22f, 0.54f to 0.32f, 0.9f to 0.08f, 0.14f to 0.1f)
        .map { (x, y) -> Offset(w * x, h * y) }
    val bandColors = listOf(Color(0xFF2F6FE0), Color(0xFF1F4FC2), Color(0xFF173A93))
    val bands = List(bandColors.size) { Path() }
    val crests = List(bandColors.size) { Path() }
    val steps = 32
    bandColors.indices.forEach { i ->
        val base = h * (0.6f + i * 0.11f)
        val amplitude = m * 0.035f
        val waveLength = w * 0.55f
        val phase = i * 1.3f
        fun y(x: Float) = base + amplitude * sin(2f * PI.toFloat() * x / waveLength + phase)
        bands[i].moveTo(0f, y(0f))
        crests[i].moveTo(0f, y(0f))
        for (s in 1..steps) {
            val x = w * s / steps
            bands[i].lineTo(x, y(x))
            crests[i].lineTo(x, y(x))
        }
        bands[i].lineTo(w, h)
        bands[i].lineTo(0f, h)
        bands[i].close()
    }
    val crestStroke = Stroke(m * 0.012f, cap = StrokeCap.Round)
    return onDrawBehind {
        drawRect(sky)
        stars.forEach { drawCircle(Color.White, m * 0.012f, it, alpha = 0.7f) }
        drawCircle(glow, moonRadius * 3f, moon)
        drawCircle(Color(0xFFEAF1FF), moonRadius, moon)
        bandColors.forEachIndexed { i, color ->
            drawPath(bands[i], color)
            drawPath(crests[i], Color(0x66CFE3FF), style = crestStroke)
        }
    }
}

private fun CacheDrawScope.gardenArt(): DrawResult {
    val w = size.width
    val h = size.height
    val m = min(w, h)
    val sky = Brush.verticalGradient(listOf(Color(0xFF1C4D3A), Color(0xFF2E8B57), Color(0xFF55C27F)))
    val backHill = Path().apply {
        moveTo(0f, h * 0.58f)
        quadraticTo(w * 0.35f, h * 0.42f, w * 0.7f, h * 0.56f)
        quadraticTo(w * 0.88f, h * 0.62f, w, h * 0.55f)
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }
    val frontHill = Path().apply {
        moveTo(0f, h * 0.72f)
        quadraticTo(w * 0.5f, h * 0.6f, w, h * 0.74f)
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }
    val petal = m * 0.045f
    val petalOffsets = List(5) { k ->
        val a = k * 2f * PI.toFloat() / 5 - PI.toFloat() / 2
        Offset(cos(a), sin(a)) * petal
    }
    val flowers = listOf(
        Triple(0.16f, 0.66f, true),
        Triple(0.38f, 0.74f, false),
        Triple(0.62f, 0.64f, true),
        Triple(0.84f, 0.72f, false),
        Triple(0.26f, 0.86f, false),
        Triple(0.52f, 0.88f, true),
        Triple(0.78f, 0.9f, true),
    ).map { (x, y, pink) -> Offset(w * x, h * y) to pink }
    val stemLength = m * 0.16f
    val stemWidth = m * 0.014f
    return onDrawBehind {
        drawRect(sky)
        drawPath(backHill, Color(0xFF237A4B))
        drawPath(frontHill, Color(0xFF16603A))
        flowers.forEach { (center, pink) ->
            drawLine(Color(0xFF0E4D2C), center, center + Offset(0f, stemLength), stemWidth, StrokeCap.Round)
            val petalColor = if (pink) Color(0xFFFF7EB6) else Color(0xFFFFD166)
            petalOffsets.forEach { drawCircle(petalColor, petal, center + it) }
            drawCircle(if (pink) Color(0xFFFFD166) else Color(0xFFFB923C), petal * 0.7f, center)
        }
    }
}
