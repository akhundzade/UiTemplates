package com.azersun.uitemplates.demos.screentransition

import com.azersun.uitemplates.showcase.Demo
import com.azersun.uitemplates.showcase.DemoGroup

val screenTransitionGroup: DemoGroup = DemoGroup(
    "Screen transition",
    listOf(
        Demo("screen-swipeback", "Swipe back", "Drag from the edge; the list slides in behind with parallax", { SwipeBackTransitionDemo() }),
        Demo("screen-doors", "Split doors", "The old screen parts down the middle like a pair of doors", { SplitDoorsTransitionDemo() }),
        Demo("screen-blinds", "Blinds", "The new screen turns in on six slats from the top down", { BlindsTransitionDemo() }),
        Demo("screen-depth", "Depth stack", "The old screen tilts back as the new one rises from below", { DepthStackTransitionDemo() }),
        Demo("screen-blur", "Blur dissolve", "One screen blurs away as the next sharpens into focus", { BlurDissolveTransitionDemo() }),
        Demo("screen-liquid", "Liquid wipe", "A wavy, glowing edge sweeps across to reveal the next screen", { LiquidWipeTransitionDemo() }),
    ),
)
