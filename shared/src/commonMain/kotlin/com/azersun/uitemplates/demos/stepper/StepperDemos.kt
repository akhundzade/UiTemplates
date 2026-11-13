package com.azersun.uitemplates.demos.stepper

import com.azersun.uitemplates.showcase.Demo
import com.azersun.uitemplates.showcase.DemoGroup

val stepperGroup: DemoGroup = DemoGroup(
    "Stepper",
    listOf(
        Demo("stepper-press", "Soft press", "Button squishes, the new number pops up", { SoftPressStepperDemo() }),
        Demo("stepper-slide", "Side slide", "Pill nudges, number slides in from the tapped side", { SideSlideStepperDemo() }),
        Demo("stepper-ripple", "Ripple reveal", "A ripple from the button reveals the new number", { RippleRevealStepperDemo() }),
        Demo("stepper-stretch", "Stretch pill", "Pill stretches toward the tap, number wobbles", { StretchPillStepperDemo() }),
        Demo("stepper-spin", "Glyph spin", "+ or − spins half a turn, number flips edge-on", { GlyphSpinStepperDemo() }),
        Demo("stepper-halo", "Halo burst", "A glow ring bursts out, number zooms down in", { HaloBurstStepperDemo() }),
    ),
)
