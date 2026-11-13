package com.azersun.uitemplates.demos.viewas

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val InStepMillis = 70L
private const val OutStepMillis = 45L

@Composable
fun ZipperRowsDemo() {
    val state = rememberViewAsState()
    val surface = remember { Animatable(0f) }
    val rows = remember { List(ViewMode.entries.size) { Animatable(0f) } }
    var shown by remember { mutableStateOf(false) }

    LaunchedEffect(state.open) {
        if (state.open) {
            shown = true
            launch { surface.animateTo(1f, tween(180)) }
            rows.forEachIndexed { i, row ->
                launch {
                    delay(60L + i * InStepMillis)
                    row.animateTo(1f, spring(dampingRatio = 0.68f, stiffness = 380f))
                }
            }
        } else if (shown) {
            rows.forEachIndexed { i, row ->
                launch {
                    delay(i * OutStepMillis)
                    row.animateTo(0f, tween(220, easing = FastOutLinearInEasing))
                }
            }
            delay(rows.size * OutStepMillis + 120L)
            surface.animateTo(0f, tween(160))
            shown = false
        }
    }

    ViewAsScene(state, 4, "Zipper Rows", "Rows slide in from alternating sides; slide out on pick.") {
        if (shown) {
            ViewAsMenu(
                state,
                backgroundAlpha = { surface.value },
                row = { i ->
                    Modifier.graphicsLayer {
                        val v = rows[i].value
                        val side = if (i % 2 == 0) -1f else 1f
                        translationX = (1f - v) * side * size.width
                        alpha = (v * 1.4f).coerceIn(0f, 1f)
                    }
                },
            )
        }
    }
}
