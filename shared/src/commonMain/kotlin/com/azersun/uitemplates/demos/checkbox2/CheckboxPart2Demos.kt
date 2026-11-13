package com.azersun.uitemplates.demos.checkbox2

import com.azersun.uitemplates.showcase.Demo
import com.azersun.uitemplates.showcase.DemoGroup

val checkboxPart2Group: DemoGroup = DemoGroup(
    "Checkbox · Part 2",
    listOf(
        Demo("checkbox2-drag", "Drag select", "Drag down the list and every box you pass checks", { DragSelectDemo() }),
        Demo("checkbox2-hand-drawn", "Hand-drawn tick", "A pencil scribbles a rough ✓ that overshoots the box", { HandDrawnTickDemo() }),
        Demo("checkbox2-flip", "Flip tick", "The box lifts and flips in 3D to a filled ✓ back", { FlipTickDemo() }),
        Demo("checkbox2-ring", "Progress ring", "Each task fills the ring; at 3/3 it becomes a glowing ✓", { ProgressRingDemo() }),
        Demo("checkbox2-tri-state", "Tri-state", "Empty → – → ✓, with the dash bending into the check", { TriStateDemo() }),
        Demo("checkbox2-pop", "Pop burst", "The box squishes, then pops a ✓ with a ring and dots", { PopBurstDemo() }),
    ),
)
