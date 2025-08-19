package io.hackle.android

import org.junit.Test
import strikt.api.expectThrows

class HackleAppInvocatorTest {
    @Test
    fun `throws NullPointerException when HackleApp is not initialized`() {
        expectThrows<IllegalStateException> {
            HackleAppInvocator.hackleInvocator()
        }
    }
}
