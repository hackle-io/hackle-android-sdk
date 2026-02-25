//package io.hackle.android.internal.session
//
//import io.hackle.android.internal.database.KeyValueRepository
//import io.hackle.android.internal.database.repository.MapKeyValueRepository
//import io.hackle.android.internal.lifecycle.AppState
//import io.hackle.android.internal.model.Device
//import io.hackle.android.internal.user.UserManager
//import io.hackle.android.mock.MockDevice
//import io.hackle.sdk.common.User
//import io.mockk.spyk
//import io.mockk.verify
//import org.junit.Test
//import strikt.api.expectThat
//import strikt.assertions.*
//
//class SessionManagerTest {
//
//    private fun manager(
//        sessionTimeoutMillis: Long = 10000,
//        repository: KeyValueRepository = MapKeyValueRepository(),
//        vararg listeners: SessionListener,
//    ): SessionManager {
//        return SessionManager(
//            userManager = UserManager(MockDevice("test_id", emptyMap()), MapKeyValueRepository()),
//            keyValueRepository = repository,
//            sessionTimeoutMillis = sessionTimeoutMillis
//        ).also { listeners.forEach(it::addListener) }
//    }
//
//    @Test
//    fun `initialize`() {
//        // given
//        val repository = MapKeyValueRepository(mutableMapOf(
//            "session_id" to "42.ffffffff",
//            "last_event_time" to 42L
//        ))
//        val sut = manager(repository = repository)
//
//        // when
//        sut.initialize()
//
//        // then
//        expectThat(sut.currentSession)
//            .isNotNull()
//            .get { id } isEqualTo "42.ffffffff"
//        expectThat(sut.lastEventTime)
//            .isNotNull()
//            .isEqualTo(42)
//    }
//
//    @Test
//    fun `initialize2`() {
//        // given
//        val repository = MapKeyValueRepository()
//        val sut = manager(repository = repository)
//
//        // when
//        sut.initialize()
//
//        // then
//        expectThat(sut.currentSession).isNull()
//        expectThat(sut.lastEventTime).isNull()
//    }
//
//    @Test
//    fun `startNewSession`() {
//        val repository = MapKeyValueRepository()
//        val listener = SessionListenerStub()
//        val sut = manager(10000, repository, listener)
//
//        expectThat(sut.currentSession).isNull()
//        expectThat(sut.lastEventTime).isNull()
//
//        val user1 = User.of("user1")
//        val session1 = sut.startNewSession(user1, 42)
//
//        expectThat(session1.id).startsWith("42.")
//        expectThat(sut.currentSession).isEqualTo(session1)
//        expectThat(sut.lastEventTime).isEqualTo(42)
//        expectThat(listener.ended).hasSize(0)
//        expectThat(listener.started) {
//            hasSize(1)
//            get(0).isEqualTo(Triple(session1, user1, 42))
//        }
//        expectThat(repository.getString("session_id")).isEqualTo(session1.id)
//        expectThat(repository.getLong("last_event_time", -1)).isEqualTo(42)
//
//
//        val user2 = User.of("user2")
//        val session2 = sut.startNewSession(user2, 43)
//
//        expectThat(session2.id).startsWith("43.")
//        expectThat(sut.currentSession).isEqualTo(session2)
//        expectThat(sut.lastEventTime).isEqualTo(43)
//        expectThat(listener.ended) {
//            hasSize(1)
//            get(0).isEqualTo(Triple(session1, user2, 42))
//        }
//        expectThat(listener.started) {
//            hasSize(2)
//            get(1).isEqualTo(Triple(session2, user2, 43))
//        }
//        expectThat(repository.getString("session_id")).isEqualTo(session2.id)
//        expectThat(repository.getLong("last_event_time", -1)).isEqualTo(43)
//    }
//
//    @Test
//    fun `startNewSessionIfNeeded - lastEventTime 이 없으면 세션을 시작한다`() {
//        // given
//        val repository = MapKeyValueRepository()
//        val listener = SessionListenerStub()
//        val sut = manager(10000, repository, listener)
//
//        // when
//        val actual = sut.startNewSessionIfNeeded(User.of("hello"), 42)
//
//        // then
//        expectThat(actual.id).startsWith("42.")
//        expectThat(sut.lastEventTime) isEqualTo 42
//    }
//
//    @Test
//    fun `startNewSessionIfNeeded - 세션 만료전이면 기존 세션을 리턴한다`() {
//        val repository = MapKeyValueRepository()
//        val listener = SessionListenerStub()
//        val sut = manager(10, repository, listener)
//
//        val user = User.of("hello")
//
//        val s1 = sut.startNewSession(user, 42)
//        val s2 = sut.startNewSessionIfNeeded(user, 51)
//
//        expectThat(s1) isSameInstanceAs s2
//        expectThat(sut.lastEventTime) isEqualTo 51
//    }
//
//    @Test
//    fun `startNewSessionIfNeeded - 세션이 만료됐으면 새로운 세션을 시작한다`() {
//        val repository = MapKeyValueRepository()
//        val listener = SessionListenerStub()
//        val sut = manager(10, repository, listener)
//
//        val user = User.of("hello")
//
//        val s1 = sut.startNewSession(user, 42)
//        val s2 = sut.startNewSessionIfNeeded(user, 52)
//
//        expectThat(s1) isNotEqualTo s2
//        expectThat(sut.lastEventTime) isEqualTo 52
//        expectThat(listener.started) {
//            hasSize(2)
//            get { first() } isEqualTo Triple(s1, user, 42)
//            get { last() } isEqualTo Triple(s2, user, 52)
//        }
//        expectThat(listener.ended) {
//            hasSize(1)
//            get { first() } isEqualTo Triple(s1, user, 42)
//        }
//    }
//
//    @Test
//    fun `updateLastEventTime`() {
//        val repository = MapKeyValueRepository()
//        val listener = SessionListenerStub()
//        val sut = manager(10, repository, listener)
//
//        expectThat(sut.lastEventTime).isNull()
//        sut.updateLastEventTime(42)
//        expectThat(sut.lastEventTime) isEqualTo 42
//        expectThat(repository.getLong("last_event_time", -1)) isEqualTo 42
//    }
//
//    @Test
//    fun `onChanged - FOREGROUND 세션 초기화 시도`() {
//        // given
//        val repository = MapKeyValueRepository()
//        val listener = SessionListenerStub()
//        val manager = manager(10000, repository, listener)
//
//        val sut = spyk(manager)
//
//        // when
//        sut.onChanged(AppState.FOREGROUND, 42)
//
//        // then
//        verify(exactly = 1) { sut.startNewSessionIfNeeded(any(), 42) }
//    }
//
//    @Test
//    fun `onChanged - BACKGROUND 현재 세션을 저장한다`() {
//        // given
//        val repository = MapKeyValueRepository()
//        val listener = SessionListenerStub()
//        val sut = manager(10000, repository, listener)
//        val session = sut.startNewSession(User.of("hello"), 42)
//
//        // when
//        sut.onChanged(AppState.BACKGROUND, 320)
//
//        // then
//        expectThat(repository.getString("session_id")) isEqualTo session.id
//
//    }
//
//    @Test
//    fun `onChanged - 백그라운드로 넘어기면 전달받은 timestamp 로 업데이트한다`() {
//        val repository = MapKeyValueRepository()
//        val listener = SessionListenerStub()
//        val sut = manager(10000, repository, listener)
//        expectThat(sut.lastEventTime).isNull()
//        sut.onChanged(AppState.BACKGROUND, 42)
//        expectThat(sut.lastEventTime) isEqualTo 42
//        expectThat(repository.getLong("last_event_time", -1)) isEqualTo 42L
//    }
//
//    private class SessionListenerStub : SessionListener {
//
//        val started = mutableListOf<Triple<Session, User, Long>>()
//        val ended = mutableListOf<Triple<Session, User, Long>>()
//        override fun onSessionStarted(session: Session, user: User, timestamp: Long) {
//            started += Triple(session, user, timestamp)
//        }
//
//        override fun onSessionEnded(session: Session, user: User, timestamp: Long) {
//            ended += Triple(session, user, timestamp)
//        }
//    }
//}

package io.hackle.android.internal.session

import io.hackle.android.internal.database.repository.KeyValueRepository
import io.hackle.android.internal.database.repository.MapKeyValueRepository
import io.hackle.android.internal.platform.packageinfo.PackageVersionInfo
import io.hackle.android.internal.user.UserManager
import io.hackle.android.mock.MockDevice
import io.hackle.android.mock.MockPackageInfo
import io.hackle.sdk.common.HackleSessionExpiry
import io.hackle.sdk.common.HackleSessionPolicy
import io.hackle.sdk.common.User
import io.mockk.mockk
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEqualTo

class SessionManagerSessionPolicyTest {

    private fun manager(
        sessionTimeoutMillis: Long = 10000,
        repository: KeyValueRepository = MapKeyValueRepository(),
        sessionPolicy: HackleSessionPolicy = HackleSessionPolicy.DEFAULT,
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
            sessionPolicy = sessionPolicy,
        ).also { listeners.forEach(it::addListener) }
    }

    @Test
    fun `DEFAULT policy - userId change triggers new session`() {
        val sut = manager()
        val oldUser = User.builder().userId("A").deviceId("d1").build()
        val newUser = User.builder().userId("B").deviceId("d1").build()

        val session1 = sut.startNewSession(oldUser, 100)
        val session2 = sut.startNewSessionIfNeeded(oldUser, newUser, 200)

        expectThat(session1.id) isNotEqualTo session2.id
    }

    @Test
    fun `DEFAULT policy - null to userId triggers new session`() {
        val sut = manager()
        val oldUser = User.builder().deviceId("d1").build()
        val newUser = User.builder().userId("A").deviceId("d1").build()

        val session1 = sut.startNewSession(oldUser, 100)
        val session2 = sut.startNewSessionIfNeeded(oldUser, newUser, 200)

        expectThat(session1.id) isNotEqualTo session2.id
    }

    @Test
    fun `DEFAULT policy - userId to null triggers new session`() {
        val sut = manager()
        val oldUser = User.builder().userId("A").deviceId("d1").build()
        val newUser = User.builder().deviceId("d1").build()

        val session1 = sut.startNewSession(oldUser, 100)
        val session2 = sut.startNewSessionIfNeeded(oldUser, newUser, 200)

        expectThat(session1.id) isNotEqualTo session2.id
    }

    @Test
    fun `DEFAULT policy - deviceId change triggers new session`() {
        val sut = manager()
        val oldUser = User.builder().deviceId("d1").build()
        val newUser = User.builder().deviceId("d2").build()

        val session1 = sut.startNewSession(oldUser, 100)
        val session2 = sut.startNewSessionIfNeeded(oldUser, newUser, 200)

        expectThat(session1.id) isNotEqualTo session2.id
    }

    @Test
    fun `custom policy - exclude NULL_TO_USER_ID keeps session on login`() {
        val policy = HackleSessionPolicy.builder()
            .expiredPolicy(
                HackleSessionExpiry.USER_ID_CHANGE,
                HackleSessionExpiry.DEVICE_ID_CHANGE,
                HackleSessionExpiry.USER_ID_TO_NULL
            )
            .build()
        val sut = manager(sessionPolicy = policy)
        val oldUser = User.builder().deviceId("d1").build()
        val newUser = User.builder().userId("A").deviceId("d1").build()

        val session1 = sut.startNewSession(oldUser, 100)
        val session2 = sut.startNewSessionIfNeeded(oldUser, newUser, 200)

        expectThat(session1.id) isEqualTo session2.id
    }

    @Test
    fun `custom policy - exclude USER_ID_TO_NULL keeps session on logout`() {
        val policy = HackleSessionPolicy.builder()
            .expiredPolicy(
                HackleSessionExpiry.USER_ID_CHANGE,
                HackleSessionExpiry.DEVICE_ID_CHANGE,
                HackleSessionExpiry.NULL_TO_USER_ID
            )
            .build()
        val sut = manager(sessionPolicy = policy)
        val oldUser = User.builder().userId("A").deviceId("d1").build()
        val newUser = User.builder().deviceId("d1").build()

        val session1 = sut.startNewSession(oldUser, 100)
        val session2 = sut.startNewSessionIfNeeded(oldUser, newUser, 200)

        expectThat(session1.id) isEqualTo session2.id
    }

    @Test
    fun `empty policy - all changes keep session`() {
        val policy = HackleSessionPolicy.builder()
            .expiredPolicy()
            .build()
        val sut = manager(sessionPolicy = policy)
        val oldUser = User.builder().userId("A").deviceId("d1").build()
        val newUser = User.builder().userId("B").deviceId("d2").build()

        val session1 = sut.startNewSession(oldUser, 100)
        val session2 = sut.startNewSessionIfNeeded(oldUser, newUser, 200)

        expectThat(session1.id) isEqualTo session2.id
    }

    @Test
    fun `compound change - OR logic triggers session when any condition matches`() {
        val policy = HackleSessionPolicy.builder()
            .expiredPolicy(HackleSessionExpiry.DEVICE_ID_CHANGE)
            .build()
        val sut = manager(sessionPolicy = policy)

        // userId change + deviceId change, but only DEVICE_ID_CHANGE in policy
        val oldUser = User.builder().userId("A").deviceId("d1").build()
        val newUser = User.builder().userId("B").deviceId("d2").build()

        val session1 = sut.startNewSession(oldUser, 100)
        val session2 = sut.startNewSessionIfNeeded(oldUser, newUser, 200)

        expectThat(session1.id) isNotEqualTo session2.id
    }

    @Test
    fun `compound change - no matching condition keeps session`() {
        val policy = HackleSessionPolicy.builder()
            .expiredPolicy(HackleSessionExpiry.DEVICE_ID_CHANGE)
            .build()
        val sut = manager(sessionPolicy = policy)

        // only userId change, but DEVICE_ID_CHANGE in policy
        val oldUser = User.builder().userId("A").deviceId("d1").build()
        val newUser = User.builder().userId("B").deviceId("d1").build()

        val session1 = sut.startNewSession(oldUser, 100)
        val session2 = sut.startNewSessionIfNeeded(oldUser, newUser, 200)

        expectThat(session1.id) isEqualTo session2.id
    }

    @Test
    fun `no identifier change - session is maintained`() {
        val sut = manager()
        val oldUser = User.builder().userId("A").deviceId("d1").build()
        val newUser = User.builder().userId("A").deviceId("d1").build()

        val session1 = sut.startNewSession(oldUser, 100)
        val session2 = sut.startNewSessionIfNeeded(oldUser, newUser, 200)

        expectThat(session1.id) isEqualTo session2.id
    }
}
