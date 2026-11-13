package com.azersun.uitemplates.demos.inputerror

import com.azersun.uitemplates.showcase.Demo
import com.azersun.uitemplates.showcase.DemoGroup

val inputErrorGroup: DemoGroup = DemoGroup(
    "Input error",
    listOf(
        Demo("inputerror-zigzag", "Zigzag underline", "The underline snaps into a red zigzag that buzzes flat", { ZigzagUnderlineDemo() }),
        Demo("inputerror-alert-pop", "Alert pop", "A red ! springs out with a burst of sparks", { AlertPopDemo() }),
        Demo("inputerror-message-drop", "Message drop", "The message drops from under the field and swings", { MessageDropDemo() }),
        Demo("inputerror-red-echo", "Red echo", "The border flashes red twice and sends out echoes", { RedEchoDemo() }),
        Demo("inputerror-letter-jitter", "Letter jitter", "Each letter hops and shivers red in turn", { LetterJitterDemo() }),
        Demo("inputerror-tilt-wobble", "Tilt wobble", "The field rocks like jelly, then settles", { TiltWobbleDemo() }),
    ),
)
