package com.azersun.uitemplates.demos.switch1

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import com.azersun.uitemplates.demos.checkbox1.CheckboxStage
import com.azersun.uitemplates.demos.checkbox1.mix
import kotlinx.coroutines.launch

private val MorphFill = Brush.horizontalGradient(listOf(SwitchColors.Violet, SwitchColors.Cyan))

/*
 * Both glyphs as two strokes, in fractions of the knob's diameter from its centre. Each ✕ stroke slides
 * onto one arm of the ✓, and both inner ends meet at the ✓'s vertex.
 */
private const val CrossArm = 0.17f
private const val TickShortX = -0.2f
private const val TickShortY = 0.02f
private const val TickVertexX = -0.05f
private const val TickVertexY = 0.17f
private const val TickLongX = 0.22f
private const val TickLongY = -0.14f

@Composable
fun IconMorphDemo() {
    var on by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    fun toggle() {
        on = !on
        scope.launch { progress.animateTo(if (on) 1f else 0f, spring(dampingRatio = 0.68f, stiffness = 210f)) }
    }

    CheckboxStage(
        3,
        "Icon Morph",
        "Knob spins across as its ✕ morphs into a ✓. Track fills violet→cyan.",
        autoplay = { toggle() },
    ) {
        Spacer(
            Modifier
                .size(TrackWidth, TrackHeight)
                .clickable(interactionSource = null, indication = null, role = Role.Switch) { toggle() }
                .drawWithCache {
                    val inset = KnobInset.toPx()
                    val radius = size.height / 2 - inset
                    val travel = size.width - inset * 2 - radius * 2
                    val unit = radius * 2
                    val stroke = unit * 0.09f
                    onDrawBehind {
                        val p = progress.value
                        // Position and shape stop at the ends; the spin keeps the spring's overshoot.
                        val m = p.coerceIn(0f, 1f)
                        val center = Offset(inset + radius + m * travel, size.height / 2)
                        drawTrack(m, MorphFill)
                        drawKnob(center, radius)
                        val color = lerp(SwitchColors.Icon, SwitchColors.Violet, m)
                        rotate(p * 360f, center) {
                            drawLine(
                                color,
                                center + Offset(unit * mix(-CrossArm, TickShortX, m), unit * mix(-CrossArm, TickShortY, m)),
                                center + Offset(unit * mix(CrossArm, TickVertexX, m), unit * mix(CrossArm, TickVertexY, m)),
                                stroke,
                                StrokeCap.Round,
                            )
                            drawLine(
                                color,
                                center + Offset(unit * mix(CrossArm, TickLongX, m), unit * mix(-CrossArm, TickLongY, m)),
                                center + Offset(unit * mix(-CrossArm, TickVertexX, m), unit * mix(CrossArm, TickVertexY, m)),
                                stroke,
                                StrokeCap.Round,
                            )
                        }
                    }
                },
        )
    }
}
