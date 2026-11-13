package com.azersun.uitemplates.demos.inputerror

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

@Composable
fun MessageDropDemo() {
    val form = remember { EmailFormState() }
    val drop = remember { Animatable(0f) }
    val swing = remember { Animatable(0f) }
    val error = form.status == FieldStatus.Error
    LaunchedEffect(form.status, form.errorTick) {
        if (!error) return@LaunchedEffect
        drop.snapTo(0f)
        swing.snapTo(0f)
        drop.animateTo(1f, tween(240, easing = FastOutLinearInEasing))
        // The left end catches like a chain while the right end keeps falling: a velocity kick, not a pose,
        // so the swing peaks at about 12° and then decays.
        swing.animateTo(0f, spring(dampingRatio = 0.18f, stiffness = 160f), initialVelocity = 200f)
    }
    val alpha by animateFloatAsState(if (error) 1f else 0f, if (error) snap() else tween(160), label = "helper")

    InputErrorStage(
        3,
        "Message Drop",
        "The message drops out from under the field and swings.",
        form,
        helper = {
            HelperText(
                Modifier.graphicsLayer {
                    // Fully tucked behind the field's bottom edge when drop is 0.
                    val hidden = HelperRowHeight.toPx() / 2 + size.height / 2 + 2.dp.toPx()
                    translationY = (drop.value - 1f) * hidden
                    transformOrigin = TransformOrigin(0f, 0.5f)
                    rotationZ = swing.value
                    this.alpha = alpha
                },
            )
        },
    ) {
        EmailField(form)
    }
}
