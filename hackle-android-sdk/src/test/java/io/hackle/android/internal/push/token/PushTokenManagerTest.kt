package io.hackle.android.internal.push.token

import io.hackle.android.internal.database.repository.KeyValueRepository
import io.hackle.android.internal.database.repository.MapKeyValueRepository
import io.hackle.android.internal.push.PushEventTracker
import io.hackle.android.internal.push.PushPlatformType
import io.hackle.android.internal.push.PushProviderType
import io.hackle.android.internal.session.Session
import io.hackle.android.internal.time.FixedClock
import io.hackle.android.internal.user.UserManager
import io.hackle.sdk.common.User
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNull

class PushTokenManagerTest {

    private lateinit var repository: KeyValueRepository

    private lateinit var pushTokenFetcher: PushTokenFetcher

    private lateinit var pushEventTracker: PushEventTracker

    private lateinit var userManager: UserManager

    private lateinit var sut: PushTokenManager

    private val user = User.builder().deviceId("device").build()

    @Before
    fun before() {
        repository = MapKeyValueRepository()
        pushTokenFetcher = mockk()
        pushEventTracker = mockk(relaxed = true)
        userManager = mockk { every { currentUser } returns user }
        sut = PushTokenManager(repository, pushTokenFetcher, pushEventTracker, userManager, FixedClock(42))
    }

    private fun fetchedToken(token: PushToken?) {
        every { pushTokenFetcher.fetch(any()) } answers { firstArg<(PushToken?) -> Unit>().invoke(token) }
    }

    @Test
    fun `currentPushToken - when token is not stored then return null`() {
        // when
        val actual = sut.currentPushToken

        // then
        expectThat(actual).isNull()
    }

    @Test
    fun `currentPushToken - when token is stored then return stored token`() {
        // given
        repository.putString("fcm_token", "token")

        // when
        val actual = sut.currentPushToken

        // then
        expectThat(actual).isEqualTo(PushToken(PushPlatformType.ANDROID, PushProviderType.FCM, "token"))
    }

    @Test
    fun `initialize - when fetched token is null then do nothing`() {
        // given
        fetchedToken(null)

        // when
        sut.initialize()

        // then
        expectThat(sut.currentPushToken).isNull()
        expectThat(repository.getString("fcm_token")).isNull()
        verify(exactly = 0) { pushEventTracker.trackToken(any(), any(), any()) }
    }

    @Test
    fun `initialize - when fetched token is same as stored token then do nothing`() {
        // given
        val token = PushToken.of("token")
        fetchedToken(token)
        repository.putString("fcm_token", "token")

        sut.initialize()

        // then
        expectThat(sut.currentPushToken).isEqualTo(token)
        verify(exactly = 0) { pushEventTracker.trackToken(any(), any(), any()) }
    }

    @Test
    fun `initialize - when fetched token is different from stored token then replace token`() {
        // given
        val token = PushToken.of("new_token")
        fetchedToken(token)
        repository.putString("fcm_token", "old_token")

        sut.initialize()

        // then
        expectThat(sut.currentPushToken).isEqualTo(token)
        expectThat(repository.getString("fcm_token")).isEqualTo("new_token")
        verify(exactly = 1) { pushEventTracker.trackToken(token, user, 42) }
    }

    @Test
    fun `initialize - when token is fetched for the first time then store and track token`() {
        // given
        val token = PushToken.of("token")
        fetchedToken(token)

        // when
        sut.initialize()

        // then
        expectThat(repository.getString("fcm_token")).isEqualTo("token")
        verify(exactly = 1) { pushEventTracker.trackToken(token, user, 42) }
    }

    @Test
    fun `initialize - does not wait for push token fetch`() {
        // given
        every { pushTokenFetcher.fetch(any()) } returns Unit

        // when
        sut.initialize()

        // then
        expectThat(sut.currentPushToken).isNull()
        verify(exactly = 0) { pushEventTracker.trackToken(any(), any(), any()) }
    }

    @Test
    fun `initialize - when fetch throws exception then do not throw`() {
        // given
        every { pushTokenFetcher.fetch(any()) } throws IllegalStateException("fail")

        // when
        sut.initialize()

        // then
        expectThat(sut.currentPushToken).isNull()
    }

    @Test
    fun `when session started then track push token event`() {
        // given
        repository.putString("fcm_token", "token")

        // when
        sut.onSessionStarted(Session.create(42), user, 42)

        // then
        verify(exactly = 1) {
            pushEventTracker.trackToken(PushToken.of("token"), user, 42)
        }
    }
}
