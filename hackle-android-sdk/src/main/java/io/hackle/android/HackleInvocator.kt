package io.hackle.android

import io.hackle.sdk.common.HackleInvocator

object HackleAppInvocator {
    @JvmStatic
    fun hackleInvocator(): HackleInvocator {
        return Hackle.app.invocator()
    }
}
