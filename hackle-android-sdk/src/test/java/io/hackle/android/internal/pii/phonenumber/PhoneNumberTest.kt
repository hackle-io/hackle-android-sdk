package io.hackle.android.internal.pii.phonenumber

import junit.framework.TestCase.assertEquals
import org.junit.Test

class PhoneNumberTest {
    @Test
    fun `filtered_phone_number_with_+`() {
        // Given
        val phoneNumbers = arrayOf(
            "+821012345678",
            "+82-10-1234-5678",
            "+82 10 1234 5678",
            "+82 10 1234-5678",
            "+82(10)12345678",
            "+82(10)1234-5678",
            "+82(10)1234 5678",
            "+82-10-1234-5678aaa",
            "aaa+82 10 1234 5678",
        )
        val expected = "+821012345678"

        for (phoneNumber in phoneNumbers) {
            // When
            val result = PhoneNumber.filtered(phoneNumber)

            // Then
            assertEquals(result, expected)
        }
    }

    @Test
    fun filtered_phone_number() {
        // Given
        val phoneNumbers = arrayOf(
            "01012345678",
            "01012345678",
            "010-1234-5678",
            "010 1234 5678",
            "010 1234-5678",
            "010(1234)5678",
            "010(1234)5678",
            "010(1234)5678aaa",
            "aaa010 1234 5678",
        )
        val expected = "01012345678"

        for (phoneNumber in phoneNumbers) {
            // When
            val result = PhoneNumber.filtered(phoneNumber)

            // Then
            assertEquals(result, expected)
        }
    }
}