package io.hackle.android.internal.remoteconfig

import io.hackle.android.internal.user.HackleUserResolver
import io.hackle.android.internal.user.UserManager
import io.hackle.sdk.common.User
import io.hackle.sdk.common.decision.DecisionReason
import io.hackle.sdk.common.decision.RemoteConfigDecision
import io.hackle.sdk.core.client.HackleInternalClient
import io.hackle.sdk.core.user.HackleUser
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class HackleRemoteConfigImplTest {

    @Test
    fun `user 가 null 인 경우 UserManager 의 currentUser 를 사용해야 한다`() {
        val user = User.of("test_user")
        val userManager = mockk<UserManager> { every { currentUser } returns user }
        val hackleUserResolver = mockk<HackleUserResolver> {
            every { resolve(any()) } answers { HackleUser.of(firstArg(), emptyMap()) }
        }
        val client = mockk<HackleInternalClient> {
            every { remoteConfig(any(), any(), any(), any()) } answers {
                RemoteConfigDecision.of("42", DecisionReason.DEFAULT_RULE)
            }
        }

        val remoteConfig = HackleRemoteConfigImpl(
            null,
            client,
            userManager,
            hackleUserResolver
        )

        val actual = remoteConfig.getString("test", "value")

        expectThat(actual) isEqualTo "42"
        verify { hackleUserResolver.resolve(user) }
    }

    @Test
    fun `user 가 있는경우 업데이트 후 사용한다`() {
        val user = User.of("test_user")
        val updatedUser = User.of("updated_user")
        val userManager = mockk<UserManager> { every { setUser(any()) } answers { updatedUser } }
        val hackleUserResolver = mockk<HackleUserResolver> {
            every { resolve(any()) } answers { HackleUser.of(firstArg(), emptyMap()) }
        }
        val client = mockk<HackleInternalClient> {
            every { remoteConfig(any(), any(), any(), any()) } answers {
                RemoteConfigDecision.of("42", DecisionReason.DEFAULT_RULE)
            }
        }

        val remoteConfig = HackleRemoteConfigImpl(
            user,
            client,
            userManager,
            hackleUserResolver
        )

        val actual = remoteConfig.getString("test", "value")

        expectThat(actual) isEqualTo "42"
        verify { hackleUserResolver.resolve(updatedUser) }
    }
}