package com.azersun.uitemplates.demos.datepicker

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.azersun.uitemplates.demos.menus.MenuColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val RiseHeight = 10.dp
private val RiseElevation = 18.dp

@Composable
fun RiseLandDemo() {
    val state = rememberDatePickerState()
    val rise = remember { WeekDays.associateWith { Animatable(0f) } }
    val squash = remember { WeekDays.associateWith { Animatable(1f) } }
    val landed = remember { WeekDays.associateWith { Animatable(if (it == state.selected) 1f else 0f) } }

    LaunchedEffect(state.selected) {
        val day = state.selected
        if (landed.getValue(day).value == 1f) return@LaunchedEffect
        WeekDays.filter { it != day }.forEach { other ->
            launch { landed.getValue(other).animateTo(0f, tween(200)) }
            launch { rise.getValue(other).animateTo(0f, tween(160)) }
            launch { squash.getValue(other).animateTo(1f, tween(160)) }
        }
        val lift = rise.getValue(day)
        lift.animateTo(1f, tween(240, easing = FastOutSlowInEasing))
        delay(90)
        lift.animateTo(0f, tween(170, easing = FastOutLinearInEasing))
        launch { landed.getValue(day).animateTo(1f, tween(110)) }
        val squish = squash.getValue(day)
        squish.animateTo(0.9f, tween(70, easing = FastOutSlowInEasing))
        squish.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 650f))
    }

    DatePickerScene(
        state = state,
        number = 3,
        title = "Rise & Land",
        caption = "The tapped day rises on a deep shadow, then lands with a squash as the capsule appears.",
        cellModifier = { day ->
            Modifier.graphicsLayer {
                val r = rise.getValue(day).value
                val s = squash.getValue(day).value
                translationY = -RiseHeight.toPx() * r
                scaleY = s
                scaleX = 1f + (1f - s) * 0.6f
                transformOrigin = TransformOrigin(0.5f, 1f)
                shadowElevation = RiseElevation.toPx() * r
                shape = CapsuleShape
                clip = false
            }
        },
    ) { day, _ ->
        val r = rise.getValue(day)
        // The ground shadow is drawn in the cell's lifted layer, so it is pushed back down by the lift.
        Canvas(Modifier.matchParentSize()) {
            val lift = r.value
            if (lift <= 0f) return@Canvas
            val width = size.width * (0.9f - 0.25f * lift)
            val height = 7.dp.toPx()
            val top = size.height + RiseHeight.toPx() * lift - height * 0.4f
            drawOval(
                Color.Black,
                topLeft = Offset((size.width - width) / 2, top),
                size = Size(width, height),
                alpha = 0.5f * lift,
            )
        }
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer { alpha = r.value }
                .background(MenuColors.TileLight, CapsuleShape),
        )
        Capsule(Modifier.graphicsLayer { alpha = landed.getValue(day).value })
    }
}
