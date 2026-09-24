package com.ruby.myllmchatkmp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform