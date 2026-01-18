package me.deference.formsample

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform