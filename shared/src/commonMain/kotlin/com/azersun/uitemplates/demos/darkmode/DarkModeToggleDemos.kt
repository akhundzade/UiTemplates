package com.azersun.uitemplates.demos.darkmode

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.azersun.uitemplates.demos.menus.MenuStage
import com.azersun.uitemplates.showcase.Demo
import com.azersun.uitemplates.showcase.DemoGroup
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.max

private enum class Reveal(val number: Int, val title: String, val caption: String) {
    Portal(1, "Portal Open", "The new theme opens as a circle from the sun/moon button."),
    Curtain(2, "Curtain Drop", "The new theme drops like a curtain as the moon falls in."),
    Lines(3, "Line by Line", "Rows switch theme one by one; the sun's rays tuck in."),
    Flip(4, "Theme Flip", "The whole card flips in 3D; its back is the other theme."),
    Eclipse(5, "Eclipse Slide", "A giant disc glides over the card like an eclipse."),
    Clock(6, "Clock Sweep", "The new theme sweeps around like a clock hand; the icon spins."),
}

val darkModeToggleGroup: DemoGroup = DemoGroup(
    "Dark mode toggle",
    listOf(
        Demo("darkmode-portal", "Portal open", "A circle opens from the sun/moon button", { ThemeToggleDemo(Reveal.Portal) }),
        Demo("darkmode-curtain", "Curtain drop", "The new theme drops like a curtain", { ThemeToggleDemo(Reveal.Curtain) }),
        Demo("darkmode-lines", "Line by line", "Rows switch one by one, sun rays tuck in", { ThemeToggleDemo(Reveal.Lines) }),
        Demo("darkmode-flip", "Theme flip", "The card flips in 3D to the other theme", { ThemeToggleDemo(Reveal.Flip) }),
        Demo("darkmode-eclipse", "Eclipse slide", "A giant disc glides over the card", { ThemeToggleDemo(Reveal.Eclipse) }),
        Demo("darkmode-clock", "Clock sweep", "The theme sweeps round like a clock hand", { ThemeToggleDemo(Reveal.Clock) }),
    ),
)

private fun palette(dark: Boolean) = if (dark) DarkCard else LightCard

private const val LINE_STAGGER = 0.22f

@Composable
private fun ThemeToggleDemo(reveal: Reveal) {
    var dark by remember { mutableStateOf(true) }
    var toDark by remember { mutableStateOf<Boolean?>(null) }
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    var cardTop by remember { mutableFloatStateOf(0f) }
    val rowTops = remember { FloatArray(2) }

    fun toggle() {
        if (toDark != null) return
        toDark = !dark
        scope.launch {
            progress.snapTo(0f)
            progress.animateTo(1f, reveal.spec())
            dark = !dark
            toDark = null
            progress.snapTo(0f)
        }
    }

    LaunchedEffect(Unit) {
        delay(700)
        toggle()
    }

    MenuStage(reveal.number, reveal.title, reveal.caption) {
        val target = toDark
        val p = progress.value
        val toggleCenter = with(density) {
            Offset(
                (320.dp - CardPadding - ToggleSize / 2).toPx(),
                (CardPadding + 22.dp).toPx(),
            )
        }
        val cardModifier = Modifier.onGloballyPositioned { cardTop = it.positionInRoot().y }
        val onRowTops: (Int, Float) -> Unit = { i, y -> rowTops[i] = y }

        if (reveal == Reveal.Flip) {
            FlipCard(dark, target, p, ::toggle)
            return@MenuStage
        }

        Box {
            val outgoing = palette(dark)
            ProfileCard(
                palette = outgoing,
                onToggle = ::toggle,
                modifier = cardModifier,
                rays = if (reveal == Reveal.Lines && target != null) 1f - p else 1f,
                iconRotation = if (reveal == Reveal.Clock && target != null) 360f * p else 0f,
                onRowTops = onRowTops,
            )
            if (target != null) {
                val drop = if (reveal == Reveal.Curtain) with(density) { -(1f - p).coerceAtLeast(0f) * 34.dp.toPx() } else 0f
                ProfileCard(
                    palette = palette(target),
                    onToggle = {},
                    rays = if (reveal == Reveal.Lines) lineProgress(p, 0) else 1f,
                    iconRotation = if (reveal == Reveal.Clock) 360f * p else 0f,
                    iconDropY = drop,
                    modifier = Modifier.drawWithContent {
                        val path = revealPath(reveal, size, p, toggleCenter, rowTops.map { it - cardTop })
                        clipPath(path) { this@drawWithContent.drawContent() }
                        if (reveal == Reveal.Eclipse && p < 1f) {
                            val disc = eclipseDisc(size, p)
                            drawCircle(
                                Color.White.copy(alpha = 0.18f * (1f - p)),
                                disc.second,
                                disc.first,
                                style = Stroke(2.dp.toPx()),
                            )
                        }
                    },
                )
            }
        }
    }
}

private fun Reveal.spec(): AnimationSpec<Float> = when (this) {
    Reveal.Portal -> tween(620, easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f))
    Reveal.Curtain -> spring(dampingRatio = 0.62f, stiffness = 120f)
    Reveal.Lines -> tween(900, easing = LinearEasing)
    Reveal.Flip -> spring(dampingRatio = 0.72f, stiffness = 110f)
    Reveal.Eclipse -> tween(900, easing = FastOutSlowInEasing)
    Reveal.Clock -> tween(850, easing = CubicBezierEasing(0.5f, 0f, 0.3f, 1f))
}

/** Row [index]'s own 0..1 progress inside the staggered line-by-line wipe (0 = header, 1–2 = rows). */
private fun lineProgress(p: Float, index: Int): Float {
    val start = index * LINE_STAGGER
    val span = 1f - 2 * LINE_STAGGER
    return FastOutSlowInEasing.transform(((p - start) / span).coerceIn(0f, 1f))
}

/** Center and radius of the eclipse disc: it glides in from the right until it covers the card. */
private fun eclipseDisc(size: androidx.compose.ui.geometry.Size, p: Float): Pair<Offset, Float> {
    val radius = hypot(size.width, size.height) * 0.62f
    val startX = size.width + radius
    val endX = size.width * 0.5f
    val center = Offset(startX + (endX - startX) * p, size.height * 0.62f)
    return center to radius * (1f + 0.25f * p)
}

private fun revealPath(
    reveal: Reveal,
    size: androidx.compose.ui.geometry.Size,
    p: Float,
    toggle: Offset,
    rowTops: List<Float>,
): Path = Path().apply {
    val w = size.width
    val h = size.height
    when (reveal) {
        Reveal.Portal -> {
            val far = max(max(hypot(toggle.x, toggle.y), hypot(w - toggle.x, toggle.y)), max(hypot(toggle.x, h - toggle.y), hypot(w - toggle.x, h - toggle.y)))
            addOval(Rect(toggle, far * p))
        }
        Reveal.Curtain -> {
            // Bottom hem sags in the middle while falling and flattens as it settles.
            val y = h * p
            val sag = h * 0.22f * (1f - p.coerceIn(0f, 1f)) + h * 0.04f
            moveTo(0f, 0f)
            lineTo(w, 0f)
            lineTo(w, y - sag * 0.4f)
            quadraticTo(w / 2, y + sag, 0f, y - sag * 0.4f)
            close()
        }
        Reveal.Lines -> {
            val bounds = listOf(0f, rowTops.getOrElse(0) { h * 0.45f }, rowTops.getOrElse(1) { h * 0.72f }, h)
            for (i in 0..2) {
                val local = lineProgress(p, i)
                if (local <= 0f) continue
                addRect(Rect(w * (1f - local), bounds[i], w, bounds[i + 1]))
            }
        }
        Reveal.Eclipse -> {
            val (center, radius) = eclipseDisc(size, p)
            addOval(Rect(center, radius))
        }
        Reveal.Clock -> {
            val c = Offset(w / 2, h / 2)
            val r = hypot(w, h)
            moveTo(c.x, c.y)
            arcTo(Rect(c, r), -90f, 360f * p, false)
            close()
        }
        Reveal.Flip -> addRect(Rect(0f, 0f, w, h))
    }
}

@Composable
private fun FlipCard(dark: Boolean, target: Boolean?, p: Float, onToggle: () -> Unit) {
    val angle = if (target == null) 0f else 180f * p
    val showBack = angle > 90f
    val shown = if (target != null && showBack) target else dark
    Box(
        Modifier.graphicsLayer {
            rotationY = angle
            cameraDistance = 14f * density
            val lift = 1f - 0.08f * kotlin.math.sin((angle / 180f).coerceIn(0f, 1f) * kotlin.math.PI.toFloat())
            scaleX = lift
            scaleY = lift
        },
    ) {
        ProfileCard(
            palette = palette(shown),
            onToggle = onToggle,
            modifier = Modifier.graphicsLayer { rotationY = if (showBack) 180f else 0f },
        )
    }
}
