package com.azersun.uitemplates.demos.search

import com.azersun.uitemplates.showcase.Demo
import com.azersun.uitemplates.showcase.DemoGroup

val searchGroup: DemoGroup = DemoGroup(
    "Search",
    listOf(
        Demo("search-expand", "Icon expand", "The round button stretches into a bar and types", { IconExpandDemo() }),
        Demo("search-suggestions", "Live suggestions", "Suggestions drop in with the match in bold", { LiveSuggestionsDemo() }),
        Demo("search-clear", "Clear field", "Letters fall away and the placeholder fades in", { ClearFieldDemo() }),
        Demo("search-voice", "Voice search", "Rings pulse, the wave dances, words pop in", { VoiceSearchDemo() }),
        Demo("search-focus", "Focus morph", "The bar rises to the top and Cancel slides in", { FocusMorphDemo() }),
        Demo("search-loading", "Search loading", "The icon spins, skeletons shimmer, results fade in", { SearchLoadingDemo() }),
    ),
)
