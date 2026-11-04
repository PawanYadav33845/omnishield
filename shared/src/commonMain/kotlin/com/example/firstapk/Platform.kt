package com.example.firstapk

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform