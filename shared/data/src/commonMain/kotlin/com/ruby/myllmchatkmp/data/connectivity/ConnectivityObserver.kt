package com.ruby.myllmchatkmp.data.connectivity

import kotlinx.coroutines.flow.Flow

/** Real network monitoring on Android; iOS/JVM report always-online (see PLAN.md phase 4 note). */
interface ConnectivityObserver {
    fun observe(): Flow<Boolean>
}
