package io.hackle.android

import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import io.hackle.sdk.common.HackleSessionPolicy
import io.hackle.sdk.common.HackleSessionPersistCondition
import io.hackle.sdk.common.HackleSessionTimeoutCondition
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNull
import strikt.assertions.isSameInstanceAs

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
        expectThat(result.automaticAppLifecycleTracking).isEqualTo(true)
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

    @Test
    fun `automaticAppLifecycleTracking`() {
        configTests(HackleConfig::automaticAppLifecycleTracking to true)
        configTests(HackleConfig::automaticAppLifecycleTracking to false) {
            automaticAppLifecycleTracking(false)
        }
        configTests(HackleConfig::automaticAppLifecycleTracking to true) {
            automaticAppLifecycleTracking(true)
        }
    }

    @Test
    fun `enableMonitoring`() {
        configTests(HackleConfig::enableMonitoring to true)
        configTests(HackleConfig::enableMonitoring to false) {
            enableMonitoring(false)
        }
        configTests(HackleConfig::enableMonitoring to true) {
            enableMonitoring(true)
        }
    }

    @Test
    fun `sessionPolicy`() {
        // default
        val defaultConfig = HackleConfig.builder().build()
        expectThat(defaultConfig.sessionPolicy.persistCondition).isNull()

        // custom
        val customPolicy = HackleSessionPolicy.builder()
            .persistCondition(HackleSessionPersistCondition.NULL_TO_USER_ID)
            .build()
        val customConfig = HackleConfig.builder()
            .sessionPolicy(customPolicy)
            .build()
        expectThat(customConfig.sessionPolicy.persistCondition).isSameInstanceAs(HackleSessionPersistCondition.NULL_TO_USER_ID)
    }

    @Test
    fun `sessionPolicy timeout millis - config timeout만 설정하면 policy에 복사된다`() {
        val config = HackleConfig.builder()
            .sessionTimeoutMillis(60000)
            .build()
        expectThat(config.sessionPolicy.timeoutCondition.timeoutMillis).isEqualTo(60000L)
    }

    @Test
    fun `sessionPolicy timeout millis - policy timeout 설정하면 config timeout을 무시한다`() {
        val policy = HackleSessionPolicy.builder()
            .timeoutCondition(HackleSessionTimeoutCondition.builder().millis(60000).build())
            .build()
        val config = HackleConfig.builder()
            .sessionTimeoutMillis(120000)
            .sessionPolicy(policy)
            .build()
        expectThat(config.sessionPolicy.timeoutCondition.timeoutMillis).isEqualTo(60000L)
    }

    @Test
    fun `sessionPolicy timeout millis - 아무것도 설정하지 않으면 기본값이 policy에 복사된다`() {
        val config = HackleConfig.builder().build()
        expectThat(config.sessionPolicy.timeoutCondition.timeoutMillis).isEqualTo(1000L * 60 * 30)
    }

    @Test
    fun `sessionPolicy timeout millis - policy에 persistCondition과 함께 사용`() {
        val policy = HackleSessionPolicy.builder()
            .persistCondition(HackleSessionPersistCondition.NULL_TO_USER_ID)
            .timeoutCondition(HackleSessionTimeoutCondition.builder().millis(60000).build())
            .build()
        val config = HackleConfig.builder()
            .sessionPolicy(policy)
            .build()
        expectThat(config.sessionPolicy.timeoutCondition.timeoutMillis).isEqualTo(60000L)
        expectThat(config.sessionPolicy.persistCondition).isSameInstanceAs(HackleSessionPersistCondition.NULL_TO_USER_ID)
    }

    @Test
    fun `sessionPolicy timeout millis - config timeout 설정시 기존 persistCondition 유지`() {
        val policy = HackleSessionPolicy.builder()
            .persistCondition(HackleSessionPersistCondition.NULL_TO_USER_ID)
            .build()
        val config = HackleConfig.builder()
            .sessionPolicy(policy)
            .sessionTimeoutMillis(60000)
            .build()
        expectThat(config.sessionPolicy.timeoutCondition.timeoutMillis).isEqualTo(60000L)
        expectThat(config.sessionPolicy.persistCondition).isSameInstanceAs(HackleSessionPersistCondition.NULL_TO_USER_ID)
    }

    @Suppress("DEPRECATION")
    @Test
    fun `sessionTimeoutMillis deprecated property - sessionPolicy의 timeout millis를 반환한다`() {
        val config = HackleConfig.builder()
            .sessionTimeoutMillis(60000)
            .build()
        expectThat(config.sessionTimeoutMillis).isEqualTo(60000)
    }

    @Suppress("DEPRECATION")
    @Test
    fun `sessionTimeoutMillis deprecated property - 기본값은 30분이다`() {
        val config = HackleConfig.builder().build()
        expectThat(config.sessionTimeoutMillis).isEqualTo(1000 * 60 * 30)
    }

    @Test
    fun `HackleSessionPolicy toBuilder - 모든 필드가 정확히 복사된다`() {
        val original = HackleSessionPolicy.builder()
            .persistCondition(HackleSessionPersistCondition.NULL_TO_USER_ID)
            .timeoutCondition(HackleSessionTimeoutCondition.builder().millis(42000).onBackground(false).onApplicationStateChange(true).build())
            .build()
        val copy = original.toBuilder().build()
        expectThat(copy.persistCondition).isSameInstanceAs(original.persistCondition)
        expectThat(copy.timeoutCondition.timeoutMillis).isEqualTo(original.timeoutCondition.timeoutMillis)
        expectThat(copy.timeoutCondition.onBackground).isEqualTo(original.timeoutCondition.onBackground)
        expectThat(copy.timeoutCondition.onApplicationStateChange).isEqualTo(original.timeoutCondition.onApplicationStateChange)
    }

    @Test
    fun `sessionPolicy timeout millis - 0 이하의 값은 기본값으로 대체된다`() {
        val config = HackleConfig.builder()
            .sessionPolicy(HackleSessionPolicy.builder().timeoutCondition(HackleSessionTimeoutCondition.builder().millis(0).build()).build())
            .build()
        expectThat(config.sessionPolicy.timeoutCondition.timeoutMillis).isEqualTo(1000L * 60 * 30)
    }

    @Test
    fun `sessionPolicy timeout millis - 음수값은 기본값으로 대체된다`() {
        val config = HackleConfig.builder()
            .sessionPolicy(HackleSessionPolicy.builder().timeoutCondition(HackleSessionTimeoutCondition.builder().millis(-1000).build()).build())
            .build()
        expectThat(config.sessionPolicy.timeoutCondition.timeoutMillis).isEqualTo(1000L * 60 * 30)
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
