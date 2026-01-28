package io.hackle.sdk.common

import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue

class HackleWebViewConfigTest {

    @Test
    fun `DEFAULT config has automaticRouteTracking enabled`() {
        val config = HackleWebViewConfig.DEFAULT
        expectThat(config.automaticRouteTracking).isTrue()
    }

    @Test
    fun `DEFAULT config has automaticScreenTracking disabled`() {
        val config = HackleWebViewConfig.DEFAULT
        expectThat(config.automaticScreenTracking).isFalse()
    }

    @Test
    fun `DEFAULT config has automaticEngagementTracking disabled`() {
        val config = HackleWebViewConfig.DEFAULT
        expectThat(config.automaticEngagementTracking).isFalse()
    }

    @Test
    fun `builder creates config with default values`() {
        val config = HackleWebViewConfig.builder().build()

        expectThat(config.automaticRouteTracking).isTrue()
        expectThat(config.automaticScreenTracking).isFalse()
        expectThat(config.automaticEngagementTracking).isFalse()
    }

    @Test
    fun `builder sets automaticRouteTracking`() {
        val config = HackleWebViewConfig.builder()
            .automaticRouteTracking(false)
            .build()

        expectThat(config.automaticRouteTracking).isFalse()
    }

    @Test
    fun `builder sets automaticScreenTracking`() {
        val config = HackleWebViewConfig.builder()
            .automaticScreenTracking(true)
            .build()

        expectThat(config.automaticScreenTracking).isTrue()
    }

    @Test
    fun `builder sets automaticEngagementTracking`() {
        val config = HackleWebViewConfig.builder()
            .automaticEngagementTracking(true)
            .build()

        expectThat(config.automaticEngagementTracking).isTrue()
    }

    @Test
    fun `builder sets all options`() {
        val config = HackleWebViewConfig.builder()
            .automaticRouteTracking(false)
            .automaticScreenTracking(true)
            .automaticEngagementTracking(true)
            .build()

        expectThat(config.automaticRouteTracking).isFalse()
        expectThat(config.automaticScreenTracking).isTrue()
        expectThat(config.automaticEngagementTracking).isTrue()
    }

    @Test
    fun `builder is chainable`() {
        val builder = HackleWebViewConfig.builder()

        val result = builder
            .automaticRouteTracking(true)
            .automaticScreenTracking(true)
            .automaticEngagementTracking(true)

        expectThat(result).isEqualTo(builder)
    }
}
