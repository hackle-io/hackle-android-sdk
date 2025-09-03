package io.hackle.android.internal.inappmessage.evaluation

import io.hackle.android.internal.user.resolvedIdentifiers
import io.hackle.sdk.common.User
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class InAppMessageIdentifierCheckerTest {

    private val sut = InAppMessageIdentifierChecker()

    @Test
    fun `check`() {
        verify(null, null, null, null, false)
        verify(null, null, null, "c", false)
        verify(null, null, "c", null, false)
        verify(null, null, "c", "c", false)
        verify(null, null, "c", "d", true)

        verify(null, "a", null, null, false)
        verify(null, "a", null, "c", false)
        verify(null, "a", "c", null, false)
        verify(null, "a", "c", "c", false)
        verify(null, "a", "c", "d", true)

        verify("a", null, null, null, false)
        verify("a", null, null, "c", false)
        verify("a", null, "c", null, false)
        verify("a", null, "c", "c", false)
        verify("a", null, "c", "d", true)

        verify("a", "a", null, null, false)
        verify("a", "a", null, "c", false)
        verify("a", "a", "c", null, false)
        verify("a", "a", "c", "c", false)
        verify("a", "a", "c", "d", false)

        verify("a", "b", null, null, true)
        verify("a", "b", null, "c", true)
        verify("a", "b", "c", null, true)
        verify("a", "b", "c", "c", true)
        verify("a", "b", "c", "d", true)
    }

    private fun verify(
        oldUserId: String?,
        newUserId: String?,
        oldDeviceId: String?,
        newDeviceId: String?,
        expected: Boolean,
    ) {
        val oldUser = User.builder()
            .userId(oldUserId)
            .deviceId(oldDeviceId)
            .build()

        val newUser = User.builder()
            .userId(newUserId)
            .deviceId(newDeviceId)
            .build()

        val actual = sut.isIdentifierChanged(oldUser.resolvedIdentifiers, newUser.resolvedIdentifiers)
        expectThat(actual) isEqualTo expected
    }
}