package io.hackle.android.internal.session

import io.hackle.android.internal.application.lifecycle.ApplicationLifecycleManager
import io.hackle.android.internal.application.lifecycle.ApplicationState
import io.hackle.android.internal.database.repository.KeyValueRepository
import io.hackle.android.internal.database.repository.MapKeyValueRepository
import io.hackle.android.internal.platform.packageinfo.PackageVersionInfo
import io.hackle.android.internal.user.UserManager
import io.hackle.android.mock.MockDevice
import io.hackle.android.mock.MockPackageInfo
import io.hackle.sdk.common.HackleSessionPolicy
import io.hackle.sdk.common.HackleSessionPersistCondition
import io.hackle.sdk.common.User
import io.mockk.every
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
        sessionPolicy: HackleSessionPolicy? = null,
        applicationLifecycleManager: ApplicationLifecycleManager = mockk {
            every { currentState } returns ApplicationState.FOREGROUND
        },
        vararg listeners: SessionListener,
    ): SessionManager {
        val policy = sessionPolicy ?: HackleSessionPolicy.builder()
            .timeoutMillis(sessionTimeoutMillis)
            .build()
        return SessionManager(
            userManager = UserManager(
                MockDevice("test_id", emptyMap()),
                MockPackageInfo(PackageVersionInfo("1.0.0", 1L)),
                MapKeyValueRepository(),
                mockk(),
                mockk()
            ),
            keyValueRepository = repository,
            applicationLifecycleManager = applicationLifecycleManager,
            sessionPolicy = policy,
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
        val sut = manager(sessionTimeoutMillis = 10000, repository = repository, listeners = *arrayOf(listener))

        expectThat(sut.currentSession).isNull()
        expectThat(sut.lastEventTime).isNull()

        val user1 = User.of("user1")
        val session1 = sut.startNewSession(user1, user1, 42)

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
        val session2 = sut.startNewSession(user2, user2, 43)

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

        sut.startNewSession(oldUser, oldUser, 100)
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
        val sut = manager(sessionTimeoutMillis = 10000, repository = repository, listeners = *arrayOf(listener))

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
        val sut = manager(sessionTimeoutMillis = 10, repository = repository, listeners = *arrayOf(listener))

        val user = User.of("hello")

        val s1 = sut.startNewSession(user, user, 42)
        val s2 = sut.startNewSessionIfNeeded(user, user, 51)

        expectThat(s1) isSameInstanceAs s2
        expectThat(sut.lastEventTime) isEqualTo 51
    }

    @Test
    fun `startNewSessionIfNeeded - 세션이 만료됐으면 새로운 세션을 시작한다`() {
        val repository = MapKeyValueRepository()
        val listener = SessionListenerStub()
        val sut = manager(sessionTimeoutMillis = 10, repository = repository, listeners = *arrayOf(listener))

        val user = User.of("hello")

        val s1 = sut.startNewSession(user, user, 42)
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

        val session1 = sut.startNewSession(oldUser, oldUser, 100)
        val session2 = sut.startNewSessionIfNeeded(oldUser, newUser, 200)

        expectThat(session1.id) isEqualTo session2.id
    }

    @Test
    fun `onUserUpdated - default policy expires session on identifier change`() {
        val listener = SessionListenerStub()
        val sut = manager(listeners = *arrayOf(listener))
        val oldUser = User.builder().userId("A").deviceId("d1").build()
        val newUser = User.builder().userId("B").deviceId("d1").build()

        sut.startNewSession(oldUser, oldUser, 100)
        listener.clear()

        sut.onUserUpdated(oldUser, newUser, 200)

        expectThat(listener.ended).hasSize(1)
        expectThat(listener.ended[0].second) isEqualTo oldUser
        expectThat(listener.started).hasSize(1)
        expectThat(listener.started[0].second) isEqualTo newUser
    }

    @Test
    fun `onUserUpdated - custom policy preserves session on null to userId change`() {
        val policy = HackleSessionPolicy.builder()
            .persistCondition(HackleSessionPersistCondition.NULL_TO_USER_ID)
            .build()
        val listener = SessionListenerStub()
        val sut = manager(sessionPolicy = policy, listeners = *arrayOf(listener))
        val oldUser = User.builder().deviceId("d1").build()
        val newUser = User.builder().userId("A").deviceId("d1").build()

        sut.startNewSession(oldUser, oldUser, 100)
        listener.clear()

        sut.onUserUpdated(oldUser, newUser, 200)

        expectThat(listener.ended).hasSize(0)
        expectThat(listener.started).hasSize(0)
    }

    @Test
    fun `onUserUpdated - same identifiers keeps session`() {
        val listener = SessionListenerStub()
        val sut = manager(listeners = *arrayOf(listener))
        val user = User.builder().userId("A").deviceId("d1").build()
        val sameUser = User.builder().userId("A").deviceId("d1").build()

        sut.startNewSession(user, user, 100)
        listener.clear()

        sut.onUserUpdated(user, sameUser, 200)

        expectThat(listener.ended).hasSize(0)
        expectThat(listener.started).hasSize(0)
    }

    @Test
    fun `onUserUpdated - policy preserves but timeout expired starts new session`() {
        val policy = HackleSessionPolicy.builder()
            .persistCondition { _, _ -> true }
            .timeoutMillis(100)
            .build()
        val listener = SessionListenerStub()
        val sut = manager(sessionPolicy = policy, listeners = *arrayOf(listener))
        val oldUser = User.builder().userId("A").deviceId("d1").build()
        val newUser = User.builder().userId("B").deviceId("d1").build()

        sut.startNewSession(oldUser, oldUser, 100)
        listener.clear()

        sut.onUserUpdated(oldUser, newUser, 300)

        expectThat(listener.ended).hasSize(1)
        expectThat(listener.started).hasSize(1)
    }

    @Test
    fun `updateLastEventTime`() {
        val repository = MapKeyValueRepository()
        val listener = SessionListenerStub()
        val sut = manager(sessionTimeoutMillis = 10, repository = repository, listeners = *arrayOf(listener))

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
        val manager = manager(sessionTimeoutMillis = 10000, repository = repository, listeners = *arrayOf(listener))

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
        val sut = manager(sessionTimeoutMillis = 10000, repository = repository, listeners = *arrayOf(listener))
        val user = User.of("hello")
        val session = sut.startNewSession(user, user, 42)

        // when
        sut.onBackground(320)

        // then
        expectThat(repository.getString("session_id")) isEqualTo session.id
    }

    @Test
    fun `onBackground - 전달받은 timestamp 로 업데이트한다`() {
        val repository = MapKeyValueRepository()
        val listener = SessionListenerStub()
        val sut = manager(sessionTimeoutMillis = 10000, repository = repository, listeners = *arrayOf(listener))
        expectThat(sut.lastEventTime).isNull()
        sut.onBackground(42)
        expectThat(sut.lastEventTime) isEqualTo 42
        expectThat(repository.getLong("last_event_time", -1)) isEqualTo 42L
    }

    @Test
    fun `startNewSessionIfNeeded - policy timeout 사용하여 세션 만료 확인`() {
        val policy = HackleSessionPolicy.builder()
            .timeoutMillis(10)
            .build()
        val sut = manager(sessionPolicy = policy)

        val user = User.of("hello")
        val s1 = sut.startNewSession(user, user, 42)
        val s2 = sut.startNewSessionIfNeeded(user, user, 52)

        expectThat(s1) isNotEqualTo s2
    }

    @Test
    fun `startNewSessionIfNeeded - policy timeout 사용하여 세션 유지 확인`() {
        val policy = HackleSessionPolicy.builder()
            .timeoutMillis(100)
            .build()
        val sut = manager(sessionPolicy = policy)

        val user = User.of("hello")
        val s1 = sut.startNewSession(user, user, 42)
        val s2 = sut.startNewSessionIfNeeded(user, user, 51)

        expectThat(s1) isSameInstanceAs s2
    }

    // === expireOnBackground = true (기본값) ===

    @Test
    fun `startNewSessionIfNeeded with isBackground - foreground 이벤트는 lastEventTime만 갱신한다`() {
        val listener = SessionListenerStub()
        val sut = manager(sessionTimeoutMillis = 10, listeners = *arrayOf(listener))
        val user = User.of("hello")

        sut.startNewSession(user, user, 100)
        listener.clear()

        sut.startNewSessionIfNeededOnEvent(user, 200)

        expectThat(sut.lastEventTime) isEqualTo 200
        expectThat(listener.started).hasSize(0)
        expectThat(listener.ended).hasSize(0)
    }

    @Test
    fun `startNewSessionIfNeeded with isBackground - background 이벤트 + expireOnBackground true + 세션 만료 시 새로운 세션 시작`() {
        val policy = HackleSessionPolicy.builder()
            .timeoutMillis(10)
            .expireOnBackground(true)
            .build()
        val backgroundMock = mockk<ApplicationLifecycleManager> {
            every { currentState } returns ApplicationState.BACKGROUND
        }
        val listener = SessionListenerStub()
        val sut = manager(sessionPolicy = policy, applicationLifecycleManager = backgroundMock, listeners = *arrayOf(listener))
        val user = User.of("hello")

        sut.startNewSession(user, user, 100)
        listener.clear()

        sut.startNewSessionIfNeededOnEvent(user, 200)

        expectThat(listener.started).hasSize(1)
        expectThat(listener.ended).hasSize(1)
    }

    @Test
    fun `startNewSessionIfNeeded with isBackground - background 이벤트 + expireOnBackground true + 세션 미만료 시 세션 유지`() {
        val policy = HackleSessionPolicy.builder()
            .timeoutMillis(1000)
            .expireOnBackground(true)
            .build()
        val backgroundMock = mockk<ApplicationLifecycleManager> {
            every { currentState } returns ApplicationState.BACKGROUND
        }
        val listener = SessionListenerStub()
        val sut = manager(sessionPolicy = policy, applicationLifecycleManager = backgroundMock, listeners = *arrayOf(listener))
        val user = User.of("hello")

        sut.startNewSession(user, user, 100)
        listener.clear()

        sut.startNewSessionIfNeededOnEvent(user, 200)

        expectThat(listener.started).hasSize(0)
        expectThat(listener.ended).hasSize(0)
        expectThat(sut.lastEventTime) isEqualTo 200
    }

    // === expireOnBackground = false ===

    @Test
    fun `startNewSessionIfNeeded with isBackground - background 이벤트 + expireOnBackground false + 세션 만료 시에도 세션 유지`() {
        val policy = HackleSessionPolicy.builder()
            .timeoutMillis(10)
            .expireOnBackground(false)
            .build()
        val backgroundMock = mockk<ApplicationLifecycleManager> {
            every { currentState } returns ApplicationState.BACKGROUND
        }
        val listener = SessionListenerStub()
        val sut = manager(sessionPolicy = policy, applicationLifecycleManager = backgroundMock, listeners = *arrayOf(listener))
        val user = User.of("hello")

        sut.startNewSession(user, user, 100)
        listener.clear()

        sut.startNewSessionIfNeededOnEvent(user, 200)

        expectThat(listener.started).hasSize(0)
        expectThat(listener.ended).hasSize(0)
        // lastEventTime은 갱신되지 않아야 함 (포그라운드 전환 시 정확한 timeout 체크를 위해)
        expectThat(sut.lastEventTime) isEqualTo 100
    }

    @Test
    fun `startNewSessionIfNeeded with isBackground - foreground 이벤트는 expireOnBackground 값과 무관하게 lastEventTime 갱신`() {
        val policy = HackleSessionPolicy.builder()
            .timeoutMillis(10)
            .expireOnBackground(false)
            .build()
        val listener = SessionListenerStub()
        val sut = manager(sessionPolicy = policy, listeners = *arrayOf(listener))
        val user = User.of("hello")

        sut.startNewSession(user, user, 100)
        listener.clear()

        sut.startNewSessionIfNeededOnEvent(user, 200)

        expectThat(sut.lastEventTime) isEqualTo 200
    }

    @Test
    fun `onUserUpdated - expireOnBackground false 이어도 식별자 변경 시 세션 재시작`() {
        val policy = HackleSessionPolicy.builder()
            .timeoutMillis(10)
            .expireOnBackground(false)
            .build()
        val listener = SessionListenerStub()
        val sut = manager(sessionPolicy = policy, listeners = *arrayOf(listener))
        val oldUser = User.builder().userId("A").deviceId("d1").build()
        val newUser = User.builder().userId("B").deviceId("d1").build()

        sut.startNewSession(oldUser, oldUser, 100)
        listener.clear()

        sut.onUserUpdated(oldUser, newUser, 200)

        expectThat(listener.ended).hasSize(1)
        expectThat(listener.started).hasSize(1)
    }

    @Test
    fun `onForeground - expireOnBackground false 이어도 포그라운드 전환 시 세션 재시작 가능`() {
        val policy = HackleSessionPolicy.builder()
            .timeoutMillis(10)
            .expireOnBackground(false)
            .build()
        val listener = SessionListenerStub()
        val sut = manager(sessionPolicy = policy, listeners = *arrayOf(listener))
        val user = User.of("hello")

        sut.startNewSession(user, user, 100)
        listener.clear()

        sut.onForeground(200, true)

        expectThat(listener.ended).hasSize(1)
        expectThat(listener.started).hasSize(1)
    }

    // === 통합 시나리오 테스트 ===

    @Test
    fun `expireOnBackground false - 백그라운드 이벤트 후 포그라운드 전환 시 세션 재시작`() {
        val policy = HackleSessionPolicy.builder()
            .timeoutMillis(50)
            .expireOnBackground(false)
            .build()
        val backgroundMock = mockk<ApplicationLifecycleManager> {
            every { currentState } returns ApplicationState.BACKGROUND
        }
        val listener = SessionListenerStub()
        val sut = manager(sessionPolicy = policy, applicationLifecycleManager = backgroundMock, listeners = *arrayOf(listener))
        val user = User.of("hello")

        // 1. 세션 시작
        val session1 = sut.startNewSession(user, user, 100)
        listener.clear()

        // 2. 백그라운드로 전환 (lastEventTime = 100)
        sut.onBackground(100)

        // 3. 백그라운드에서 이벤트 발생 (세션 만료 시간 이후)
        //    expireOnBackground = false이므로 세션 유지
        sut.startNewSessionIfNeededOnEvent(user, 200)
        expectThat(listener.started).hasSize(0)
        expectThat(sut.lastEventTime) isEqualTo 100  // lastEventTime 갱신되지 않음

        // 4. 포그라운드로 전환 → 세션 만료 확인 후 새로운 세션 시작
        sut.onForeground(250, true)
        expectThat(listener.ended).hasSize(1)
        expectThat(listener.started).hasSize(1)
        expectThat(sut.currentSession).isNotNull().isNotEqualTo(session1)
    }

    @Test
    fun `expireOnBackground true - 백그라운드 이벤트로 세션 재시작`() {
        val policy = HackleSessionPolicy.builder()
            .timeoutMillis(50)
            .expireOnBackground(true)
            .build()
        val backgroundMock = mockk<ApplicationLifecycleManager> {
            every { currentState } returns ApplicationState.BACKGROUND
        }
        val listener = SessionListenerStub()
        val sut = manager(sessionPolicy = policy, applicationLifecycleManager = backgroundMock, listeners = *arrayOf(listener))
        val user = User.of("hello")

        // 1. 세션 시작
        val session1 = sut.startNewSession(user, user, 100)
        listener.clear()

        // 2. 백그라운드로 전환
        sut.onBackground(100)

        // 3. 백그라운드에서 이벤트 발생 (세션 만료 시간 이후)
        //    expireOnBackground = true이므로 세션 재시작
        sut.startNewSessionIfNeededOnEvent(user, 200)
        expectThat(listener.ended).hasSize(1)
        expectThat(listener.started).hasSize(1)
        expectThat(sut.currentSession).isNotNull().isNotEqualTo(session1)
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
