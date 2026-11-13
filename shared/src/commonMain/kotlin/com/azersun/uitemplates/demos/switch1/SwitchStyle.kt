package com.azersun.uitemplates.demos.switch1

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.azersun.uitemplates.demos.menus.MenuColors

internal object SwitchColors {
    val Track = Color(0xFF2E2A5C)
    val Knob = Color(0xFFF7F6FF)
    val Icon = Color(0xFF8E8AB8)
    val Outline = Color(0xFF625D9E)
    val Violet = MenuColors.Violet
    val Blue = MenuColors.Blue
    val Cyan = MenuColors.Cyan
    val Pink = MenuColors.Pink
    val Text = MenuColors.Text
    val Muted = MenuColors.Muted
}

internal val TrackWidth = 120.dp
internal val TrackHeight = 64.dp
internal val KnobInset = 7.dp

/** The rounded track: the off colour, with [brush] faded in over it by [fill]. */
internal fun DrawScope.drawTrack(fill: Float, brush: Brush) {
    val radius = CornerRadius(size.height / 2)
    drawRoundRect(SwitchColors.Track, cornerRadius = radius)
    if (fill > 0f) drawRoundRect(brush, cornerRadius = radius, alpha = fill.coerceAtMost(1f))
}

internal fun DrawScope.drawKnob(center: Offset, radius: Float, color: Color = SwitchColors.Knob) {
    drawCircle(Color.Black, radius, center + Offset(0f, radius * 0.08f), alpha = 0.22f)
    drawCircle(color, radius, center)
}
