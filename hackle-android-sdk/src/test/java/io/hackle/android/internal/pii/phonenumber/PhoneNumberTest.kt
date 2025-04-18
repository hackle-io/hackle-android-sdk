package io.hackle.android.internal.pii.phonenumber

import junit.framework.TestCase.assertEquals
import org.junit.Test

class PhoneNumberTest {
    @Test
    fun `phone number`() {
        // Given
        val phoneNumber = "+821012345678"

        val result = PhoneNumber.create(phoneNumber)

        // Then
        assertEquals(result.value, phoneNumber)
    }
}