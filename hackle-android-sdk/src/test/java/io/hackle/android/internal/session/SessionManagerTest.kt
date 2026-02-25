package io.hackle.android.internal.session

import io.hackle.android.internal.database.repository.KeyValueRepository
import io.hackle.android.internal.database.repository.MapKeyValueRepository
import io.hackle.android.internal.platform.packageinfo.PackageVersionInfo
import io.hackle.android.internal.user.UserManager
import io.hackle.android.mock.MockDevice
import io.hackle.android.mock.MockPackageInfo
import io.hackle.sdk.common.User
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.*

class SessionManagerTest {

    private fun manager(
        sessionTimeoutMillis: Long = 10000,
        repository: KeyValueRepository = MapKeyValueRepository(),
        vararg listeners: SessionListener,
    ): SessionManager {
        return SessionManager(
            userManager = UserManager(
                MockDevice("test_id", emptyMap()),
                MockPackageInfo(PackageVersionInfo("1.0.0", 1L)),
                MapKeyValueRepository(),
                mockk(),
                mockk()
            ),
            keyValueRepository = repository,
            sessionTimeoutMillis = sessionTimeoutMillis,
        ).also { listeners.forEach(it::addListener) }
    }

    @Test
    fun `initialize`() {
        // given
        val repository = MapKeyValueRepository(mutableMapOf(
            "session_id" to "42.ffffffff",
            "last_event_time" to 42L
        ))
        val sut = manager(repository = repository)

        // when
        sut.initialize()

        // then
        expectThat(sut.currentSession)
            .isNotNull()
            .get { id } isEqualTo "42.ffffffff"
        expectThat(sut.lastEventTime)
            .isNotNull()
            .isEqualTo(42)
    }

    @Test
    fun `initialize - empty repository`() {
        // given
        val repository = MapKeyValueRepository()
        val sut = manager(repository = repository)

        // when
        sut.initialize()

        // then
        expectThat(sut.currentSession).isNull()
        expectThat(sut.lastEventTime).isNull()
    }

    @Test
    fun `startNewSession`() {
        val repository = MapKeyValueRepository()
        val listener = SessionListenerStub()
        val sut = manager(10000, repository, listener)

        expectThat(sut.currentSession).isNull()
        expectThat(sut.lastEventTime).isNull()

        val user1 = User.of("user1")
        val session1 = sut.startNewSession(user1, 42)

        expectThat(session1.id).startsWith("42.")
        expectThat(sut.currentSession).isEqualTo(session1)
        expectThat(sut.lastEventTime).isEqualTo(42)
        expectThat(listener.ended).hasSize(0)
        expectThat(listener.started) {
            hasSize(1)
            get(0).isEqualTo(Triple(session1, user1, 42L))
        }
        expectThat(repository.getString("session_id")).isEqualTo(session1.id)
        expectThat(repository.getLong("last_event_time", -1)).isEqualTo(42)


        val user2 = User.of("user2")
        val session2 = sut.startNewSession(user2, 43)

        expectThat(session2.id).startsWith("43.")
        expectThat(sut.currentSession).isEqualTo(session2)
        expectThat(sut.lastEventTime).isEqualTo(43)
        expectThat(listener.ended) {
            hasSize(1)
            get(0).isEqualTo(Triple(session1, user2, 42L))
        }
        expectThat(listener.started) {
            hasSize(2)
            get(1).isEqualTo(Triple(session2, user2, 43L))
        }
        expectThat(repository.getString("session_id")).isEqualTo(session2.id)
        expectThat(repository.getLong("last_event_time", -1)).isEqualTo(43)
    }

    @Test
    fun `startNewSession - oldUser and newUser are distinguished`() {
        val listener = SessionListenerStub()
        val sut = manager(listeners = *arrayOf(listener))
        val oldUser = User.builder().userId("A").deviceId("d1").build()
        val newUser = User.builder().userId("B").deviceId("d1").build()

        sut.startNewSession(oldUser, 100)
        listener.clear()

        val session = sut.startNewSession(oldUser, newUser, 200)

        expectThat(session.id).startsWith("200.")
        expectThat(listener.ended).hasSize(1)
        expectThat(listener.ended[0].second) isEqualTo oldUser
        expectThat(listener.started).hasSize(1)
        expectThat(listener.started[0].second) isEqualTo newUser
    }

    @Test
    fun `startNewSessionIfNeeded - lastEventTime 이 없으면 세션을 시작한다`() {
        // given
        val repository = MapKeyValueRepository()
        val listener = SessionListenerStub()
        val sut = manager(10000, repository, listener)

        val user = User.of("hello")

        // when
        val actual = sut.startNewSessionIfNeeded(user, user, 42)

        // then
        expectThat(actual.id).startsWith("42.")
        expectThat(sut.lastEventTime) isEqualTo 42
    }

    @Test
    fun `startNewSessionIfNeeded - 세션 만료전이면 기존 세션을 리턴한다`() {
        val repository = MapKeyValueRepository()
        val listener = SessionListenerStub()
        val sut = manager(10, repository, listener)

        val user = User.of("hello")

        val s1 = sut.startNewSession(user, 42)
        val s2 = sut.startNewSessionIfNeeded(user, user, 51)

        expectThat(s1) isSameInstanceAs s2
        expectThat(sut.lastEventTime) isEqualTo 51
    }

    @Test
    fun `startNewSessionIfNeeded - 세션이 만료됐으면 새로운 세션을 시작한다`() {
        val repository = MapKeyValueRepository()
        val listener = SessionListenerStub()
        val sut = manager(10, repository, listener)

        val user = User.of("hello")

        val s1 = sut.startNewSession(user, 42)
        val s2 = sut.startNewSessionIfNeeded(user, user, 52)

        expectThat(s1) isNotEqualTo s2
        expectThat(sut.lastEventTime) isEqualTo 52
        expectThat(listener.started) {
            hasSize(2)
            get { first() } isEqualTo Triple(s1, user, 42L)
            get { last() } isEqualTo Triple(s2, user, 52L)
        }
        expectThat(listener.ended) {
            hasSize(1)
            get { first() } isEqualTo Triple(s1, user, 42L)
        }
    }

    @Test
    fun `startNewSessionIfNeeded - no identifier change keeps session`() {
        val sut = manager()
        val oldUser = User.builder().userId("A").deviceId("d1").build()
        val newUser = User.builder().userId("A").deviceId("d1").build()

        val session1 = sut.startNewSession(oldUser, 100)
        val session2 = sut.startNewSessionIfNeeded(oldUser, newUser, 200)

        expectThat(session1.id) isEqualTo session2.id
    }

    @Test
    fun `updateLastEventTime`() {
        val repository = MapKeyValueRepository()
        val listener = SessionListenerStub()
        val sut = manager(10, repository, listener)

        expectThat(sut.lastEventTime).isNull()
        sut.updateLastEventTime(42)
        expectThat(sut.lastEventTime) isEqualTo 42
        expectThat(repository.getLong("last_event_time", -1)) isEqualTo 42
    }

    @Test
    fun `onForeground - 세션 초기화 시도`() {
        // given
        val repository = MapKeyValueRepository()
        val listener = SessionListenerStub()
        val manager = manager(10000, repository, listener)

        val sut = spyk(manager)

        // when
        sut.onForeground(42, false)

        // then
        verify(exactly = 1) { sut.startNewSessionIfNeeded(any(), any(), 42) }
    }

    @Test
    fun `onBackground - 현재 세션을 저장한다`() {
        // given
        val repository = MapKeyValueRepository()
        val listener = SessionListenerStub()
        val sut = manager(10000, repository, listener)
        val session = sut.startNewSession(User.of("hello"), 42)

        // when
        sut.onBackground(320)

        // then
        expectThat(repository.getString("session_id")) isEqualTo session.id
    }

    @Test
    fun `onBackground - 전달받은 timestamp 로 업데이트한다`() {
        val repository = MapKeyValueRepository()
        val listener = SessionListenerStub()
        val sut = manager(10000, repository, listener)
        expectThat(sut.lastEventTime).isNull()
        sut.onBackground(42)
        expectThat(sut.lastEventTime) isEqualTo 42
        expectThat(repository.getLong("last_event_time", -1)) isEqualTo 42L
    }

    private class SessionListenerStub : SessionListener {

        val started = mutableListOf<Triple<Session, User, Long>>()
        val ended = mutableListOf<Triple<Session, User, Long>>()

        override fun onSessionStarted(session: Session, user: User, timestamp: Long) {
            started += Triple(session, user, timestamp)
        }

        override fun onSessionEnded(session: Session, user: User, timestamp: Long) {
            ended += Triple(session, user, timestamp)
        }

        fun clear() {
            started.clear()
            ended.clear()
        }
    }
}
