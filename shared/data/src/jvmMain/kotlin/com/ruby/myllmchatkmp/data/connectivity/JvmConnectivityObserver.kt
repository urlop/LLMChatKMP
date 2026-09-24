package com.ruby.myllmchatkmp.data.connectivity

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Desktop wasn't a roadmap target; reports always-online. */
class JvmConnectivityObserver : ConnectivityObserver {
    override fun observe(): Flow<Boolean> = flowOf(true)
}
