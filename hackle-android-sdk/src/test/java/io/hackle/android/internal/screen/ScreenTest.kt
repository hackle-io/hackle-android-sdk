package io.hackle.android.internal.screen

import android.app.Activity
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class ScreenTest {

    @Test
    fun `create from activity`() {
        val screen = Screen.from(ScreenActivity())
        expectThat(screen).isEqualTo(Screen("ScreenActivity", "ScreenActivity"))
    }

    private class ScreenActivity : Activity()
}
