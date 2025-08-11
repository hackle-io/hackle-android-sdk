
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
        sut = PIIEventManager()
    }

    @Test
    fun `set phone number`() {
        // given
        every { userManager.resolve(null) } returns mockk()
        val phoneNumber = "0101234567890"

        // when
        val event = sut.setPhoneNumber(PhoneNumber(phoneNumber))

        // then
        expectThat(event).isEqualTo(Event.builder("\$secured_properties")
            .property("\$set", mapOf("\$phone_number" to phoneNumber))
            .build())
    }

    @Test
    fun `unset phone number`() {
        // given
        every { userManager.resolve(null) } returns mockk()

        // when
        val event = sut.unsetPhoneNumber()
        
        // then
        expectThat(event).isEqualTo(Event.builder("\$secured_properties")
            .property("\$unset", mapOf("\$phone_number" to "-"))
            .build())
    }
}
