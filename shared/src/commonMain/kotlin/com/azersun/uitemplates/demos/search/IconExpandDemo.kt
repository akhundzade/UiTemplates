package com.azersun.uitemplates.demos.search

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun IconExpandDemo() {
    val autoplay = rememberAutoplay()
    val query = remember { Query() }
    var expanded by remember { mutableStateOf(false) }
    val expand = remember { Animatable(0f) }
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(focused) {
        if (focused) expanded = true
    }
    LaunchedEffect(expanded) {
        expand.animateTo(
            if (expanded) 1f else 0f,
            if (expanded) spring(dampingRatio = 0.62f, stiffness = 240f) else spring(dampingRatio = 0.85f, stiffness = 320f),
        )
    }
    AutoplayEffect(autoplay) {
        expanded = false
        autoplay.focus = false
        query.set("")
        delay(AutoplayDelayMillis)
        expanded = true
        autoplay.focus = true
        delay(560)
        query.type("running shoes")
    }

    SearchStage(1, "Icon Expand", "The round button stretches into a search bar; text types in.", autoplay) {
        BoxWithConstraints(
            Modifier
                .align(Alignment.Center)
                .padding(horizontal = 28.dp)
                .widthIn(max = 380.dp)
                .fillMaxWidth()
                .height(FieldHeight),
        ) {
            val collapsedPx = with(LocalDensity.current) { FieldHeight.toPx() }
            val fullPx = constraints.maxWidth.toFloat()
            SearchField(
                query = query,
                placeholder = "Search products",
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    // Lets the spring's overshoot push the bar past its slot instead of being clamped.
                    .wrapContentWidth(Alignment.End, unbounded = true)
                    .layout { measurable, constraints ->
                        val width = (collapsedPx + (fullPx - collapsedPx) * expand.value).coerceAtLeast(0f).roundToInt()
                        val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
                        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                    }
                    .clickable(enabled = !expanded, interactionSource = null, indication = null) {
                        expanded = true
                        focusRequester.requestFocus()
                    },
                glowing = !expanded || autoplay.focus,
                caret = autoplay.focus,
                interactionSource = interaction,
                focusRequester = focusRequester,
                textAlpha = { ((expand.value - 0.45f) / 0.4f).coerceIn(0f, 1f) },
                leading = { MagnifierIcon(SearchColors.Text) },
                trailing = {
                    ClearButton(visible = expanded && query.text.isNotEmpty()) {
                        query.set("")
                        expanded = false
                        focusManager.clearFocus()
                    }
                },
            )
        }
    }
}
