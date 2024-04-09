package io.hackle.android.internal.utils.concurrent

import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class ThrottlerTest {


    @Test
    fun `when cannot acquire then execute reject`() {
        // given
        val limiter = mockk<ThrottleLimiter>()
        every { limiter.tryAcquire() } returns false
        val sut = Throttler(limiter)

        var accept = 0
        var reject = 0

        // when
        sut.execute(accept = { accept++ }, reject = { reject++ })

        // then
        expectThat(accept) isEqualTo 0
        expectThat(reject) isEqualTo 1
    }

    @Test
    fun `when acquire then exectue accept`() {
        // given
        val limiter = mockk<ThrottleLimiter>()
        every { limiter.tryAcquire() } returns true
        val sut = Throttler(limiter)

        var accept = 0
        var reject = 0

        // when
        sut.execute(accept = { accept++ }, reject = { reject++ })

        // then
        expectThat(accept) isEqualTo 1
        expectThat(reject) isEqualTo 0
    }
}
