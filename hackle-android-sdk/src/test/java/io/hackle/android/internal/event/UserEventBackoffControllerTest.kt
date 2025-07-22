package io.hackle.android.internal.event

import io.hackle.android.internal.time.FixedClock
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isFalse
import strikt.assertions.isTrue
import kotlin.math.pow

class UserEventBackoffControllerTest {

    private lateinit var sut: UserEventBackoffController
    private lateinit var clock: FixedClock
    private val retryInterval = 10_000 // 10 seconds

    @Before
    fun before() {
        clock = FixedClock(0L)
        sut = UserEventBackoffController(retryInterval, clock)
    }

    @Test
    fun `초기에는 flush 가능하다`() {
        expectThat(sut.isAllowNextFlush()).isTrue()
    }

    @Test
    fun `성공한 경우 바로 다음 flush 가능하다`() {
        sut.checkResponse(isSuccess = true)
        expectThat(sut.isAllowNextFlush()).isTrue()
    }

    @Test
    fun `실패한 경우 다음 flush 시점까지 기다려야 한다`() {
        // given
        sut.checkResponse(isSuccess = false)

        // when - check before retry interval
        clock.fastForward(retryInterval - 2_000)
        expectThat(sut.isAllowNextFlush()).isFalse()

        // when - check after retry interval
        clock.fastForward(3_000)
        expectThat(sut.isAllowNextFlush()).isTrue()
    }

    @Test
    fun `연속 실패시 지수백오프로 대기시간이 증가한다`() {
        // when - 1st failure
        sut.checkResponse(isSuccess = false)
        clock.fastForward(interval(1) - 2_000)
        expectThat(sut.isAllowNextFlush()).isFalse()
        clock.fastForward(3_000)
        expectThat(sut.isAllowNextFlush()).isTrue()

        // when - 2nd failure
        sut.checkResponse(isSuccess = false)
        clock.fastForward(interval(2) - 2_000)
        expectThat(sut.isAllowNextFlush()).isFalse()
        clock.fastForward(3_000)
        expectThat(sut.isAllowNextFlush()).isTrue()

        // when - 3rd failure
        sut.checkResponse(isSuccess = false)
        clock.fastForward(interval(3) - 2_000)
        expectThat(sut.isAllowNextFlush()).isFalse()
        clock.fastForward(3_000)
        expectThat(sut.isAllowNextFlush()).isTrue()
    }

    @Test
    fun `성공하면 대기시간이 초기화된다`() {
        // given
        sut.checkResponse(isSuccess = false)
        expectThat(sut.isAllowNextFlush()).isFalse()
        sut.checkResponse(isSuccess = false)
        expectThat(sut.isAllowNextFlush()).isFalse()

        // when
        sut.checkResponse(isSuccess = true)
        // then
        expectThat(sut.isAllowNextFlush()).isTrue()
    }

    private fun interval(failureCount: Int): Int {
        val exponential = 2.0.pow(failureCount.toDouble() - 1).toInt()
        return exponential * retryInterval
    }
}