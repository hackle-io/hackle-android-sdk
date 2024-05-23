package io.hackle.android.internal.pushtoken

import io.hackle.android.HackleAppMode
import io.hackle.android.internal.database.repository.KeyValueRepository
import io.hackle.android.internal.database.repository.MapKeyValueRepository
import io.hackle.android.internal.push.PushEventTracker
import io.hackle.android.internal.pushtoken.datasource.PushTokenDataSource
import io.hackle.android.internal.user.UserManager
import io.hackle.sdk.common.User
import io.mockk.called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class PushTokenManagerTest {

    private lateinit var preferences: KeyValueRepository
    private lateinit var userManager: UserManager
    private lateinit var dataSource: PushTokenDataSource
    private lateinit var eventTracker: PushEventTracker

    private lateinit var manager: PushTokenManager

    @Before
    fun setup() {
        preferences = MapKeyValueRepository()
        userManager = mockk()
        every { userManager.currentUser } returns mockk()
        every { userManager.toHackleUser(any()) } returns mockk()
        dataSource = mockk()
        eventTracker = mockk(relaxed = true)

        manager = PushTokenManager(
            mode = HackleAppMode.NATIVE,
            preferences = preferences,
            userManager = userManager,
            dataSource = dataSource,
            eventTracker = eventTracker
        )
    }

    @Test
    fun initialize() {
        val pushToken = "foobar1234"
        every { dataSource.getPushToken() } returns pushToken
        manager.initialize()

        verify(exactly = 1) {
            eventTracker.trackToken("foobar1234", any(), any())
        }
        expectThat(manager.registeredPushToken).isEqualTo(pushToken)
    }

    @Test
    fun `initialize with same push token`() {
        val pushToken = "foobar1234"
        preferences.putString("fcm_token", pushToken)
        every { dataSource.getPushToken() } returns pushToken

        manager.initialize()

        verify {
            eventTracker wasNot called
        }
        expectThat(manager.registeredPushToken).isEqualTo(pushToken)
    }

    @Test
    fun `initialize with another new push token`() {
        val pushToken = "foobar1234"
        val newPushToken = "newfoobar123"
        preferences.putString("fcm_token", pushToken)
        every { dataSource.getPushToken() } returns newPushToken

        expectThat(manager.registeredPushToken).isEqualTo(pushToken)

        manager.initialize()

        verify(exactly = 1) {
            eventTracker.trackToken(newPushToken, any(), any())
        }
        expectThat(manager.registeredPushToken).isEqualTo(newPushToken)
    }

    @Test
    fun `when webwiew wrapper mode then do not notify push token`() {
        val pushToken = "foobar1234"
        val newPushToken = "newfoobar123"
        preferences.putString("fcm_token", pushToken)
        every { dataSource.getPushToken() } returns newPushToken

        val sut = PushTokenManager(
            mode = HackleAppMode.WEB_VIEW_WRAPPER,
            preferences = preferences,
            userManager = userManager,
            dataSource = dataSource,
            eventTracker = eventTracker
        )
        expectThat(sut.registeredPushToken).isEqualTo(pushToken)

        sut.initialize()

        verify(exactly = 0) {
            eventTracker.trackToken(any(), any(), any())
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
            eventTracker.trackToken(pushToken, any(), any())
        }
    }
}
