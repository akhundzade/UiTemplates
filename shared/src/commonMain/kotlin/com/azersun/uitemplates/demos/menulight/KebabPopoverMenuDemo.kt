package com.azersun.uitemplates.demos.menulight

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.azersun.uitemplates.demos.menus.StaggerIn
import kotlinx.coroutines.launch

private val PopoverShape = RoundedCornerShape(14.dp)

@Composable
fun KebabPopoverMenuDemo() {
    val state = remember { MinimalMenuState() }
    val scope = rememberCoroutineScope()
    val pop = remember { Animatable(0f) }
    val visible by remember { derivedStateOf { pop.value > 0.001f } }
    LaunchedEffect(state.open) {
        if (state.open) {
            pop.animateTo(1f, spring(dampingRatio = 0.68f, stiffness = 480f))
        } else {
            pop.animateTo(0f, tween(170, easing = FastOutLinearInEasing))
        }
    }

    MinimalMenuScene(state, 2, "Kebab Popover", "A rounded popover scales out of the ⋮ button, anchored at its top-right corner.") {
        MinimalPage(state) { KebabButton(state.open, onClick = { state.open = !state.open }) }
        if (state.open) {
            Box(Modifier.fillMaxSize().clickable(remember { MutableInteractionSource() }, indication = null) { state.open = false })
        }
        if (visible) {
            Column(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = PagePadding + KebabSize + 8.dp, end = PagePadding)
                    .graphicsLayer {
                        transformOrigin = TransformOrigin(1f, 0f)
                        scaleX = pop.value
                        scaleY = pop.value
                        alpha = (pop.value * 2f).coerceIn(0f, 1f)
                    }
                    .width(176.dp)
                    .shadow(18.dp, PopoverShape, ambientColor = Color.Black.copy(alpha = 0.10f), spotColor = Color.Black.copy(alpha = 0.16f))
                    .background(LightColors.Page, PopoverShape)
                    .border(1.dp, LightColors.Hairline, PopoverShape)
                    .padding(vertical = 6.dp),
            ) {
                MenuOption.entries.forEachIndexed { i, option ->
                    StaggerIn(index = i, visible = state.open, offsetX = 0f, offsetY = -6f, baseDelayMillis = 60, stepMillis = 40) {
                        OptionRow(
                            option,
                            active = option == state.current,
                            enabled = state.open,
                            onClick = { scope.launch { state.pick(option) } },
                        )
                    }
                }
            }
        }
    }
}
