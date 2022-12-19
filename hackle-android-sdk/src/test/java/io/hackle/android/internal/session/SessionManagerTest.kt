package io.hackle.android.internal.session

import io.hackle.android.internal.database.KeyValueRepository
import io.hackle.android.internal.lifecycle.AppState
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.spyk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.*
import java.util.concurrent.Executor

class SessionManagerTest {

    @RelaxedMockK
    private lateinit var eventExecutor: Executor

    @RelaxedMockK
    private lateinit var keyValueRepository: KeyValueRepository

    private var onSessionStarted: Pair<Session, Long>? = null
    private var onSessionEnded: Pair<Session, Long>? = null

    @Before
    fun before() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        every { eventExecutor.execute(any()) } answers { firstArg<Runnable>().run() }
    }

    private fun manager(sessionTimeoutMillis: Long = 10000): SessionManager {
        val listener = Listener()
        return SessionManager(eventExecutor, keyValueRepository, sessionTimeoutMillis).also {
            it.addListener(listener)
        }
    }


    @Test
    fun `startNewSession `() {
        val sut = manager()

        val s1 = sut.startNewSession(42)
        expectThat(s1.id).startsWith("42.")
        expectThat(onSessionStarted).isNotNull().and {
            get { first } isSameInstanceAs s1
            get { second } isEqualTo 42
        }
        expectThat(onSessionEnded).isNull()
        verify(exactly = 1) {
            keyValueRepository.putString("session_id", any())
        }


        val s2 = sut.startNewSession(43)
        expectThat(s2.id).startsWith("43.")
        expectThat(onSessionStarted).isNotNull().and {
            get { first } isSameInstanceAs s2
            get { second } isEqualTo 43
        }
        expectThat(onSessionEnded).isNotNull().and {
            get { first } isSameInstanceAs s1
            get { second } isEqualTo 42
        }
        verify(exactly = 2) {
            keyValueRepository.putString("session_id", any())
        }
    }

    @Test
    fun `startNewSessionIfNeeded - lastEventTime 이 없으면 세션을 시작한다`() {
        // given
        val sut = manager()

        // when
        val actual = sut.startNewSessionIfNeeded(42)

        // then
        expectThat(actual.id).startsWith("42.")
        expectThat(sut.lastEventTime) isEqualTo 42
    }

    @Test
    fun `startNewSessionIfNeeded - 세션 만료전이면 기존 세션을 리턴한다`() {
        val sut = manager(10)

        val s1 = sut.startNewSession(42)
        val s2 = sut.startNewSessionIfNeeded(51)

        expectThat(s1) isSameInstanceAs s2
        expectThat(sut.lastEventTime) isEqualTo 51
    }

    @Test
    fun `startNewSessionIfNeeded - 세션이 만료됐으면 새로운 세션을 시작한다`() {
        val sut = manager(10)

        val s1 = sut.startNewSession(42)
        val s2 = sut.startNewSessionIfNeeded(52)

        expectThat(s1) isNotEqualTo s2
        expectThat(sut.lastEventTime) isEqualTo 52
        expectThat(onSessionStarted?.first) isEqualTo s2
        expectThat(onSessionEnded?.first) isEqualTo s1
    }

    @Test
    fun `updateLastEventTime`() {
        val sut = manager()
        expectThat(sut.lastEventTime).isNull()
        sut.updateLastEventTime(42)
        expectThat(sut.lastEventTime) isEqualTo 42
        verify(exactly = 1) {
            keyValueRepository.putLong("last_event_time", 42)
        }
    }

    @Test
    fun `onInitialized - 저장된 세션이 없으면 null`() {
        // given
        val sut = manager()
        every { keyValueRepository.getString(any()) } returns null

        // when
        sut.onInitialized()

        // then
        expectThat(sut.currentSession).isNull()
    }

    @Test
    fun `onInitialized - 저장된 세션이 있는경우`() {
        // given
        val sut = manager()
        every { keyValueRepository.getString(any()) } returns "42"

        // when
        sut.onInitialized()

        // then
        expectThat(sut.currentSession).isNotNull()
            .get { id } isEqualTo "42"
    }

    @Test
    fun `onInitialized - 저장되어있는 LastEventTime 이 있으면 가져와서 설정한다`() {
        // given
        val sut = manager()
        every { keyValueRepository.getLong(any(), any()) } returns 42L

        // when
        sut.onInitialized()

        // then
        expectThat(sut.lastEventTime) isEqualTo 42
    }

    @Test
    fun `onInitialized - 저장되어있는 LastEventTime 가 없으면 null`() {
        // given
        val sut = manager()
        every { keyValueRepository.getLong(any(), any()) } returns -1

        // when
        sut.onInitialized()

        // then
        expectThat(sut.lastEventTime).isNull()
    }

    @Test
    fun `onChanged - FOREGROUND 세션 초기화 시도`() {
        // given
        val sut = spyk(manager())

        // when
        sut.onChanged(AppState.FOREGROUND, 42)

        // then
        verify(exactly = 1) { eventExecutor.execute(any()) }
        verify(exactly = 1) { sut.startNewSessionIfNeeded(42) }
    }

    @Test
    fun `onChanged - BACKGROUND 현재 세션을 저장한다`() {
        // given
        val sut = spyk(manager())
        every { sut.currentSession } returns Session("42")

        // when
        sut.onChanged(AppState.BACKGROUND, 320)

        // then
        verify(exactly = 1) {
            keyValueRepository.putString("session_id", "42")
        }
    }

    @Test
    fun `onChanged - 백그라운드로 넘어기면 전달받은 timestamp 로 업데이트한다`() {
        val sut = spyk(manager())
        expectThat(sut.lastEventTime).isNull()
        sut.onChanged(AppState.BACKGROUND, 42)
        expectThat(sut.lastEventTime) isEqualTo 42
        verify(exactly = 1) {
            keyValueRepository.putLong("last_event_time", 42)
        }
    }

    private inner class Listener : SessionListener {
        override fun onSessionStarted(session: Session, timestamp: Long) {
            onSessionStarted = session to timestamp
        }

        override fun onSessionEnded(session: Session, timestamp: Long) {
            onSessionEnded = session to timestamp
        }
    }
}
