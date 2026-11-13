package com.azersun.uitemplates.demos.button2

import com.azersun.uitemplates.showcase.Demo
import com.azersun.uitemplates.showcase.DemoGroup

val buttonPart2Group: DemoGroup = DemoGroup(
    "Button · Part 2",
    listOf(
        Demo("button2-hold", "Hold to confirm", "Hold down: a ring fills, then the button turns into a ✓", { HoldToConfirmDemo() }),
        Demo("button2-plane", "Paper plane", "The plane flies off with a trail as the label flips to Sent!", { PaperPlaneDemo() }),
        Demo("button2-confetti", "Confetti burst", "A tap squishes the button and bursts fluttering confetti", { ConfettiBurstDemo() }),
        Demo("button2-copy", "Copy feedback", "The copy icon morphs into a ✓ and a tooltip pops up", { CopyFeedbackDemo() }),
        Demo("button2-shine", "Shine sweep", "A diagonal shine sweeps across while sparkles twinkle", { ShineSweepDemo() }),
        Demo("button2-slide", "Slide to confirm", "Drag the knob to the end: the track fills, the knob turns ✓", { SlideToConfirmDemo() }),
    ),
)
