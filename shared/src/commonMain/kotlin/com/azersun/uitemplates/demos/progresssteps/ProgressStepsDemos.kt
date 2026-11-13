package com.azersun.uitemplates.demos.progresssteps

import com.azersun.uitemplates.showcase.Demo
import com.azersun.uitemplates.showcase.DemoGroup

val progressStepsGroup: DemoGroup = DemoGroup(
    "Progress steps",
    listOf(
        Demo("steps-bead", "Bead line", "A glowing bead runs to the next dot and lights the line", { BeadLineDemo() }),
        Demo("steps-number", "Big number", "The step number flips like a split-flap; the name rolls", { BigNumberDemo() }),
        Demo("steps-backnext", "Back & next", "The line fills and the title slides in either direction", { BackNextDemo() }),
        Demo("steps-path", "Skill path", "Gold ticks, a hopping star and nodes that unlock with a pop", { SkillPathDemo() }),
        Demo("steps-blocks", "Level blocks", "A ball with eyes hops up the stairs as each block rises", { LevelBlocksDemo() }),
        Demo("steps-gauge", "Gauge arc", "The arc fills and the needle springs to the next step", { GaugeArcDemo() }),
    ),
)
