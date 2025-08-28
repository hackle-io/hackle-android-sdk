package io.hackle.android.ui.notification

import junit.framework.TestCase.assertEquals
import org.junit.Test

class ChannelTypeTest {

    @Test
    fun `from should return correct ChannelType for valid names`() {
        // Given & When & Then
        assertEquals(ChannelType.HACKLE_DEFAULT, ChannelType.from("HACKLE_DEFAULT"))
        assertEquals(ChannelType.HACKLE_HIGH, ChannelType.from("HACKLE_HIGH"))
        assertEquals(ChannelType.CUSTOM, ChannelType.from("CUSTOM"))
    }

    @Test
    fun `from should return HACKLE_DEFAULT for non-existent channel names`() {
        // Given
        val invalidNames = listOf(
            "INVALID_CHANNEL",
            "hackle_default", 
            "HACKLE_LOW",
            "UNKNOWN",
            ""
        )

        // When & Then
        invalidNames.forEach { invalidName ->
            assertEquals(ChannelType.HACKLE_DEFAULT, ChannelType.from(invalidName),
            )
        }
    }
    
    @Test
    fun `all enum values should be correctly mapped in ALL`() {
        // Given
        val expectedChannelTypes = ChannelType.values()

        // When & Then
        expectedChannelTypes.forEach { expectedType ->
            val actualType = ChannelType.from(expectedType.name)
            assertEquals(expectedType, actualType)
        }
    }

    @Test
    fun `from should be case sensitive`() {
        // Given & When & Then
        assertEquals(ChannelType.HACKLE_DEFAULT, ChannelType.from("hackle_default"))
        assertEquals(ChannelType.HACKLE_DEFAULT, ChannelType.from("Hackle_Default"))
        assertEquals(ChannelType.HACKLE_DEFAULT, ChannelType.from("HACKLE_default"))
    }

    @Test
    fun `from should handle empty and whitespace strings`() {
        // Given & When & Then
        assertEquals(ChannelType.HACKLE_DEFAULT, ChannelType.from(""))
        assertEquals(ChannelType.HACKLE_DEFAULT, ChannelType.from(" "))
        assertEquals(ChannelType.HACKLE_DEFAULT, ChannelType.from("   "))
        assertEquals(ChannelType.HACKLE_DEFAULT, ChannelType.from("\t"))
        assertEquals(ChannelType.HACKLE_DEFAULT, ChannelType.from("\n"))
    }
}