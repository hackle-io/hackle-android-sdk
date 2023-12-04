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
