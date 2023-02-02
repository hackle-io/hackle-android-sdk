package io.hackle.android.internal.user

import io.hackle.android.internal.model.Device
import io.hackle.sdk.common.User
import io.hackle.sdk.core.user.HackleUser
import io.hackle.sdk.core.user.IdentifierType

internal class HackleUserResolver(
    private val device: Device,
) {
    fun resolve(user: User): HackleUser {
        return HackleUser.builder()
            .identifiers(user.identifiers)
            .identifier(IdentifierType.ID, user.id)
            .identifier(IdentifierType.ID, device.id, overwrite = false)
            .identifier(IdentifierType.USER, user.userId)
            .identifier(IdentifierType.DEVICE, user.deviceId)
            .identifier(IdentifierType.DEVICE, device.id, overwrite = false)
            .identifier(IdentifierType.HACKLE_DEVICE_ID, device.id)
            .properties(user.properties)
            .hackleProperties(device.properties)
            .build()
    }
}
