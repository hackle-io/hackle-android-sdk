package io.hackle.android.internal.user

import io.hackle.android.internal.model.Device
import io.hackle.android.mock.MockDevice
import io.hackle.sdk.common.User
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class HackleUserResolverTest {

    @Test
    fun `resolve`() {
        val device = MockDevice("hackleDeviceId", mapOf("key" to "hackle_value"))
        val resolver = HackleUserResolver(device)

        val user = User.builder("id")
            .userId("userId")
            .deviceId("deviceId")
            .identifier("customId", "customValue")
            .property("key", "user_value")
            .build()

        val hackleUser = resolver.resolve(user)

        expectThat(hackleUser) {
            get { identifiers } isEqualTo mapOf(
                "customId" to "customValue",
                "\$id" to "id",
                "\$userId" to "userId",
                "\$deviceId" to "deviceId",
                "\$hackleDeviceId" to "hackleDeviceId",
            )
            get { properties } isEqualTo mapOf("key" to "user_value")
            get { hackleProperties } isEqualTo mapOf("key" to "hackle_value")
        }
    }

    @Test
    fun `식별자 없는 경우`() {
        val device = MockDevice("hackleDeviceId", mapOf("key" to "hackle_value"))
        val resolver = HackleUserResolver(device)

        val hackleUser = resolver.resolve(User.builder().build())

        expectThat(hackleUser) {
            get { identifiers } isEqualTo mapOf(
                "\$id" to "hackleDeviceId",
                "\$deviceId" to "hackleDeviceId",
                "\$hackleDeviceId" to "hackleDeviceId"
            )
        }
    }
}