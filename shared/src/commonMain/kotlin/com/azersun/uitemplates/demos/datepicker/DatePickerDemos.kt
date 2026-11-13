package com.azersun.uitemplates.demos.datepicker

import com.azersun.uitemplates.showcase.Demo
import com.azersun.uitemplates.showcase.DemoGroup

val datePickerGroup: DemoGroup = DemoGroup(
    "Date picker",
    listOf(
        Demo("datepicker-halo", "Halo trace", "A glowing ring traces the day, then it fills", { HaloTraceDemo() }),
        Demo("datepicker-brackets", "Snap brackets", "Corner brackets fly over, open wide and snap shut", { SnapBracketsDemo() }),
        Demo("datepicker-rise", "Rise & land", "The day lifts on a shadow and lands with a squash", { RiseLandDemo() }),
        Demo("datepicker-wipe", "Side wipe", "Color wipes out of the old day and into the new", { SideWipeDemo() }),
        Demo("datepicker-dot", "Dot swell", "A dot under the date swells into the capsule", { DotSwellDemo() }),
        Demo("datepicker-twist", "Twist in", "The capsule twists in from a quarter spin", { TwistInDemo() }),
    ),
)
