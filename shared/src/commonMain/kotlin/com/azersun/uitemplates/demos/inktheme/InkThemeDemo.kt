package com.azersun.uitemplates.demos.inktheme

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

private class Palette(
    val background: Color,
    val surface: Color,
    val text: Color,
    val muted: Color,
    val line: Color,
    val buttonBackground: Color,
    val buttonText: Color,
    val dark: Boolean,
)

private val Accent = Color(0xFFE8572A)
private val InkColor = Color(0xFF0B0B0B)

private val DarkPalette = Palette(
    background = Color(0xFF0F0F0F),
    surface = Color(0xFF1B1B1B),
    text = Color(0xFFF2EFE9),
    muted = Color(0xFF8C8780),
    line = Color(0xFF2A2A2A),
    buttonBackground = Color(0xFFF2EFE9),
    buttonText = Color(0xFF111111),
    dark = true,
)

private val LightPalette = Palette(
    background = Color(0xFFF3F0EA),
    surface = Color(0xFFE7E2D9),
    text = Color(0xFF141414),
    muted = Color(0xFF6E6A64),
    line = Color(0xFFDAD4C9),
    buttonBackground = Color(0xFF141414),
    buttonText = Color(0xFFF3F0EA),
    dark = false,
)

/**
 * Linear so the front keeps spreading for the whole duration like ink in water; any ease-out
 * finishes the visible spread early, because most of the screen sits close to the toggle.
 */
private val InkEasing = LinearEasing

private data class InkSettings(
    val durationMillis: Int = 2000,
    val tendrils: Float = 0.35f,
    val noiseScale: Float = 3f,
    val softness: Float = 0.012f,
    val drift: Float = 1f,
    val slowMotion: Boolean = false,
    val tapAnywhere: Boolean = false,
)

private class InkTransition(val toDark: Boolean, val origin: Offset)

@Composable
fun InkThemeDemo() {
    val renderer = remember { createInkRenderer() }
    val scope = rememberCoroutineScope()
    val progress = remember { Animatable(0f) }
    var clock by remember { mutableFloatStateOf(0f) }
    var dark by remember { mutableStateOf(true) }
    var transition by remember { mutableStateOf<InkTransition?>(null) }
    var settings by remember { mutableStateOf(InkSettings()) }
    var panelOpen by remember { mutableStateOf(false) }
    var rootOrigin by remember { mutableStateOf(Offset.Zero) }
    var toggleCenter by remember { mutableStateOf(Offset.Zero) }
    var lastOrigin by remember { mutableStateOf<Offset?>(null) }

    fun start(origin: Offset) {
        if (transition != null) return
        val next = InkTransition(toDark = !dark, origin = origin)
        transition = next
        lastOrigin = origin
        scope.launch {
            val slow = if (settings.slowMotion) 4 else 1
            progress.snapTo(0f)
            val clockJob = launch {
                val start = withFrameNanos { it }
                while (true) {
                    withFrameNanos { clock = (it - start) / 1_000_000_000f / slow * settings.drift }
                }
            }
            progress.animateTo(1f, tween(settings.durationMillis * slow, easing = InkEasing))
            clockJob.cancel()
            dark = next.toDark
            transition = null
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .onGloballyPositioned { rootOrigin = it.positionInRoot() }
            .pointerInput(settings.tapAnywhere) {
                if (settings.tapAnywhere) detectTapGestures { start(it) }
            },
    ) {
        val active = transition
        val basePalette = if (active != null) palette(!active.toDark) else palette(dark)
        PortfolioPage(
            palette = basePalette,
            onToggle = { start(toggleCenter) },
            onTogglePositioned = { toggleCenter = it - rootOrigin },
        )
        if (active != null) {
            PortfolioPage(
                palette = palette(active.toDark),
                onToggle = {},
                onTogglePositioned = {},
                modifier = Modifier.inkReveal(
                    renderer = renderer,
                    origin = active.origin,
                    progress = { progress.value },
                    clock = { clock },
                    settings = settings,
                    inkBand = if (active.toDark) 0.24f else 0.1f,
                ),
            )
        }

        Column(
            Modifier
                .align(Alignment.BottomStart)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(16.dp),
        ) {
            AnimatedVisibility(
                visible = panelOpen,
                enter = fadeIn() + slideInVertically { it / 4 },
                exit = fadeOut() + slideOutVertically { it / 4 },
            ) {
                InkControls(
                    settings = settings,
                    onChange = { settings = it },
                    onClose = { panelOpen = false },
                    onReplay = {
                        val origin = lastOrigin ?: toggleCenter
                        if (transition == null) {
                            dark = !dark
                            start(origin)
                        }
                    },
                    onReset = { settings = InkSettings() },
                    shaderAvailable = renderer != null,
                )
            }
            if (!panelOpen) {
                Text(
                    "Ink controls",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xE6161616))
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(50))
                        .clickable { panelOpen = true }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
    }
}

private fun palette(dark: Boolean) = if (dark) DarkPalette else LightPalette

private fun Modifier.inkReveal(
    renderer: InkRenderer?,
    origin: Offset,
    progress: () -> Float,
    clock: () -> Float,
    settings: InkSettings,
    inkBand: Float,
): Modifier = if (renderer != null) {
    graphicsLayer {
        renderEffect = renderer.effect(
            InkUniforms(
                width = size.width,
                height = size.height,
                originX = origin.x,
                originY = origin.y,
                progress = progress(),
                time = clock(),
                tendrils = settings.tendrils,
                noiseScale = settings.noiseScale,
                softness = settings.softness,
                inkBand = inkBand,
                ink = InkColor,
            ),
        )
    }
} else {
    // Pre-Android 13 fallback: a wobbling blob clip instead of the shader's marbled edge.
    drawWithContent {
        clipPath(inkBlobPath(size, origin, progress(), settings.tendrils, clock())) {
            this@drawWithContent.drawContent()
        }
    }
}

private val BlobHarmonics = listOf(3 to 0.9f, 5 to 2.1f, 7 to 4.0f, 11 to 1.3f, 17 to 5.2f, 23 to 0.4f)

private fun inkBlobPath(size: Size, origin: Offset, progress: Float, tendrils: Float, time: Float): Path {
    val maxR = max(
        max(hypot(origin.x, origin.y), hypot(size.width - origin.x, origin.y)),
        max(hypot(origin.x, size.height - origin.y), hypot(size.width - origin.x, size.height - origin.y)),
    )
    val radius = progress * maxR * (1f + tendrils * 1.2f)
    val steps = 180
    return Path().apply {
        for (i in 0..steps) {
            val theta = 2f * PI.toFloat() * i / steps
            var wobble = 0f
            BlobHarmonics.forEach { (k, phase) -> wobble += sin(k * theta + phase + time * (k % 4)) / k }
            val r = radius * (1f + tendrils * wobble * 0.9f)
            val x = origin.x + cos(theta) * r
            val y = origin.y + sin(theta) * r
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
}

@Composable
private fun PortfolioPage(
    palette: Palette,
    onToggle: () -> Unit,
    onTogglePositioned: (Offset) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .background(palette.background)
            .padding(horizontal = 22.dp, vertical = 18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(Accent))
            Text(
                "Codemuse",
                color = palette.text,
                fontFamily = FontFamily.Serif,
                fontSize = 15.sp,
                modifier = Modifier.padding(start = 7.dp),
            )
            Spacer(Modifier.weight(1f))
            listOf("Work", "About", "Journal").forEachIndexed { i, label ->
                Text(
                    label,
                    color = if (i == 0) palette.text else palette.muted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(end = 12.dp),
                )
            }
            ThemeToggle(palette, onToggle, onTogglePositioned)
        }

        Text(
            "Independent product designer · Lisbon · Taking work for 2027",
            color = palette.muted,
            fontSize = 10.sp,
            letterSpacing = 0.4.sp,
            modifier = Modifier.padding(top = 44.dp),
        )
        Text(
            buildAnnotatedString {
                append("Designing quiet interfaces for ")
                withStyle(SpanStyle(color = Accent, fontStyle = FontStyle.Italic)) { append("loud") }
                append(" ideas.")
            },
            color = palette.text,
            fontFamily = FontFamily.Serif,
            fontSize = 40.sp,
            lineHeight = 44.sp,
            letterSpacing = (-0.8).sp,
            modifier = Modifier.padding(top = 14.dp),
        )
        Text(
            "I help early-stage teams turn tangled products into calm, legible tools — from first sketch " +
                "to shipped design system. Previously design lead at a Series B fintech and a small type foundry.",
            color = palette.muted,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            modifier = Modifier.padding(top = 18.dp),
        )
        Row(Modifier.padding(top = 22.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "See selected work",
                color = palette.buttonText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(palette.buttonBackground)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            )
            Text(
                "hello@codemuse.studio",
                color = palette.text,
                fontSize = 12.sp,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.padding(start = 16.dp),
            )
        }

        Row(Modifier.padding(top = 40.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Selected work", color = palette.text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.weight(1f))
            Text("2023 — 2026", color = palette.muted, fontSize = 10.sp)
        }
        Box(Modifier.padding(top = 10.dp).fillMaxWidth().height(1.dp).background(palette.line))
        Row(
            Modifier.padding(top = 14.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            WorkCard(palette, Modifier.weight(1f)) { SunsetArt(palette) }
            WorkCard(palette, Modifier.weight(1f)) {
                Text(
                    "Ff",
                    color = palette.text,
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic,
                    fontSize = 44.sp,
                    modifier = Modifier.align(Alignment.Center),
                )
                Text(
                    "TYPE SPECIMEN · VOL. 04",
                    color = palette.muted,
                    fontSize = 6.sp,
                    letterSpacing = 0.6.sp,
                    modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
                )
            }
            WorkCard(palette, Modifier.weight(1f)) { TargetArt(palette) }
        }
    }
}

@Composable
private fun WorkCard(palette: Palette, modifier: Modifier, content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit) {
    Box(
        modifier
            .aspectRatio(0.82f)
            .clip(RoundedCornerShape(10.dp))
            .background(palette.surface),
        content = content,
    )
}

@Composable
private fun SunsetArt(palette: Palette) {
    Canvas(Modifier.fillMaxSize()) {
        val hill = if (palette.dark) Color(0xFF2C2C2C) else Color(0xFFD1CBC0)
        drawCircle(Accent, radius = size.width * 0.22f, center = Offset(size.width * 0.5f, size.height * 0.5f))
        drawCircle(hill, radius = size.width * 0.9f, center = Offset(size.width * 0.5f, size.height * 1.45f))
    }
}

@Composable
private fun TargetArt(palette: Palette) {
    Canvas(Modifier.fillMaxSize()) {
        val c = Offset(size.width / 2, size.height / 2)
        drawCircle(Accent, radius = size.width * 0.27f, center = c, style = Stroke(size.width * 0.05f))
        drawCircle(palette.text, radius = size.width * 0.1f, center = c)
    }
}

@Composable
private fun ThemeToggle(palette: Palette, onToggle: () -> Unit, onPositioned: (Offset) -> Unit) {
    Box(
        Modifier
            .size(30.dp)
            .onGloballyPositioned { onPositioned(it.boundsInRoot().center) }
            .clip(CircleShape)
            .border(1.dp, palette.text.copy(alpha = 0.35f), CircleShape)
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(14.dp)) {
            val c = Offset(size.width / 2, size.height / 2)
            if (palette.dark) {
                drawCircle(palette.text, radius = size.width * 0.22f, center = c)
                repeat(8) { i ->
                    val a = i * PI.toFloat() / 4
                    drawLine(
                        palette.text,
                        c + Offset(cos(a), sin(a)) * size.width * 0.34f,
                        c + Offset(cos(a), sin(a)) * size.width * 0.48f,
                        strokeWidth = 1.4.dp.toPx(),
                    )
                }
            } else {
                drawCircle(palette.text, radius = size.width * 0.42f, center = c)
                drawCircle(palette.background, radius = size.width * 0.36f, center = c + Offset(size.width * 0.2f, -size.height * 0.16f))
            }
        }
    }
}

@Composable
private fun InkControls(
    settings: InkSettings,
    onChange: (InkSettings) -> Unit,
    onClose: () -> Unit,
    onReplay: () -> Unit,
    onReset: () -> Unit,
    shaderAvailable: Boolean,
) {
    Column(
        Modifier
            .width(270.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xF0161616))
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(16.dp))
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Ink controls", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Text(
                if (shaderAvailable) "Shader" else "Fallback",
                color = Accent,
                fontSize = 10.sp,
                modifier = Modifier.padding(end = 10.dp),
            )
            Text("✕", color = Color(0xFFAAAAAA), fontSize = 13.sp, modifier = Modifier.clickable(onClick = onClose))
        }
        ControlSlider("Duration", "${settings.durationMillis}ms", settings.durationMillis.toFloat(), 400f..3000f) {
            onChange(settings.copy(durationMillis = it.roundToInt()))
        }
        ControlSlider("Tendrils", settings.tendrils.format(2), settings.tendrils, 0f..1f) {
            onChange(settings.copy(tendrils = it))
        }
        ControlSlider("Noise scale", settings.noiseScale.format(1), settings.noiseScale, 1f..8f) {
            onChange(settings.copy(noiseScale = it))
        }
        ControlSlider("Edge softness", settings.softness.format(3), settings.softness, 0.001f..0.08f) {
            onChange(settings.copy(softness = it))
        }
        ControlSlider("Drift speed", settings.drift.format(2), settings.drift, 0f..3f) {
            onChange(settings.copy(drift = it))
        }
        ControlCheck("Slow motion (0.25×)", settings.slowMotion) { onChange(settings.copy(slowMotion = it)) }
        ControlCheck("Tap anywhere to ink", settings.tapAnywhere) { onChange(settings.copy(tapAnywhere = it)) }
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Replay last click",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Accent)
                    .clickable(onClick = onReplay)
                    .padding(vertical = 9.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Text(
                "Reset",
                color = Color.White,
                fontSize = 12.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(10.dp))
                    .clickable(onClick = onReset)
                    .padding(horizontal = 14.dp, vertical = 9.dp),
            )
        }
    }
}

@Composable
private fun ControlSlider(
    label: String,
    valueLabel: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValue: (Float) -> Unit,
) {
    Row(Modifier.padding(top = 10.dp).fillMaxWidth()) {
        Text(label, color = Color(0xFFCCCCCC), fontSize = 11.sp)
        Spacer(Modifier.weight(1f))
        Text(valueLabel, color = Accent, fontSize = 11.sp)
    }
    Slider(
        value = value,
        onValueChange = onValue,
        valueRange = range,
        modifier = Modifier.height(28.dp),
        colors = SliderDefaults.colors(
            thumbColor = Accent,
            activeTrackColor = Accent,
            inactiveTrackColor = Color(0xFF3A3A3A),
        ),
    )
}

@Composable
private fun ControlCheck(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onChecked(!checked) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onChecked,
            colors = CheckboxDefaults.colors(checkedColor = Accent, uncheckedColor = Color(0xFF777777)),
            modifier = Modifier.size(32.dp),
        )
        Text(label, color = Color(0xFFCCCCCC), fontSize = 11.sp)
    }
}

private fun Float.format(decimals: Int): String {
    var factor = 1
    repeat(decimals) { factor *= 10 }
    val scaled = (this * factor).roundToInt()
    val whole = scaled / factor
    val fraction = (scaled % factor).toString().padStart(decimals, '0')
    return if (decimals == 0) "$whole" else "$whole.$fraction"
}
