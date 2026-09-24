package com.ruby.myllmchatkmp.data.connectivity

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * TODO(needs Mac/Xcode to verify): real monitoring needs the Network framework's NWPathMonitor,
 * which is C interop this sandbox can compile but never run against a live network -- see
 * PLAN.md phase 4 note. Reports always-online for now.
 */
class IosConnectivityObserver : ConnectivityObserver {
    override fun observe(): Flow<Boolean> = flowOf(true)
}
