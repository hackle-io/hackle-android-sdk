package io.hackle.android.internal.pushtoken

import io.hackle.android.internal.database.repository.KeyValueRepository
import io.hackle.android.internal.database.repository.MapKeyValueRepository
import io.hackle.android.internal.pushtoken.registration.PushTokenRegistration
import io.hackle.android.internal.user.UserManager
import io.hackle.sdk.common.User
import io.hackle.sdk.core.HackleCore
import io.mockk.called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class PushTokenManagerTest {

    private lateinit var core: HackleCore
    private lateinit var preferences: KeyValueRepository
    private lateinit var userManager: UserManager
    private lateinit var registration: PushTokenRegistration

    private lateinit var manager: PushTokenManager

    @Before
    fun setup() {
        core = mockk(relaxed = true)
        preferences = MapKeyValueRepository()
        userManager = mockk()
        every { userManager.currentUser } returns mockk()
        every { userManager.toHackleUser(any()) } returns mockk()
        registration = mockk()

        manager = PushTokenManager(
            core = core,
            preferences = preferences,
            userManager = userManager,
            registration = registration
        )
    }

    @Test
    fun initialize() {
        val pushToken = "foobar1234"
        every { registration.getPushToken() } returns pushToken
        manager.initialize()

        verify(exactly = 1) {
            core.track(
                withArg {
                    expectThat(it.key).isEqualTo("\$push_token")
                    expectThat(it.properties["provider_type"]).isEqualTo("FCM")
                    expectThat(it.properties["token"]).isEqualTo(pushToken)
                },
                any(),
                any()
            )
        }
        expectThat(manager.registeredPushToken).isEqualTo(pushToken)
    }

    @Test
    fun `initialize with same push token`() {
        val pushToken = "foobar1234"
        preferences.putString("fcm_token", pushToken)
        every { registration.getPushToken() } returns pushToken

        manager.initialize()

        verify {
            core wasNot called
        }
        expectThat(manager.registeredPushToken).isEqualTo(pushToken)
    }

    @Test
    fun `initialize with another new push token`() {
        val pushToken = "foobar1234"
        val newPushToken = "newfoobar123"
        preferences.putString("fcm_token", pushToken)
        every { registration.getPushToken() } returns newPushToken

        expectThat(manager.registeredPushToken).isEqualTo(pushToken)

        manager.initialize()

        verify(exactly = 1) {
            core.track(
                withArg {
                    expectThat(it.key).isEqualTo("\$push_token")
                    expectThat(it.properties["provider_type"]).isEqualTo("FCM")
                    expectThat(it.properties["token"]).isEqualTo(newPushToken)
                },
                any(),
                any()
            )
        }
        expectThat(manager.registeredPushToken).isEqualTo(newPushToken)
    }

    @Test
    fun `resend push token when user updated called`() {
        val pushToken = "foobar1234"
        preferences.putString("fcm_token", pushToken)

        val timestamp = System.currentTimeMillis()
        manager.onUserUpdated(User.of("foo"), User.of("bar"), timestamp)

        verify(exactly = 1) {
            core.track(
                withArg {
                    expectThat(it.key).isEqualTo("\$push_token")
                    expectThat(it.properties["token"]).isEqualTo(pushToken)
                }, any(), timestamp
            )
        }

    }
}