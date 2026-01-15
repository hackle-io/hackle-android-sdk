package io.hackle.android.internal.screen

import android.app.Activity
import io.hackle.sdk.common.Screen
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class ScreenTest {

    @Test
    fun `create from activity`() {
        val screen = Screen.from(ScreenActivity())
        expectThat(screen).isEqualTo(
            Screen.builder("ScreenActivity", "ScreenActivity").build()
        )
    }

    @Test
    fun `builder - creates screen with name and className only`() {
        val screen = Screen.builder("TestScreen", "com.test.TestScreen")
            .build()

        expectThat(screen.name).isEqualTo("TestScreen")
        expectThat(screen.className).isEqualTo("com.test.TestScreen")
        expectThat(screen.properties).isEqualTo(null)
    }

    @Test
    fun `builder - creates screen with properties`() {
        val properties = mapOf(
            "key1" to "value1",
            "key2" to 123,
            "key3" to true
        )

        val screen = Screen.builder("TestScreen", "com.test.TestScreen")
            .properties(properties)
            .build()

        expectThat(screen.name).isEqualTo("TestScreen")
        expectThat(screen.className).isEqualTo("com.test.TestScreen")
        expectThat(screen.properties).isEqualTo(properties)
    }

    @Test
    fun `builder - properties can be null`() {
        val screen = Screen.builder("TestScreen", "com.test.TestScreen")
            .properties(null)
            .build()

        expectThat(screen.name).isEqualTo("TestScreen")
        expectThat(screen.className).isEqualTo("com.test.TestScreen")
        expectThat(screen.properties).isEqualTo(null)
    }

    @Test
    fun `builder - properties can be empty map`() {
        val screen = Screen.builder("TestScreen", "com.test.TestScreen")
            .properties(emptyMap())
            .build()

        expectThat(screen.name).isEqualTo("TestScreen")
        expectThat(screen.className).isEqualTo("com.test.TestScreen")
        expectThat(screen.properties).isEqualTo(emptyMap<String, Any>())
    }

    private class ScreenActivity : Activity()
}
