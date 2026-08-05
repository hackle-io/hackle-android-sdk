package io.hackle.android.internal.user.local

import io.hackle.android.internal.user.UserManager
import io.hackle.sdk.common.PropertyOperations
import io.hackle.sdk.common.User
import io.hackle.sdk.core.HackleCore
import io.hackle.sdk.core.user.HackleUser
import io.hackle.sdk.core.user.IdentifierType
import io.mockk.*
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class PropertiesEventTrackerTest {

    @MockK
    private lateinit var userManager: UserManager

    @MockK
    private lateinit var core: HackleCore

    @InjectMockKs
    private lateinit var sut: PropertiesEventTracker

    @Before
    fun before() {
        MockKAnnotations.init(this, relaxUnitFun = true)
    }

    @Test
    fun `onPropertyOperations - operations를 property 이벤트로 변환해 track하고 flush한다`() {
        // given
        val user = User.builder().deviceId("device_id").build()
        val hackleUser = HackleUser.builder().identifier(IdentifierType.ID, "user").build()
        every { userManager.hackleUser(user) } returns hackleUser

        val operations = PropertyOperations.builder()
            .set("age", 42)
            .unset("grade")
            .build()

        // when
        sut.onPropertyOperations(user, operations, 320)

        // then
        verifyOrder {
            core.track(
                withArg {
                    expectThat(it) {
                        get { key } isEqualTo "\$properties"
                        get { properties } isEqualTo mapOf(
                            "\$set" to mapOf<String, Any>("age" to 42),
                            "\$unset" to mapOf<String, Any>("grade" to "-"),
                        )
                    }
                },
                hackleUser,
                320
            )
            core.flush()
        }
    }

    @Test
    fun `onUserUpdated - 아무것도 하지 않는다`() {
        // when
        sut.onUserUpdated(User.builder().build(), User.builder().userId("user").build(), 42)

        // then
        verify { core wasNot Called }
    }
}
