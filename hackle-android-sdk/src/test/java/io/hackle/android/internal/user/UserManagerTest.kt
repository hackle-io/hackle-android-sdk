package io.hackle.android.internal.user

import io.hackle.sdk.common.User
import io.hackle.sdk.core.user.HackleUser
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class UserManagerTest {

    private lateinit var update: MutableList<Pair<HackleUser, Long>>

    private fun sut(): UserManager {
        update = mutableListOf()
        return UserManager().also {
            it.addListener(object : UserListener {
                override fun onUserUpdated(user: HackleUser, timestamp: Long) {
                    update.add(user to timestamp)
                }
            })
        }
    }

    private fun updateUser(
        u1: Pair<String?, String?>,
        u2: Pair<String?, String?>,
        isSame: Boolean,
    ) {

        val sut = sut()

        val user1 = HackleUser.of(User.builder()
            .userId(u1.first)
            .deviceId(u1.second)
            .build())

        val user2 = HackleUser.of(User.builder()
            .userId(u2.first)
            .deviceId(u2.second)
            .build())

        sut.updateUser(user1)
        sut.updateUser(user2)

        expectThat(update.size).isEqualTo(if (isSame) 0 else 1)
    }

    @Test
    fun `updateUser`() {
        updateUser(null to null, null to null, true)
        updateUser(null to null, null to "a", false)
        updateUser(null to null, null to "b", false)
        updateUser(null to null, "a" to null, true)
        updateUser(null to null, "a" to "a", false)
        updateUser(null to null, "a" to "b", false)
        updateUser(null to null, "b" to null, true)
        updateUser(null to null, "b" to "a", false)
        updateUser(null to null, "b" to "b", false)

        updateUser(null to "a", null to null, false)
        updateUser(null to "a", null to "a", true)
        updateUser(null to "a", null to "b", false)
        updateUser(null to "a", "a" to null, false)
        updateUser(null to "a", "a" to "a", true)
        updateUser(null to "a", "a" to "b", false)
        updateUser(null to "a", "b" to null, false)
        updateUser(null to "a", "b" to "a", true)
        updateUser(null to "a", "b" to "b", false)

        updateUser(null to "b", null to null, false)
        updateUser(null to "b", null to "a", false)
        updateUser(null to "b", null to "b", true)
        updateUser(null to "b", "a" to null, false)
        updateUser(null to "b", "a" to "a", false)
        updateUser(null to "b", "a" to "b", true)
        updateUser(null to "b", "b" to null, false)
        updateUser(null to "b", "b" to "a", false)
        updateUser(null to "b", "b" to "b", true)

        updateUser("a" to null, null to null, true)
        updateUser("a" to null, null to "a", false)
        updateUser("a" to null, null to "b", false)
        updateUser("a" to null, "a" to null, true)
        updateUser("a" to null, "a" to "a", true)
        updateUser("a" to null, "a" to "b", true)
        updateUser("a" to null, "b" to null, false)
        updateUser("a" to null, "b" to "a", false)
        updateUser("a" to null, "b" to "b", false)

        updateUser("a" to "a", null to null, false)
        updateUser("a" to "a", null to "a", true)
        updateUser("a" to "a", null to "b", false)
        updateUser("a" to "a", "a" to null, true)
        updateUser("a" to "a", "a" to "a", true)
        updateUser("a" to "a", "a" to "b", true)
        updateUser("a" to "a", "b" to null, false)
        updateUser("a" to "a", "b" to "a", false)
        updateUser("a" to "a", "b" to "b", false)

        updateUser("a" to "b", null to null, false)
        updateUser("a" to "b", null to "a", false)
        updateUser("a" to "b", null to "b", true)
        updateUser("a" to "b", "a" to null, true)
        updateUser("a" to "b", "a" to "a", true)
        updateUser("a" to "b", "a" to "b", true)
        updateUser("a" to "b", "b" to null, false)
        updateUser("a" to "b", "b" to "a", false)
        updateUser("a" to "b", "b" to "b", false)

        updateUser("b" to null, null to null, true)
        updateUser("b" to null, null to "a", false)
        updateUser("b" to null, null to "b", false)
        updateUser("b" to null, "a" to null, false)
        updateUser("b" to null, "a" to "a", false)
        updateUser("b" to null, "a" to "b", false)
        updateUser("b" to null, "b" to null, true)
        updateUser("b" to null, "b" to "a", true)
        updateUser("b" to null, "b" to "b", true)

        updateUser("b" to "a", null to null, false)
        updateUser("b" to "a", null to "a", true)
        updateUser("b" to "a", null to "b", false)
        updateUser("b" to "a", "a" to null, false)
        updateUser("b" to "a", "a" to "a", false)
        updateUser("b" to "a", "a" to "b", false)
        updateUser("b" to "a", "b" to null, true)
        updateUser("b" to "a", "b" to "a", true)
        updateUser("b" to "a", "b" to "b", true)

        updateUser("b" to "b", null to null, false)
        updateUser("b" to "b", null to "a", false)
        updateUser("b" to "b", null to "b", true)
        updateUser("b" to "b", "a" to null, false)
        updateUser("b" to "b", "a" to "a", false)
        updateUser("b" to "b", "a" to "b", false)
        updateUser("b" to "b", "b" to null, true)
        updateUser("b" to "b", "b" to "a", true)
        updateUser("b" to "b", "b" to "b", true)
    }
}
