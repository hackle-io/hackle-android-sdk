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
    fun `exposureEventDedupIntervalMillis`() {
        configTest(60000, HackleConfig::exposureEventDedupIntervalMillis)
        configTest(60000, HackleConfig::exposureEventDedupIntervalMillis) {
            exposureEventDedupIntervalMillis(999)
        }

        configTest(60000, HackleConfig::exposureEventDedupIntervalMillis) {
            exposureEventDedupIntervalMillis(3600001)
        }

        for (i in 1000..3600000) {
            configTest(i, HackleConfig::exposureEventDedupIntervalMillis) {
                exposureEventDedupIntervalMillis(i)
            }
        }
    }

    @Test
    fun `eventFlushIntervalMillis`() {
        configTest(10000, HackleConfig::eventFlushIntervalMillis)
        configTest(10000, HackleConfig::eventFlushIntervalMillis) {
            eventFlushIntervalMillis(999)
        }
        configTest(10000, HackleConfig::eventFlushIntervalMillis) {
            eventFlushIntervalMillis(60001)
        }
        for (i in 1000..60000) {
            configTest(i, HackleConfig::eventFlushIntervalMillis) {
                eventFlushIntervalMillis(i)
            }
        }
    }

    @Test
    fun `eventFlushThreshold`() {
        configTest(10, HackleConfig::eventFlushThreshold)
        configTest(10, HackleConfig::eventFlushThreshold) {
            eventFlushThreshold(4)
        }
        configTest(10, HackleConfig::eventFlushThreshold) {
            eventFlushThreshold(31)
        }
        for (i in 5..30) {
            configTest(i, HackleConfig::eventFlushThreshold) {
                eventFlushThreshold(i)
            }
        }
    }

    private fun <T> configTest(
        expected: T,
        actual: (HackleConfig) -> T,
        doConfig: HackleConfig.Builder.() -> Unit = {},
    ) {
        val config = HackleConfig.builder().apply { doConfig() }.build()
        assertEquals(expected, actual(config))
    }
}