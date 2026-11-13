package com.azersun.uitemplates.demos.like

import com.azersun.uitemplates.showcase.Demo
import com.azersun.uitemplates.showcase.DemoGroup

val likeGroup: DemoGroup = DemoGroup(
    "Like",
    listOf(
        Demo("like-burst", "Heart burst", "The heart pops with a ring and a burst of tiny hearts", { HeartBurstDemo() }),
        Demo("like-double-tap", "Double-tap heart", "A big heart bounces in where you double-tap the photo", { DoubleTapHeartDemo() }),
        Demo("like-count", "Count roll", "Only the changing digits roll up as a +1 floats away", { CountRollDemo() }),
        Demo("like-floating", "Floating hearts", "Tap or hold to send hearts swaying up a live stream", { FloatingHeartsDemo() }),
        Demo("like-outline-fill", "Outline fill", "The outline draws in, then hold to pour in liquid", { OutlineFillDemo() }),
        Demo("like-thumbs-up", "Thumbs up", "The thumb springs upright with a wobble and sparks", { ThumbsUpDemo() }),
    ),
)
