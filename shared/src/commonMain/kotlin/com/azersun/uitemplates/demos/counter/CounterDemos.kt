package com.azersun.uitemplates.demos.counter

import com.azersun.uitemplates.showcase.Demo
import com.azersun.uitemplates.showcase.DemoGroup

val counterGroup: DemoGroup = DemoGroup(
    "Number counter",
    listOf(
        Demo("counter-odometer", "Odometer roll", "Digits roll like a car odometer, the right ones furthest", { OdometerRollDemo() }),
        Demo("counter-flap", "Split flap", "Digits flap down in halves like an airport board", { SplitFlapDemo() }),
        Demo("counter-blur", "Blur slide", "The number slides down with motion blur, the new one drops in", { BlurSlideDemo() }),
        Demo("counter-spring", "Spring stagger", "New digits drop in one by one with a springy bounce", { SpringStaggerDemo() }),
        Demo("counter-decode", "Decode lock", "Digits scramble, then lock in from left to right", { DecodeLockDemo() }),
        Demo("counter-count", "Smooth count", "Counts to the new total, glowing mint up or pink down", { SmoothCountDemo() }),
    ),
)
