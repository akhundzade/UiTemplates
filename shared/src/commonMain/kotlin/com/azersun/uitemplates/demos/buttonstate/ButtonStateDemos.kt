package com.azersun.uitemplates.demos.buttonstate

import com.azersun.uitemplates.showcase.Demo
import com.azersun.uitemplates.showcase.DemoGroup

val buttonStateGroup: DemoGroup = DemoGroup(
    "Button · State change",
    listOf(
        Demo("buttonstate-liquid", "Liquid rise", "The next colour rises as a wave; the label swaps half way", { LiquidRiseDemo() }),
        Demo("buttonstate-magnetic", "Magnetic pull", "Leans toward your finger, sucks the label in on release", { MagneticPullDemo() }),
        Demo("buttonstate-barrel", "Barrel roll", "The pill rolls up like a barrel onto its other face", { BarrelRollDemo() }),
        Demo("buttonstate-pixels", "Pixel scatter", "The old state shatters into pixels flying from the tap", { PixelScatterDemo() }),
        Demo("buttonstate-comet", "Edge comet", "Two comets race round the edge, dragging the new colour", { EdgeCometDemo() }),
        Demo("buttonstate-letters", "Letter drop", "Letters drop out one by one; new ones fall in and bounce", { LetterDropDemo() }),
    ),
)
