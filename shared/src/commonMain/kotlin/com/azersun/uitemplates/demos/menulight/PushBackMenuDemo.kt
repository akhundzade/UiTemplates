package com.azersun.uitemplates.demos.menulight

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azersun.uitemplates.demos.menus.StaggerIn
import kotlinx.coroutines.launch

private val NavRowHeight = 44.dp
private val MarkerHeight = 14.dp
private val PageShape = RoundedCornerShape(26.dp)

@Composable
fun PushBackMenuDemo() {
    val state = remember { MinimalMenuState() }
    val push = remember { Animatable(0f) }
    LaunchedEffect(state.open) {
        push.animateTo(if (state.open) 1f else 0f, spring(dampingRatio = 0.82f, stiffness = 240f))
    }

    MinimalMenuScene(state, 1, "Push Back", "The page tilts back in 3D and slides aside, revealing the nav behind it.") {
        Box(Modifier.fillMaxSize().background(LightColors.Backdrop)) {
            SideNav(
                state,
                Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 28.dp)
                    .graphicsLayer { alpha = push.value.coerceIn(0f, 1f) },
            )
            MinimalPage(
                state,
                Modifier.graphicsLayer {
                    val p = push.value
                    rotationY = -15f * p
                    scaleX = 1f - 0.15f * p
                    scaleY = 1f - 0.15f * p
                    translationX = size.width * 0.42f * p
                    cameraDistance = 12f * density
                    shadowElevation = 24.dp.toPx() * p.coerceIn(0f, 1f)
                    shape = PageShape
                    clip = true
                },
            ) {
                KebabButton(state.open, onClick = { state.open = !state.open })
            }
            if (state.open) {
                // The pushed-back page acts as one big close button, like a drawer's scrim.
                Box(
                    Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .fillMaxWidth(0.5f)
                        .clickable(remember { MutableInteractionSource() }, indication = null) { state.open = false },
                )
            }
        }
    }
}

@Composable
private fun SideNav(state: MinimalMenuState, modifier: Modifier) {
    val scope = rememberCoroutineScope()
    val markerY by animateDpAsState(
        NavRowHeight * state.current.ordinal + (NavRowHeight - MarkerHeight) / 2,
        spring(dampingRatio = 0.7f, stiffness = 500f),
        label = "marker",
    )
    Column(modifier.width(150.dp)) {
        Caps("MENU", color = LightColors.Muted, fontSize = 9.sp)
        Spacer(Modifier.height(18.dp))
        Box {
            Box(Modifier.offset(y = markerY).width(2.dp).height(MarkerHeight).background(LightColors.Ink))
            Column {
                MenuOption.entries.forEachIndexed { i, option ->
                    StaggerIn(index = i, visible = state.open, offsetX = -20f, baseDelayMillis = 120, stepMillis = 60) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(NavRowHeight)
                                .clickable(enabled = state.open) { scope.launch { state.pick(option) } }
                                .padding(start = 16.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            Caps(
                                option.label,
                                color = if (option == state.current) LightColors.Ink else LightColors.Muted,
                                fontSize = 12.sp,
                                letterSpacing = 2.5.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}
