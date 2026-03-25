package io.hackle.android

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import io.hackle.android.internal.application.lifecycle.ApplicationLifecycleManager
import io.hackle.android.internal.monitoring.metric.MonitoringMetricRegistry
import io.hackle.android.internal.platform.device.Device
import io.hackle.android.mock.MockDevice
import io.hackle.sdk.core.internal.metrics.Metrics
import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.junit.After
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import java.io.File

class HackleAppsTest {

    @MockK
    private lateinit var application: Application

    @MockK
    private lateinit var context: Context

    @MockK
    private lateinit var sharedPreferences: SharedPreferences

    @MockK
    private lateinit var sharedPreferencesEditor: SharedPreferences.Editor

    @MockK
    private lateinit var packageManager: PackageManager

    private val testSdkKey = "test_sdk_key"
    private val testConfig = HackleConfig.DEFAULT

    @Before
    fun before() {
        MockKAnnotations.init(this, relaxUnitFun = true)

        mockkObject(Metrics)

        // Context mocking setup
        every { context.applicationContext } returns application
        every { context.packageManager } returns packageManager
        every { context.packageName } returns "io.hackle.test"

        // SharedPreferences mocking setup
        every { context.getSharedPreferences(any(), any()) } returns sharedPreferences
        every { sharedPreferences.edit() } returns sharedPreferencesEditor
        every { sharedPreferencesEditor.putString(any(), any()) } returns sharedPreferencesEditor
        every { sharedPreferencesEditor.putLong(any(), any()) } returns sharedPreferencesEditor
        every { sharedPreferencesEditor.putInt(any(), any()) } returns sharedPreferencesEditor
        every { sharedPreferencesEditor.putBoolean(any(), any()) } returns sharedPreferencesEditor
        every { sharedPreferencesEditor.apply() } just Runs
        every { sharedPreferencesEditor.commit() } returns true

        every { sharedPreferences.getString(any(), any()) } returns null
        every { sharedPreferences.getLong(any(), any()) } returns 0L
        every { sharedPreferences.getInt(any(), any()) } returns 0
        every { sharedPreferences.getBoolean(any(), any()) } returns false

        // Create real File objects for testing
        val tempDir = File("/tmp/hackle_test")
        tempDir.mkdirs()

        // File operations mocking
        every { context.filesDir } returns tempDir
        every { context.cacheDir } returns tempDir
        every { context.getDir(any(), any()) } returns tempDir

        // System services mocking
        every { context.getSystemService(any()) } returns null

        mockkObject(Device)
        every { Device.create(any<Context>(), any<String>()) } returns MockDevice(
            id = "test_device_id",
            properties = emptyMap()
        )
    }

    @After
    fun tearDown() {
        unmockkObject(HackleApp)
        unmockkObject(Device)
        unmockkObject(Metrics)
        unmockkObject(ApplicationLifecycleManager)
        clearAllMocks()
    }

    @Test
    fun `HackleApps create should return HackleApp instance with default config`() {
        // when
        val result = HackleApps.create(context, testSdkKey, testConfig)

        // then
        expectThat(result).isA<HackleApp>()
        expectThat(result.sdk.key).isEqualTo(testSdkKey)
        expectThat(result.config.mode).isEqualTo(HackleAppMode.NATIVE)
    }

    @Test
    fun `HackleApps create should return HackleApp instance with custom config`() {
        // given
        val customConfig = HackleConfig.builder()
            .mode(HackleAppMode.WEB_VIEW_WRAPPER)
            .eventFlushThreshold(20)
            .build()

        // when
        val result = HackleApps.create(context, testSdkKey, customConfig)

        // then
        expectThat(result).isA<HackleApp>()
        expectThat(result.sdk.key).isEqualTo(testSdkKey)
        expectThat(result.config.mode).isEqualTo(HackleAppMode.WEB_VIEW_WRAPPER)
    }

    @Test
    fun `HackleApps create should use context for Android operations`() {
        // when
        HackleApps.create(context, testSdkKey, testConfig)

        // then - verify that context was used for SharedPreferences access
        verify(atLeast = 1) { context.getSharedPreferences(any(), any()) }
        verify(atLeast = 1) { context.packageName }
    }

    @Test
    fun `HackleApps create should initialize metricConfiguration when monitoring is enabled`() {
        // given
        val config = HackleConfig.builder().enableMonitoring(true).build()

        // when
        HackleApps.create(context, testSdkKey, config)

        // then
        verify(exactly = 1) { Metrics.addRegistry(ofType(MonitoringMetricRegistry::class)) }
    }

    @Test
    fun `HackleApps create should not initialize metricConfiguration when monitoring is disabled`() {
        // given
        val config = HackleConfig.builder().enableMonitoring(false).build()

        // when
        HackleApps.create(context, testSdkKey, config)

        // then
        verify(exactly = 0) { Metrics.addRegistry(ofType(MonitoringMetricRegistry::class)) }
    }
}