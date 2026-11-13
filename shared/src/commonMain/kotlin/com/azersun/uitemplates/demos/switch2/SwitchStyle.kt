package com.azersun.uitemplates.demos.switch2

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.azersun.uitemplates.demos.menus.MenuColors
import com.azersun.uitemplates.demos.menus.MenuStage
import kotlinx.coroutines.delay

internal val TrackWidth = 128.dp
internal val TrackHeight = 68.dp
internal val KnobInset = 6.dp
internal val KnobSize = TrackHeight - KnobInset * 2
internal val KnobTravel = TrackWidth - KnobSize - KnobInset * 2
internal val TrackOff = MenuColors.TileLight
internal val TrackOn = Brush.horizontalGradient(listOf(MenuColors.Violet, MenuColors.Blue))

private const val AutoplayDelayMillis = 700L

/** The shared stage: runs [autoplay] 700 ms after opening unless a touch lands first (which also cancels it). */
@Composable
internal fun SwitchStage(
    number: Int,
    title: String,
    caption: String,
    autoplay: suspend () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    var interacted by remember { mutableStateOf(false) }
    val currentAutoplay by rememberUpdatedState(autoplay)
    LaunchedEffect(interacted) {
        if (interacted) return@LaunchedEffect
        delay(AutoplayDelayMillis)
        currentAutoplay()
    }

    MenuStage(number, title, caption) {
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        interacted = true
                    }
                },
            contentAlignment = Alignment.Center,
            content = content,
        )
    }
}

internal fun mix(start: Float, stop: Float, fraction: Float): Float = start + (stop - start) * fraction

internal fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
    val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}
