package io.hackle.android.internal.user

import io.hackle.android.internal.model.Device
import io.hackle.sdk.common.User
import io.hackle.sdk.core.user.HackleUser

internal class HackleUserResolver(
    private val device: Device,
) {

    fun resolveOrNull(user: User): HackleUser? {
        val decoratedUser = decorateUser(user)
        val hackleUser = HackleUser.of(decoratedUser, device.properties)

        if (hackleUser.identifiers.isEmpty()) {
            return null
        }

        return hackleUser
    }

    private fun decorateUser(user: User): User {
        return if (user.deviceId == null) {
            user.copy(deviceId = device.id)
        } else {
            user
        }
    }
}
