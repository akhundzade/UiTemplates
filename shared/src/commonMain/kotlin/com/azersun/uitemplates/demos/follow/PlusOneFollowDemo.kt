package com.azersun.uitemplates.demos.follow

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azersun.uitemplates.demos.menus.MenuColors
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

private class Chip(val id: Int, val following: Boolean)

@Composable
fun PlusOneFollowDemo() {
    val state = remember { FollowState() }
    val scope = rememberCoroutineScope()
    val chips = remember { mutableStateListOf<Chip>() }
    // The count only changes when a chip lands on it.
    var countedFollowing by remember { mutableStateOf(false) }
    val bump = remember { Animatable(1f) }
    LaunchedEffect(state.toggles) {
        if (state.toggles > 0) chips += Chip(state.toggles, state.following)
    }

    FollowScene(
        state,
        4,
        "Plus One",
        "A +1 chip flies from the button up to the follower count, which then ticks up.",
        followers = BaseFollowers + if (countedFollowing) 1 else 0,
        countModifier = Modifier.graphicsLayer {
            scaleX = bump.value
            scaleY = bump.value
            transformOrigin = TransformOrigin(1f, 0.5f)
        },
        overlay = {
            var origin by remember { mutableStateOf(Offset.Zero) }
            Box(Modifier.matchParentSize().onGloballyPositioned { origin = it.positionInRoot() }) {
                chips.forEach { chip ->
                    key(chip.id) {
                        FlyingChip(
                            chip,
                            from = { state.buttonBounds.center - origin },
                            to = { state.countCenter - origin },
                            onArrive = {
                                countedFollowing = chip.following
                                scope.launch {
                                    bump.snapTo(1.16f)
                                    bump.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 600f))
                                }
                            },
                            onDone = { chips.remove(chip) },
                        )
                    }
                }
            }
        },
    ) {
        FadingFollowButton(state.following)
    }
}

@Composable
private fun FlyingChip(chip: Chip, from: () -> Offset, to: () -> Offset, onArrive: () -> Unit, onDone: () -> Unit) {
    val flight = remember { Animatable(0f) }
    val fade = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        flight.animateTo(1f, tween(620, easing = FastOutSlowInEasing))
        onArrive()
        fade.animateTo(0f, tween(160))
        onDone()
    }
    Text(
        if (chip.following) "+1" else "−1",
        color = MenuColors.Text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .graphicsLayer {
                val t = flight.value
                val start = from()
                val end = to()
                // A quadratic arc that bows up above the straight line between the two points.
                val control = Offset((start.x + end.x) / 2, minOf(start.y, end.y) - 46.dp.toPx())
                val u = 1f - t
                val point = start * (u * u) + control * (2 * u * t) + end * (t * t)
                translationX = point.x - size.width / 2
                translationY = point.y - size.height / 2
                val scale = 0.7f + 0.45f * sin(t * PI.toFloat()) + 0.1f * t
                scaleX = scale
                scaleY = scale
                alpha = fade.value * (t * 6f).coerceIn(0f, 1f)
            }
            .then(
                if (chip.following) {
                    Modifier.background(FollowGradient, Pill)
                } else {
                    Modifier.background(FollowingSurface, Pill).border(1.dp, FollowingBorder, Pill)
                },
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}
