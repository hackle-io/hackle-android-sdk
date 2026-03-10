package io.hackle.android.internal.invocator.invocation.handlers

import com.google.gson.GsonBuilder
import io.hackle.android.internal.HackleAppCore
import io.hackle.android.internal.context.HackleAppContext
import io.hackle.android.internal.invocator.invocation.InvocationRequest
import io.hackle.sdk.common.PropertyOperations
import io.hackle.sdk.common.User
import io.hackle.sdk.common.subscription.HackleSubscriptionStatus
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.hackle.android.support.assertThrows
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import strikt.assertions.isTrue

class UserInvocationHandlersTest {

    private lateinit var core: HackleAppCore
    private val gson = GsonBuilder().serializeNulls().create()

    @Before
    fun setup() {
        core = mockk(relaxUnitFun = true)
    }

    private fun request(command: String, parameters: Map<String, Any?>? = null): InvocationRequest {
        val map = mapOf<String, Any>(
            "_hackle" to mapOf(
                "command" to command,
                "parameters" to parameters,
                "browserProperties" to mapOf("url" to "https://hackle.io")
            )
        )
        return InvocationRequest.parse(gson.toJson(map))
    }

    // Session

    @Test
    fun `GetSessionIdInvocationHandler - sessionId를 반환한다`() {
        // given
        every { core.sessionId } returns "session-123"
        val sut = GetSessionIdInvocationHandler(core)

        // when
        val response = sut.invoke(request("getSessionId"))

        // then
        expectThat(response) {
            get { isSuccess }.isTrue()
            get { data }.isEqualTo("session-123")
        }
    }

    // User

    @Test
    fun `GetUserInvocationHandler - 현재 사용자 정보를 반환한다`() {
        // given
        val user = User.builder().id("foo").userId("bar").deviceId("device-1").build()
        every { core.user } returns user
        val sut = GetUserInvocationHandler(core)

        // when
        val response = sut.invoke(request("getUser"))

        // then
        expectThat(response) {
            get { isSuccess }.isTrue()
            get { data }.isNotNull().and {
                get { id }.isEqualTo("foo")
                get { userId }.isEqualTo("bar")
                get { deviceId }.isEqualTo("device-1")
            }
        }
    }

    @Test
    fun `SetUserInvocationHandler - 사용자를 설정한다`() {
        // given
        val sut = SetUserInvocationHandler(core)
        val params = mapOf("user" to mapOf("id" to "new-id", "userId" to "new-user"))

        // when
        val response = sut.invoke(request("setUser", params))

        // then
        expectThat(response.isSuccess).isTrue()
        verify(exactly = 1) { core.setUser(any(), null) }
    }

    @Test
    fun `SetUserInvocationHandler - user 파라미터가 없으면 예외를 던진다`() {
        val sut = SetUserInvocationHandler(core)
        assertThrows<IllegalStateException> {
            sut.invoke(request("setUser", emptyMap()))
        }
    }

    @Test
    fun `ResetUserInvocationHandler - 사용자를 리셋한다`() {
        // given
        val sut = ResetUserInvocationHandler(core)

        // when
        val response = sut.invoke(request("resetUser"))

        // then
        expectThat(response.isSuccess).isTrue()
        verify(exactly = 1) { core.resetUser(any<HackleAppContext>(), null) }
    }

    // UserIdentifiers

    @Test
    fun `SetUserIdInvocationHandler - userId를 설정한다`() {
        // given
        val sut = SetUserIdInvocationHandler(core)
        val params = mapOf<String, Any?>("userId" to "user-123")

        // when
        val response = sut.invoke(request("setUserId", params))

        // then
        expectThat(response.isSuccess).isTrue()
        verify(exactly = 1) { core.setUserId("user-123", null) }
    }

    @Test
    fun `SetUserIdInvocationHandler - userId가 null이면 null로 설정한다`() {
        // given
        val sut = SetUserIdInvocationHandler(core)
        val params = mapOf<String, Any?>("userId" to null)

        // when
        val response = sut.invoke(request("setUserId", params))

        // then
        expectThat(response.isSuccess).isTrue()
        verify(exactly = 1) { core.setUserId(null, null) }
    }

    @Test
    fun `SetDeviceIdInvocationHandler - deviceId를 설정한다`() {
        // given
        val sut = SetDeviceIdInvocationHandler(core)
        val params = mapOf<String, Any?>("deviceId" to "device-123")

        // when
        val response = sut.invoke(request("setDeviceId", params))

        // then
        expectThat(response.isSuccess).isTrue()
        verify(exactly = 1) { core.setDeviceId("device-123", null) }
    }

    @Test
    fun `SetDeviceIdInvocationHandler - deviceId가 없으면 예외를 던진다`() {
        val sut = SetDeviceIdInvocationHandler(core)
        assertThrows<IllegalStateException> {
            sut.invoke(request("setDeviceId", emptyMap()))
        }
    }

    // UserProperties

    @Test
    fun `SetUserPropertyInvocationHandler - 단일 프로퍼티를 설정한다`() {
        // given
        val sut = SetUserPropertyInvocationHandler(core)
        val params = mapOf<String, Any?>("key" to "name", "value" to "hackle")

        // when
        val response = sut.invoke(request("setUserProperty", params))

        // then
        expectThat(response.isSuccess).isTrue()
        verify(exactly = 1) { core.updateUserProperties(any<PropertyOperations>(), any<HackleAppContext>(), null) }
    }

    @Test
    fun `SetUserPropertyInvocationHandler - key가 없으면 예외를 던진다`() {
        val sut = SetUserPropertyInvocationHandler(core)
        assertThrows<IllegalStateException> {
            sut.invoke(request("setUserProperty", emptyMap()))
        }
    }

    @Test
    fun `UpdateUserPropertiesInvocationHandler - 여러 프로퍼티 연산을 처리한다`() {
        // given
        val sut = UpdateUserPropertiesInvocationHandler(core)
        val params = mapOf<String, Any?>(
            "operations" to mapOf(
                "\$set" to mapOf("key1" to "value1"),
                "\$setOnce" to mapOf("key2" to "value2")
            )
        )

        // when
        val response = sut.invoke(request("updateUserProperties", params))

        // then
        expectThat(response.isSuccess).isTrue()
        verify(exactly = 1) { core.updateUserProperties(any<PropertyOperations>(), any<HackleAppContext>(), null) }
    }

    @Test
    fun `UpdateUserPropertiesInvocationHandler - operations가 없으면 예외를 던진다`() {
        val sut = UpdateUserPropertiesInvocationHandler(core)
        assertThrows<IllegalStateException> {
            sut.invoke(request("updateUserProperties", emptyMap()))
        }
    }

    // PhoneNumber

    @Test
    fun `SetPhoneNumberInvocationHandler - 전화번호를 설정한다`() {
        // given
        val sut = SetPhoneNumberInvocationHandler(core)
        val params = mapOf<String, Any?>("phoneNumber" to "+8210-1234-5678")

        // when
        val response = sut.invoke(request("setPhoneNumber", params))

        // then
        expectThat(response.isSuccess).isTrue()
        verify(exactly = 1) { core.setPhoneNumber("+8210-1234-5678", any<HackleAppContext>(), null) }
    }

    @Test
    fun `SetPhoneNumberInvocationHandler - phoneNumber가 없으면 예외를 던진다`() {
        val sut = SetPhoneNumberInvocationHandler(core)
        assertThrows<IllegalStateException> {
            sut.invoke(request("setPhoneNumber", emptyMap()))
        }
    }

    @Test
    fun `UnsetPhoneNumberInvocationHandler - 전화번호를 해제한다`() {
        // given
        val sut = UnsetPhoneNumberInvocationHandler(core)

        // when
        val response = sut.invoke(request("unsetPhoneNumber"))

        // then
        expectThat(response.isSuccess).isTrue()
        verify(exactly = 1) { core.unsetPhoneNumber(any<HackleAppContext>(), null) }
    }

    // Subscriptions

    @Test
    fun `UpdatePushSubscriptionsInvocationHandler - push 구독을 업데이트한다`() {
        // given
        val sut = UpdatePushSubscriptionsInvocationHandler(core)
        val params = mapOf<String, Any?>(
            "operations" to mapOf(
                "\$marketing" to "SUBSCRIBED",
                "\$information" to "UNSUBSCRIBED"
            )
        )

        // when
        val response = sut.invoke(request("updatePushSubscriptions", params))

        // then
        expectThat(response.isSuccess).isTrue()
        verify(exactly = 1) {
            core.updatePushSubscriptions(
                withArg {
                    expectThat(it.asMap()) {
                        get { get("\$marketing") }.isEqualTo(HackleSubscriptionStatus.SUBSCRIBED)
                        get { get("\$information") }.isEqualTo(HackleSubscriptionStatus.UNSUBSCRIBED)
                    }
                },
                any<HackleAppContext>()
            )
        }
    }

    @Test
    fun `UpdateSmsSubscriptionsInvocationHandler - sms 구독을 업데이트한다`() {
        // given
        val sut = UpdateSmsSubscriptionsInvocationHandler(core)
        val params = mapOf<String, Any?>(
            "operations" to mapOf("\$marketing" to "SUBSCRIBED")
        )

        // when
        val response = sut.invoke(request("updateSmsSubscriptions", params))

        // then
        expectThat(response.isSuccess).isTrue()
        verify(exactly = 1) { core.updateSmsSubscriptions(any(), any<HackleAppContext>()) }
    }

    @Test
    fun `UpdateKakaoSubscriptionsInvocationHandler - kakao 구독을 업데이트한다`() {
        // given
        val sut = UpdateKakaoSubscriptionsInvocationHandler(core)
        val params = mapOf<String, Any?>(
            "operations" to mapOf("\$marketing" to "SUBSCRIBED")
        )

        // when
        val response = sut.invoke(request("updateKakaoSubscriptions", params))

        // then
        expectThat(response.isSuccess).isTrue()
        verify(exactly = 1) { core.updateKakaoSubscriptions(any(), any<HackleAppContext>()) }
    }

    @Test
    fun `UpdatePushSubscriptionsInvocationHandler - operations가 없으면 예외를 던진다`() {
        val sut = UpdatePushSubscriptionsInvocationHandler(core)
        assertThrows<IllegalStateException> {
            sut.invoke(request("updatePushSubscriptions", emptyMap()))
        }
    }

    // browserProperties 전파 검증

    @Test
    fun `ResetUserInvocationHandler - browserProperties가 HackleAppContext에 전달된다`() {
        // given
        val sut = ResetUserInvocationHandler(core)

        // when
        sut.invoke(request("resetUser"))

        // then
        verify(exactly = 1) {
            core.resetUser(
                withArg<HackleAppContext> {
                    expectThat(it.browserProperties["url"]).isEqualTo("https://hackle.io")
                },
                null
            )
        }
    }
}
