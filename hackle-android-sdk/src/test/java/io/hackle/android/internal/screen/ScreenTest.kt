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
        expectThat(screen.properties).isEqualTo(emptyMap())
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
        expectThat(screen.properties).isEqualTo(emptyMap())
    }

    @Test
    fun `builder - properties can be empty map`() {
        val screen = Screen.builder("TestScreen", "com.test.TestScreen")
            .properties(emptyMap())
            .build()

        expectThat(screen.name).isEqualTo("TestScreen")
        expectThat(screen.className).isEqualTo("com.test.TestScreen")
        expectThat(screen.properties).isEqualTo(emptyMap())
    }

    @Test
    fun `builder - single property can be added`() {
        val screen = Screen.builder("TestScreen", "com.test.TestScreen")
            .property("key1", "value1")
            .build()

        expectThat(screen.name).isEqualTo("TestScreen")
        expectThat(screen.className).isEqualTo("com.test.TestScreen")
        expectThat(screen.properties).isEqualTo(mapOf("key1" to "value1"))
    }

    @Test
    fun `builder - multiple properties can be added via property method`() {
        val screen = Screen.builder("TestScreen", "com.test.TestScreen")
            .property("key1", "value1")
            .property("key2", 123)
            .property("key3", true)
            .build()

        expectThat(screen.name).isEqualTo("TestScreen")
        expectThat(screen.className).isEqualTo("com.test.TestScreen")
        expectThat(screen.properties).isEqualTo(mapOf(
            "key1" to "value1",
            "key2" to 123,
            "key3" to true
        ))
    }

    @Test
    fun `builder - property and properties can be mixed`() {
        val screen = Screen.builder("TestScreen", "com.test.TestScreen")
            .property("key1", "value1")
            .properties(mapOf("key2" to 123, "key3" to true))
            .property("key4", "value4")
            .build()

        expectThat(screen.name).isEqualTo("TestScreen")
        expectThat(screen.className).isEqualTo("com.test.TestScreen")
        expectThat(screen.properties).isEqualTo(mapOf(
            "key1" to "value1",
            "key2" to 123,
            "key3" to true,
            "key4" to "value4"
        ))
    }

    @Test
    fun `builder - later property overwrites earlier property with same key`() {
        val screen = Screen.builder("TestScreen", "com.test.TestScreen")
            .property("key1", "value1")
            .property("key1", "value2")
            .build()

        expectThat(screen.name).isEqualTo("TestScreen")
        expectThat(screen.className).isEqualTo("com.test.TestScreen")
        expectThat(screen.properties).isEqualTo(mapOf("key1" to "value2"))
    }

    @Test
    fun `builder - properties map overwrites earlier property with same key`() {
        val screen = Screen.builder("TestScreen", "com.test.TestScreen")
            .property("key1", "value1")
            .properties(mapOf("key1" to "value2", "key2" to 123))
            .build()

        expectThat(screen.name).isEqualTo("TestScreen")
        expectThat(screen.className).isEqualTo("com.test.TestScreen")
        expectThat(screen.properties).isEqualTo(mapOf(
            "key1" to "value2",
            "key2" to 123
        ))
    }

    @Test
    fun `builder - property after properties overwrites value`() {
        val screen = Screen.builder("TestScreen", "com.test.TestScreen")
            .properties(mapOf("key1" to "value1", "key2" to 123))
            .property("key1", "value2")
            .build()

        expectThat(screen.name).isEqualTo("TestScreen")
        expectThat(screen.className).isEqualTo("com.test.TestScreen")
        expectThat(screen.properties).isEqualTo(mapOf(
            "key1" to "value2",
            "key2" to 123
        ))
    }

    @Test
    fun `builder - multiple properties calls merge all properties`() {
        val screen = Screen.builder("TestScreen", "com.test.TestScreen")
            .properties(mapOf("key1" to "value1"))
            .properties(mapOf("key2" to 123))
            .properties(mapOf("key3" to true))
            .build()

        expectThat(screen.name).isEqualTo("TestScreen")
        expectThat(screen.className).isEqualTo("com.test.TestScreen")
        expectThat(screen.properties).isEqualTo(mapOf(
            "key1" to "value1",
            "key2" to 123,
            "key3" to true
        ))
    }

    @Test
    fun `builder - properties with null does not affect existing properties`() {
        val screen = Screen.builder("TestScreen", "com.test.TestScreen")
            .property("key1", "value1")
            .properties(null)
            .property("key2", 123)
            .build()

        expectThat(screen.name).isEqualTo("TestScreen")
        expectThat(screen.className).isEqualTo("com.test.TestScreen")
        expectThat(screen.properties).isEqualTo(mapOf(
            "key1" to "value1",
            "key2" to 123
        ))
    }

    @Test
    fun `builder - complex property types can be added`() {
        val list = listOf(1, 2, 3)

        val screen = Screen.builder("TestScreen", "com.test.TestScreen")
            .property("list", list)
            .property("number", 42.5)
            .build()

        expectThat(screen.name).isEqualTo("TestScreen")
        expectThat(screen.className).isEqualTo("com.test.TestScreen")
        expectThat(screen.properties).isEqualTo(mapOf(
            "list" to list,
            "number" to 42.5
        ))
    }

    @Test
    fun `backwards compatibility - deprecated constructor creates equivalent screen to builder without properties`() {
        @Suppress("DEPRECATION")
        val screenFromConstructor = Screen("TestScreen", "com.test.TestScreen")
        val screenFromBuilder = Screen.builder("TestScreen", "com.test.TestScreen").build()

        expectThat(screenFromConstructor.name).isEqualTo(screenFromBuilder.name)
        expectThat(screenFromConstructor.className).isEqualTo(screenFromBuilder.className)
        expectThat(screenFromConstructor.properties).isEqualTo(screenFromBuilder.properties)
    }

    @Test
    fun `backwards compatibility - deprecated init produces same result as builder`() {
        // given & when
        @Suppress("DEPRECATION")
        val screenFromDeprecatedInit = Screen("Test", "TestVC")
        val screenFromBuilder = Screen.builder("Test", "TestVC").build()

        // then
        expectThat(screenFromDeprecatedInit).isEqualTo(screenFromBuilder)
        expectThat(screenFromDeprecatedInit.name).isEqualTo(screenFromBuilder.name)
        expectThat(screenFromDeprecatedInit.className).isEqualTo(screenFromBuilder.className)
        expectThat(screenFromDeprecatedInit.properties).isEqualTo(emptyMap())
        expectThat(screenFromBuilder.properties).isEqualTo(emptyMap())
    }


    @Test
    fun `backwards compatibility - deprecated constructor creates screen with empty properties`() {
        @Suppress("DEPRECATION")
        val screen = Screen("TestScreen", "com.test.TestScreen")

        expectThat(screen.name).isEqualTo("TestScreen")
        expectThat(screen.className).isEqualTo("com.test.TestScreen")
        expectThat(screen.properties).isEqualTo(emptyMap())
    }

    private class ScreenActivity : Activity()
}
