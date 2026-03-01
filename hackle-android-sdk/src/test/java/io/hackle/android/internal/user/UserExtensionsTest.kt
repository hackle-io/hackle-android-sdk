package io.hackle.android.internal.user

import io.hackle.sdk.common.User
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isSameInstanceAs
import strikt.assertions.isTrue

internal class UserExtensionsTest {

    @Test
    fun `identifierEquals`() {

        val user = User.builder().build()
        expectThat(user.identifierEquals(null)).isFalse()
        expectThat(user.identifierEquals(User.builder().build())).isTrue()
        expectThat(user.identifierEquals(User.builder().userId("a").build())).isFalse()
        expectThat(user.identifierEquals(User.builder().deviceId("a").build())).isFalse()


        val u1 = User.builder("0")
            .userId("a")
            .deviceId("b")
            .identifier("type", "value")
            .property("key", "value")
            .build()

        val u2 = User.builder()
            .userId("a")
            .deviceId("b")
            .build()
        expectThat(u1.identifierEquals(u2)).isTrue()
    }

    @Test
    fun `mergeWith - null`() {
        val user = User.builder().build()
        expectThat(user.mergeWith(null)) isSameInstanceAs user
    }

    @Test
    fun `mergeWith - identifierEquals`() {
        val user1 = User.builder()
            .deviceId("deviceId")
            .property("a", "1")
            .property("b", "2")
            .build()

        val user2 = User.builder()
            .deviceId("deviceId")
            .property("b", "9")
            .property("c", "9")
            .build()
        expectThat(user1.mergeWith(user2)) {
            get { properties } isEqualTo mapOf(
                "a" to "1",
                "b" to "2",
                "c" to "9"
            )
        }
    }

    @Test
    fun `mergeWith - identifierNotEquals`() {
        val user1 = User.builder()
            .deviceId("deviceId")
            .property("a", "1")
            .property("b", "2")
            .build()

        val user2 = User.builder()
            .deviceId("deviceId2")
            .property("b", "9")
            .property("c", "9")
            .build()

        expectThat(user1.mergeWith(user2)) isSameInstanceAs user1
    }
}
