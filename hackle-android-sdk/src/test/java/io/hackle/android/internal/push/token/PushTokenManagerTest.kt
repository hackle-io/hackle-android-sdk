package io.hackle.android.internal.push.token

import io.hackle.android.internal.database.repository.KeyValueRepository
import io.hackle.android.internal.database.repository.MapKeyValueRepository
import io.hackle.android.internal.push.PushEventTracker
import io.hackle.android.internal.push.PushPlatformType
import io.hackle.android.internal.push.PushProviderType
import io.hackle.android.internal.session.Session
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

    private lateinit var sut: PushTokenManager

    @Before
    fun before() {
        repository = MapKeyValueRepository()
        pushTokenFetcher = mockk()
        pushEventTracker = mockk(relaxed = true)
        sut = PushTokenManager(repository, pushTokenFetcher, pushEventTracker)
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
        every { pushTokenFetcher.fetch() } returns null

        // when
        sut.initialize()

        // then
        expectThat(sut.currentPushToken).isNull()
        expectThat(repository.getString("fcm_token")).isNull()
    }

    @Test
    fun `initialize - when fetched token is same as stored token then do nothing`() {
        // given
        val token = PushToken.of("token")
        every { pushTokenFetcher.fetch() } returns token
        repository.putString("fcm_token", "token")

        sut.initialize()

        // then
        expectThat(sut.currentPushToken).isEqualTo(token)
    }

    @Test
    fun `initialize - when fetched token is different from stored token then replace token`() {
        // given
        val token = PushToken.of("new_token")
        every { pushTokenFetcher.fetch() } returns token
        repository.putString("fcm_token", "old_token")

        sut.initialize()

        // then
        expectThat(sut.currentPushToken).isEqualTo(token)
        expectThat(repository.getString("fcm_token")).isEqualTo("new_token")
    }

    @Test
    fun `when session started then track push token event`() {
        // given
        repository.putString("fcm_token", "token")
        val user = User.builder().deviceId("device").build()

        // when
        sut.onSessionStarted(Session.create(42), user, 42)

        // then
        verify(exactly = 1) {
            pushEventTracker.trackToken(PushToken.of("token"), user, 42)
        }
    }
}
