
package io.hackle.android.internal.pii

import io.hackle.android.internal.pii.phonenumber.PhoneNumber
import io.hackle.android.internal.user.UserManager
import io.hackle.sdk.common.Event
import io.hackle.sdk.common.User
import io.hackle.sdk.core.HackleCore
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class PIIEventManagerTest {

    private lateinit var userManager: UserManager
    private lateinit var core: HackleCore
    private lateinit var sut: PIIEventManager

    @Before
    fun before() {
        userManager = mockk()
        core = mockk(relaxed = true)
        sut = PIIEventManager(userManager, core)
    }

    @Test
    fun `set phone number`() {
        // given
        every { userManager.resolve(any()) } returns mockk()
        val phoneNumber = "0101234567890"

        // when
        sut.setPhoneNumber(PhoneNumber(phoneNumber), 42)

        // then
        verify(exactly = 1) {
            core.track(
                event = withArg {
                    expectThat(it).isEqualTo(
                        Event.builder("\$secured_properties")
                            .property("\$set", mapOf("\$phone_number" to phoneNumber))
                            .build()
                    )
                },
                user = any(),
                timestamp = 42
            )
        }
    }

    @Test
    fun `unset phone number`() {
        // given
        every { userManager.resolve(any()) } returns mockk()

        // when
        sut.unsetPhoneNumber(42)

        // then
        verify(exactly = 1) {
            core.track(
                event = withArg {
                    expectThat(it).isEqualTo(
                        Event.builder("\$secured_properties")
                            .property("\$unset", mapOf("\$phone_number" to "-"))
                            .build()
                    )
                },
                user = any(),
                timestamp = 42
            )
        }
    }
}
