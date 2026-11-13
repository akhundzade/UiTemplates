package com.azersun.uitemplates.demos.follow

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val FallStaggerMillis = 28L
private const val DropStaggerMillis = 38L
private const val DropDelayMillis = 150L

@Composable
fun LetterDropFollowDemo() {
    val state = remember { FollowState() }
    val fill by animateFloatAsState(if (state.following) 1f else 0f, tween(320), label = "fill")

    FollowScene(state, 3, "Letter Drop", "The old label's letters fall away and the new ones drop in from above and bounce.") {
        Box(
            Modifier
                .fillMaxSize()
                .followBackground { fill }
                .clip(Pill),
            contentAlignment = Alignment.Center,
        ) {
            // A fresh pair per tap: the label being left falls, the new one drops in.
            key(state.toggles) {
                if (state.toggles > 0) LetterLabel(!state.following, Motion.FallOut)
                LetterLabel(state.following, if (state.toggles > 0) Motion.DropIn else Motion.Rest)
            }
        }
    }
}

private enum class Motion { Rest, DropIn, FallOut }

/**
 * The label split into its icon and letters. Each piece has a position: -1 above the button,
 * 0 at rest, 1 fallen out below.
 */
@Composable
private fun LetterLabel(following: Boolean, motion: Motion) {
    val text = labelOf(following)
    val pieces = text.length + 1
    val positions = remember { List(pieces) { Animatable(if (motion == Motion.DropIn) -1f else 0f) } }
    LaunchedEffect(Unit) {
        positions.forEachIndexed { i, position ->
            launch {
                when (motion) {
                    Motion.Rest -> Unit
                    Motion.DropIn -> {
                        delay(DropDelayMillis + i * DropStaggerMillis)
                        position.animateTo(0f, spring(dampingRatio = 0.42f, stiffness = 520f))
                    }
                    Motion.FallOut -> {
                        delay(i * FallStaggerMillis)
                        position.animateTo(1f, tween(340, easing = FastOutLinearInEasing))
                    }
                }
            }
        }
    }

    fun Modifier.piece(i: Int) = graphicsLayer {
        val p = positions[i].value
        translationY = p * 26.dp.toPx()
        val tilt = if (motion == Motion.FallOut) 38f else -10f
        rotationZ = p * if (i % 2 == 0) tilt else -tilt
        alpha = (1f - abs(p)).coerceIn(0f, 1f)
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        FollowIcon(following, Modifier.piece(0))
        Spacer(Modifier.width(8.dp))
        text.forEachIndexed { i, char ->
            Text(
                char.toString(),
                color = labelColor(following),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.piece(i + 1),
            )
        }
    }
}
