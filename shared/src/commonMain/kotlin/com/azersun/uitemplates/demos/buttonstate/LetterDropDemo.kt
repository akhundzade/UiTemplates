package com.azersun.uitemplates.demos.buttonstate

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

private const val StaggerMillis = 45f
private const val DropMillis = 380f
private const val LandDelayMillis = 220f
private const val LandMillis = 560f
private const val TintMillis = 420f
private const val TotalMillis = 1100

private val DropDistance = 34.dp
private val FallDistance = 30.dp

/** How far each letter tips as it drops, so the fall looks scattered rather than uniform. */
private val DropTilt = floatArrayOf(-24f, 18f, -32f, 22f, -14f, 28f, -20f)

@Composable
fun LetterDropDemo() {
    var leaving by remember { mutableStateOf<PlayState?>(null) }
    var current by remember { mutableStateOf(StartState) }
    val clock = remember { Animatable(TotalMillis.toFloat()) }

    ButtonStateScene(
        number = 6,
        title = "Letter Drop",
        caption = "The old label's letters drop out one by one; the new letters fall in from above and bounce.",
        onToggle = { from, to, _ ->
            leaving = from
            current = to
            clock.snapTo(0f)
            clock.animateTo(TotalMillis.toFloat(), tween(TotalMillis, easing = LinearEasing))
            leaving = null
        },
    ) {
        Box(
            Modifier
                .matchParentSize()
                .clip(Pill)
                .drawBehind {
                    val t = clock.value
                    val old = leaving
                    if (old == null) {
                        drawRect(current.brush)
                    } else {
                        drawRect(old.brush)
                        drawRect(current.brush, alpha = (t / TintMillis).coerceIn(0f, 1f))
                    }
                },
        )
        leaving?.let { old ->
            LetterRow(old) { i ->
                val p = ((clock.value - i * StaggerMillis) / DropMillis).coerceIn(0f, 1f)
                val fall = p * p
                translationY = fall * DropDistance.toPx()
                rotationZ = fall * DropTilt[i % DropTilt.size]
                alpha = 1f - p
            }
        }
        LetterRow(current) { i ->
            val p = ((clock.value - LandDelayMillis - i * StaggerMillis) / LandMillis).coerceIn(0f, 1f)
            translationY = -FallDistance.toPx() * (1f - bounceOut(p))
            alpha = (p * 5f).coerceAtMost(1f)
        }
    }
}

/** The glyph and each letter of [state]'s label as separately movable pieces, glyph first. */
@Composable
private fun LetterRow(state: PlayState, piece: GraphicsLayerScope.(index: Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        StateGlyph(state, Modifier.graphicsLayer { piece(0) })
        Spacer(Modifier.width(GlyphGap))
        state.label.forEachIndexed { i, letter ->
            LabelText(letter.toString(), Modifier.graphicsLayer { piece(i + 1) })
        }
    }
}

/** The classic ease-out bounce: lands, then two ever smaller hops. */
private fun bounceOut(t: Float): Float {
    val n = 7.5625f
    val d = 2.75f
    return when {
        t < 1f / d -> n * t * t
        t < 2f / d -> (t - 1.5f / d).let { n * it * it + 0.75f }
        t < 2.5f / d -> (t - 2.25f / d).let { n * it * it + 0.9375f }
        else -> (t - 2.625f / d).let { n * it * it + 0.984375f }
    }
}
