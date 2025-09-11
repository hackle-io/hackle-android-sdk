package io.hackle.android

import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isA

class HackleAppsModeTest {
    @Test
    fun `HackleAppMode enum should have expected values`() {
        // then
        expectThat(HackleAppMode.NATIVE).isA<HackleAppMode>()
        expectThat(HackleAppMode.WEB_VIEW_WRAPPER).isA<HackleAppMode>()
    }
}