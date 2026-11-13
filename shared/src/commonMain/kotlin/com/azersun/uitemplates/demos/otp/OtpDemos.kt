package com.azersun.uitemplates.demos.otp

import com.azersun.uitemplates.showcase.Demo
import com.azersun.uitemplates.showcase.DemoGroup

val otpGroup: DemoGroup = DemoGroup(
    "Verification code",
    listOf(
        Demo("otp-bounce", "Box bounce", "Digits drop in, boxes squash and bounce", { BoxBounceOtpDemo() }),
        Demo("otp-focus", "Stretchy focus", "Focus ring stretches box to box, then wraps the code", { StretchyFocusOtpDemo() }),
        Demo("otp-spin", "Digit spin", "Digits roll in like a slot machine, then spin again", { DigitSpinOtpDemo() }),
        Demo("otp-merge", "Box merge", "Digits pop in, boxes slide into one bar with a ✓", { BoxMergeOtpDemo() }),
        Demo("otp-zoom", "Zoom land", "Digits zoom down into place, a shine turns them mint", { ZoomLandOtpDemo() }),
        Demo("otp-tint", "Tint rise", "Boxes fill with colour from the bottom as digits land", { TintRiseOtpDemo() }),
    ),
)
