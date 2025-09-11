package io.hackle.android

import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class HackleConfigTest {

    @Before
    fun before() {
        mockkStatic(Log::class)
        every { Log.isLoggable(any(), any()) } returns false
    }

    @After
    fun after() {
        unmockkStatic(Log::class)
    }

    @Test
    fun `HackleConfig DEFAULT should have expected values`() {
        // when
        val result = HackleConfig.DEFAULT

        // then
        expectThat(result.mode).isEqualTo(HackleAppMode.NATIVE)
        expectThat(result.pollingIntervalMillis).isEqualTo(-1) // NO_POLLING
        expectThat(result.sessionTracking).isEqualTo(true)
        expectThat(result.automaticScreenTracking).isEqualTo(true)
    }

    @Test
    fun `HackleConfig builder should create config with custom values`() {
        // when
        val result = HackleConfig.builder()
            .mode(HackleAppMode.WEB_VIEW_WRAPPER)
            .pollingIntervalMillis(120000) // 2 minutes (above minimum)
            .eventFlushThreshold(20)
            .build()

        // then
        expectThat(result.mode).isEqualTo(HackleAppMode.WEB_VIEW_WRAPPER)
        expectThat(result.pollingIntervalMillis).isEqualTo(120000)
        expectThat(result.eventFlushThreshold).isEqualTo(20)
        // WEB_VIEW_WRAPPER mode disables session tracking
        expectThat(result.sessionTracking).isEqualTo(false)
    }

    @Test
    fun exposureEventDedupIntervalMillis() {
        configTest(60000, HackleConfig::exposureEventDedupIntervalMillis)
        configTest(60000, HackleConfig::exposureEventDedupIntervalMillis) {
            exposureEventDedupIntervalMillis(999)
        }

        configTest(60000, HackleConfig::exposureEventDedupIntervalMillis) {
            exposureEventDedupIntervalMillis(86400001)
        }

        for (i in 1000..86400000) {
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

    @Test
    fun `uri`() {
        configTests(
            HackleConfig::sdkUri to "https://sdk-api.hackle.io",
            HackleConfig::eventUri to "https://event-api.hackle.io",
            HackleConfig::monitoringUri to "https://monitoring.hackle.io",
        )
        configTests(
            HackleConfig::sdkUri to "https://test-sdk.hackle.io",
            HackleConfig::eventUri to "https://test-event.hackle.io",
            HackleConfig::monitoringUri to "https://test-monitoring.hackle.io",
        ) {
            sdkUri("https://test-sdk.hackle.io")
            eventUri("https://test-event.hackle.io")
            monitoringUri("https://test-monitoring.hackle.io")
        }
    }

    @Test
    fun `sessionTimeoutMillis`() {
        configTests(HackleConfig::sessionTimeoutMillis to 1000 * 60 * 30)
        configTests(HackleConfig::sessionTimeoutMillis to 42) {
            sessionTimeoutMillis(42)
        }
    }

    @Test
    fun `pollingIntervalMillis`() {
        configTests(HackleConfig::pollingIntervalMillis to -1)
        configTests(HackleConfig::pollingIntervalMillis to 60000) {
            pollingIntervalMillis(60000)
        }
        configTests(HackleConfig::pollingIntervalMillis to 60000) {
            pollingIntervalMillis(59999)
        }
    }

    @Test
    fun `mode`() {
        configTests(HackleConfig::mode to HackleAppMode.NATIVE)
        configTests(
            HackleConfig::mode to HackleAppMode.WEB_VIEW_WRAPPER,
            HackleConfig::sessionTracking to false
        ) {
            mode(HackleAppMode.WEB_VIEW_WRAPPER)
        }
    }

    private fun <T> configTests(
        vararg tests: Pair<(HackleConfig) -> T, T>,
        builder: HackleConfig.Builder.() -> Unit = {}
    ) {

        val config = HackleConfig.builder().apply(builder).build()
        for ((actual, expected) in tests) {
            assertEquals(expected, actual(config))
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
