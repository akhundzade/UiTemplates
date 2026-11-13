package com.azersun.uitemplates.demos.switch2

import com.azersun.uitemplates.showcase.Demo
import com.azersun.uitemplates.showcase.DemoGroup

val switchPart2Group: DemoGroup = DemoGroup(
    "Switch · Part 2",
    listOf(
        Demo("switch2-flip", "Flip knob", "The knob flips like a coin from a gray O to a glowing I", { FlipKnobDemo() }),
        Demo("switch2-neon", "Neon glow", "A spark races the outline, then the toggle glows neon", { NeonGlowDemo() }),
        Demo("switch2-push", "3D push button", "A round 3D button sinks, stays down and lights a ring", { PushButton3DDemo() }),
        Demo("switch2-rocker", "Rocker switch", "A wall rocker snaps over and its LED glows cyan", { RockerSwitchDemo() }),
        Demo("switch2-emoji", "Emoji face", "The face blinks as it slides and its frown turns to a smile", { EmojiFaceDemo() }),
        Demo("switch2-mute", "Mute toggle", "The slash wipes off and sound waves ripple out", { MuteToggleDemo() }),
    ),
)
