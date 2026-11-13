package com.azersun.uitemplates.demos.switch1

import com.azersun.uitemplates.showcase.Demo
import com.azersun.uitemplates.showcase.DemoGroup

val switchPart1Group: DemoGroup = DemoGroup(
    "Switch · Part 1",
    listOf(
        Demo("switch1-liquid", "Liquid toggle", "The knob stretches like a drop, then squishes at the edge", { LiquidToggleDemo() }),
        Demo("switch1-day-night", "Day & night", "The sun rolls into a moon, clouds sink, stars twinkle", { DayNightDemo() }),
        Demo("switch1-icon-morph", "Icon morph", "The knob spins across as its ✕ morphs into a ✓", { IconMorphDemo() }),
        Demo("switch1-lamp", "Pull cord lamp", "Pull the cord: the bulb flickers on and the lamp sways", { PullCordLampDemo() }),
        Demo("switch1-checkbox", "Checkbox", "The box squishes, fills from the centre, a ✓ draws in", { GradientCheckboxDemo() }),
        Demo("switch1-segmented", "Segmented", "The pill stretches on a spring and a −20% tag pops in", { SegmentedDemo() }),
    ),
)
