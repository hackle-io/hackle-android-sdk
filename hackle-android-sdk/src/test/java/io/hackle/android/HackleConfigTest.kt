package io.hackle.android

import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class HackleConfigTest {

    @Before
    fun before() {
        mockkStatic(Log::class)
        every { Log.isLoggable(any(), any()) } returns false
    }

    @Test
    fun `exposureEventDedupIntervalMillis 설정하지 않으면 -1`() {
        val config = HackleConfig.builder()
            .build()
        assertEquals(-1, config.exposureEventDedupIntervalMillis)
    }

    @Test
    fun `exposureEventDedupIntervalMillis 100 보다 작은 값으로 설정하면 -1로 설정된다`() {
        val config = HackleConfig.builder()
            .exposureEventDedupIntervalMillis(999)
            .build()
        assertEquals(-1, config.exposureEventDedupIntervalMillis)
    }

    @Test
    fun `exposureEventDedupIntervalMillis 3600000 보다 큰 값으로 설정하면 -1로 설정된다`() {
        val config = HackleConfig.builder()
            .exposureEventDedupIntervalMillis(3600001)
            .build()
        assertEquals(-1, config.exposureEventDedupIntervalMillis)
    }

    @Test
    fun `exposureEventDedupIntervalMillis 1000 ~ 3600000 사이의 값으로 설정해야 된다`() {
        for (i in 1000..3600000) {
            val config = HackleConfig.builder()
                .exposureEventDedupIntervalMillis(i)
                .build()
            assertEquals(i, config.exposureEventDedupIntervalMillis)
        }
    }
}