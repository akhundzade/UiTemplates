package com.azersun.uitemplates.demos.colorpicker

import com.azersun.uitemplates.showcase.Demo
import com.azersun.uitemplates.showcase.DemoGroup

val colorPickerGroup: DemoGroup = DemoGroup(
    "Color picker",
    listOf(
        Demo("colorpicker-halo", "Halo slide", "A halo springs to the swatch, the tee cross-fades", { HaloSlideDemo() }),
        Demo("colorpicker-check", "Check pop", "Swatch pops with a check, the tee squashes and recolours", { CheckPopDemo() }),
        Demo("colorpicker-drop", "Paint drop", "A drop arcs onto the tee and splashes the colour out", { PaintDropDemo() }),
        Demo("colorpicker-dye", "Dye dip", "The new colour rises up the tee with a wavy edge", { DyeDipDemo() }),
        Demo("colorpicker-tint", "Panel tint", "Swatch lifts on a glow, the panel tints to match", { PanelTintDemo() }),
        Demo("colorpicker-spin", "Turntable spin", "The tee spins once and comes back in the new colour", { TurntableSpinDemo() }),
    ),
)
