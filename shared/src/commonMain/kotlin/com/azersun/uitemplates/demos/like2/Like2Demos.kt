package com.azersun.uitemplates.demos.like2

import com.azersun.uitemplates.showcase.Demo
import com.azersun.uitemplates.showcase.DemoGroup

val likePart2Group: DemoGroup = DemoGroup(
    "Like · Part 2",
    listOf(
        Demo("like2-jelly", "Jelly squish", "Press squashes the heart; release jiggles it like jelly", { JellySquishDemo() }),
        Demo("like2-star", "Star favorite", "The star spins, fills gold→pink and sparkles twinkle", { StarFavoriteDemo() }),
        Demo("like2-bookmark", "Bookmark save", "Stretches, fills violet→cyan and pops a Saved toast", { BookmarkSaveDemo() }),
        Demo("like2-reactions", "Reaction picker", "Hold Like, slide to a reaction and it flies in", { ReactionPickerDemo() }),
        Demo("like2-pill", "Like pill", "A gradient spreads from your touch; Like becomes Liked", { LikePillDemo() }),
        Demo("like2-heartbeat", "Heartbeat", "Two beats, heart waves and a travelling ECG spike", { HeartbeatDemo() }),
    ),
)
