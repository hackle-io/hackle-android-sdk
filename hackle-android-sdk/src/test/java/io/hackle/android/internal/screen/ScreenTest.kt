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

    @Test
    fun `equals - screens with same name and className but different properties are equal`() {
        // given
        val screen1 = Screen.builder("Home", "HomeVC")
            .property("campaign_id", "A")
            .build()

        val screen2 = Screen.builder("Home", "HomeVC")
            .property("campaign_id", "B")
            .build()

        // then - screens are equal (properties don't affect equality)
        expectThat(screen1).isEqualTo(screen2)
        expectThat(screen2).isEqualTo(screen1)
    }

    @Test
    fun `equals - screens with same name and className have same hashCode regardless of properties`() {
        // given
        val screen1 = Screen.builder("Home", "HomeVC")
            .property("entry_point", "push")
            .build()

        val screen2 = Screen.builder("Home", "HomeVC")
            .property("entry_point", "deeplink")
            .build()

        // then - same hashCode (properties don't affect hashCode)
        expectThat(screen1.hashCode()).isEqualTo(screen2.hashCode())
    }

    @Test
    fun `equals - screens with different name are not equal even with same properties`() {
        // given
        val properties = mapOf("key" to "value")
        val screen1 = Screen.builder("Home", "HomeVC")
            .properties(properties)
            .build()

        val screen2 = Screen.builder("Profile", "HomeVC")
            .properties(properties)
            .build()

        // then - not equal (different name)
        expectThat(screen1 == screen2).isEqualTo(false)
    }

    @Test
    fun `equals - screens with different className are not equal even with same properties`() {
        // given
        val properties = mapOf("key" to "value")
        val screen1 = Screen.builder("Home", "HomeVC")
            .properties(properties)
            .build()

        val screen2 = Screen.builder("Home", "MainVC")
            .properties(properties)
            .build()

        // then - not equal (different className)
        expectThat(screen1 == screen2).isEqualTo(false)
    }

    @Test
    fun `equals - duplicate screens with different properties are treated as duplicates in Set`() {
        // given - same screen with different properties
        val screen1 = Screen.builder("Home", "HomeVC")
            .property("campaign_id", "A")
            .build()

        val screen2 = Screen.builder("Home", "HomeVC")
            .property("campaign_id", "B")
            .build()

        val screen3 = Screen.builder("Profile", "ProfileVC")
            .build()

        // when
        val screenSet = setOf(screen1, screen2, screen3)

        // then - screen1 and screen2 are considered duplicates, only 2 unique screens
        expectThat(screenSet.size).isEqualTo(2)
        expectThat(screenSet.contains(screen1)).isEqualTo(true)
        expectThat(screenSet.contains(screen2)).isEqualTo(true)
        expectThat(screenSet.contains(screen3)).isEqualTo(true)
    }

    @Test
    fun `equals - screens with same identity but different properties work correctly as Map keys`() {
        // given
        val screen1 = Screen.builder("Home", "HomeVC")
            .property("version", "1.0")
            .build()

        val screen2 = Screen.builder("Home", "HomeVC")
            .property("version", "2.0")
            .build()

        // when
        val map = mutableMapOf<Screen, String>()
        map[screen1] = "first"
        map[screen2] = "second"

        // then - screen2 overwrites screen1 (same key because equals returns true)
        expectThat(map.size).isEqualTo(1)
        expectThat(map[screen1]).isEqualTo("second")
        expectThat(map[screen2]).isEqualTo("second")
    }

    @Test
    fun `equals - same screen reference is equal to itself`() {
        // given
        val screen = Screen.builder("Home", "HomeVC")
            .property("key", "value")
            .build()

        // then
        expectThat(screen).isEqualTo(screen)
        expectThat(screen.hashCode()).isEqualTo(screen.hashCode())
    }

    @Test
    fun `equals - screen is not equal to null or different type`() {
        // given
        val screen = Screen.builder("Home", "HomeVC").build()

        // then
        expectThat(screen.equals(null)).isEqualTo(false)
        expectThat(screen.equals("Home")).isEqualTo(false)
        expectThat(screen.equals(42)).isEqualTo(false)
    }

    @Test
    fun `equals - iOS compatibility - same screen with different properties prevents duplicate page view events`() {
        // given - simulating user entering same home screen from different entry points
        val homeScreenFromPush = Screen.builder("Home", "HomeVC")
            .property("entry_point", "push")
            .build()

        val homeScreenFromDeeplink = Screen.builder("Home", "HomeVC")
            .property("entry_point", "deeplink")
            .build()

        // then - these should be considered the same screen
        // This prevents duplicate $page_view events in ScreenManager
        expectThat(homeScreenFromPush).isEqualTo(homeScreenFromDeeplink)
        expectThat(homeScreenFromPush.hashCode()).isEqualTo(homeScreenFromDeeplink.hashCode())
    }

    private class ScreenActivity : Activity()
}
