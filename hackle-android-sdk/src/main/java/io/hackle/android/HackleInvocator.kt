package io.hackle.android

import io.hackle.sdk.common.HackleAppBridge

object HackleInvocator {
    @JvmStatic
    fun hackleAppBridge(): HackleAppBridge {
        return Hackle.app.hackleAppBridge()
    }
}
