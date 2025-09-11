package io.hackle.android.internal.model

import io.hackle.android.HackleConfig
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo

class SdkTest {
    private val testSdkKey = "test_sdk_key"
    private val testConfig = HackleConfig.DEFAULT


    @Test
    fun `Sdk of should create SDK with correct properties`() {
        // when
        val result = Sdk.of(testSdkKey, testConfig)

        // then
        expectThat(result.key).isEqualTo(testSdkKey)
        expectThat(result.name).isEqualTo("android-sdk")
        expectThat(result.version).isA<String>()
    }
}