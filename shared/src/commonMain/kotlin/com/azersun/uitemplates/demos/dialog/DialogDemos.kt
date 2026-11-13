package com.azersun.uitemplates.demos.dialog

import com.azersun.uitemplates.showcase.Demo
import com.azersun.uitemplates.showcase.DemoGroup

val dialogGroup: DemoGroup = DemoGroup(
    "Dialog",
    listOf(
        Demo("dialog-pop", "Scale pop", "Pops up from 72% with a soft overshoot", { ScalePopDemo() }),
        Demo("dialog-drop", "Spring drop", "Drops in on a spring; Cancel lifts, Delete drops", { SpringDropDemo() }),
        Demo("dialog-hinge", "Hinge swing", "Swings down in 3D from its top edge", { HingeSwingDemo() }),
        Demo("dialog-line", "Line unfold", "A glowing line opens, then unfolds into the dialog", { LineUnfoldDemo() }),
        Demo("dialog-focus", "Soft focus", "Blurs into focus as the app behind softens", { SoftFocusDemo() }),
        Demo("dialog-wobble", "Wobble in", "Grows in with a jelly wobble, squashes flat away", { WobbleInDemo() }),
    ),
)
