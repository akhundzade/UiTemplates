package com.azersun.uitemplates.showcase

import androidx.compose.runtime.Composable

class Demo(
    val id: String,
    val title: String,
    val subtitle: String,
    val content: @Composable () -> Unit,
)

class DemoGroup(val title: String, val demos: List<Demo>)

val demoGroups: List<DemoGroup> = listOf()
