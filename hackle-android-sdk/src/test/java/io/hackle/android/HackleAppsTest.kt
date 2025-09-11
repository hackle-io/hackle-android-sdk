package io.hackle.android

import android.content.Context
import io.hackle.android.internal.model.Sdk
import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isA

class HackleAppsTest {

    @MockK
    private lateinit var applicationContext: Context

    private val testSdkKey = "test_sdk_key"
    private val testConfig = HackleConfig.DEFAULT

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
    }

    @Test
    fun `Sdk of should create SDK with correct properties`() {
        // when
        val result = Sdk.of(testSdkKey, testConfig)
        
        // then
        expectThat(result.key).isEqualTo(testSdkKey)
        expectThat(result.name).isEqualTo("android-sdk")
        expectThat(result.version).isA<String>()
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
    fun `HackleAppMode enum should have expected values`() {
        // then
        expectThat(HackleAppMode.NATIVE).isA<HackleAppMode>()
        expectThat(HackleAppMode.WEB_VIEW_WRAPPER).isA<HackleAppMode>()
    }
}