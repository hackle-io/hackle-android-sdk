package io.hackle.android

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import io.hackle.android.internal.database.repository.KeyValueRepository
import io.hackle.android.internal.platform.device.Device
import io.hackle.android.mock.MockDevice
import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.junit.After
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isA
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
        every { Device.create(any<Context>(), any<KeyValueRepository>())} returns MockDevice(
            id = "test_device_id",
            properties = emptyMap(),
            isIdCreated = true
        )
    }

    @After
    fun tearDown() {
        unmockkObject(HackleApp)
        unmockkObject(Device)
        clearAllMocks()
    }

    @Test
    fun `HackleApps create should return HackleApp instance with default config`() {
        // when
        val result = HackleApps.create(context, testSdkKey, testConfig)
        
        // then
        expectThat(result).isA<HackleApp>()
        expectThat(result.sdk.key).isEqualTo(testSdkKey)
        expectThat(result.mode).isEqualTo(HackleAppMode.NATIVE)
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
        expectThat(result.mode).isEqualTo(HackleAppMode.WEB_VIEW_WRAPPER)
    }

    @Test
    fun `HackleApps create should use context for Android operations`() {
        // when
        HackleApps.create(context, testSdkKey, testConfig)
        
        // then - verify that context was used for SharedPreferences access
        verify(atLeast = 1) { context.getSharedPreferences(any(), any()) }
        verify(atLeast = 1) { context.packageName }
    }
}