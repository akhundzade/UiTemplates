package com.azersun.uitemplates.demos.textfield

import com.azersun.uitemplates.showcase.Demo
import com.azersun.uitemplates.showcase.DemoGroup

val textFieldGroup: DemoGroup = DemoGroup(
    "Text field",
    listOf(
        Demo("textfield-floating", "Floating label", "The label floats into a gap in the outline", { FloatingLabelDemo() }),
        Demo("textfield-underline", "Underline grow", "A gradient underline grows from the tap point", { UnderlineGrowDemo() }),
        Demo("textfield-glow", "Glow border", "A comet of light circles the focused pill", { GlowBorderDemo() }),
        Demo("textfield-validation", "Live validation", "Pink with a hint while invalid, teal with a ✓ once valid", { LiveValidationDemo() }),
        Demo("textfield-password", "Password reveal", "Dots flip into letters one by one", { PasswordRevealDemo() }),
        Demo("textfield-counter", "Char counter", "A ring fills, turns amber, then pink and shakes", { CharCounterDemo() }),
    ),
)
