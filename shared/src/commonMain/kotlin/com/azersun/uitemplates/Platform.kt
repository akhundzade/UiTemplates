package com.azersun.uitemplates

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform