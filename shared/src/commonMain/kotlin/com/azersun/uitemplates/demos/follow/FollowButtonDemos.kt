package com.azersun.uitemplates.demos.follow

import com.azersun.uitemplates.showcase.Demo
import com.azersun.uitemplates.showcase.DemoGroup

val followButtonGroup: DemoGroup = DemoGroup(
    "Follow button",
    listOf(
        Demo("follow-coin", "Coin flip", "The button flips over like a coin to its other side", { CoinFlipFollowDemo() }),
        Demo("follow-blinds", "Blinds turn", "Ten slats turn from the middle outward like blinds", { BlindsTurnFollowDemo() }),
        Demo("follow-letters", "Letter drop", "Old letters fall away, new ones drop in and bounce", { LetterDropFollowDemo() }),
        Demo("follow-plusone", "Plus one", "A +1 chip flies up to the follower count", { PlusOneFollowDemo() }),
        Demo("follow-ring", "Avatar ring", "A violet-to-cyan ring draws round the avatar", { AvatarRingFollowDemo() }),
        Demo("follow-stamp", "Stamp press", "The new state slams down like a stamp", { StampPressFollowDemo() }),
    ),
)
