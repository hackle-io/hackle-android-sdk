package io.hackle.android.internal.invocator.invocation.handlers

import com.google.gson.GsonBuilder
import io.hackle.android.internal.HackleAppCore
import io.hackle.android.internal.invocator.invocation.InvocationRequest
import io.hackle.android.support.assertThrows
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isNull
import strikt.assertions.isTrue

class ConfigurationInvocationHandlersTest {

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

    @Test
    fun `SetOptOutTrackingInvocationHandler - optOut을 true로 설정한다`() {
        // given
        val sut = SetOptOutTrackingInvocationHandler(core)
        val params = mapOf<String, Any?>("optOut" to true)

        // when
        val response = sut.invoke(request("setOptOutTracking", params))

        // then
        expectThat(response) {
            get { isSuccess }.isTrue()
            get { data }.isNull()
        }
        verify(exactly = 1) { core.setOptOutTracking(true) }
    }

    @Test
    fun `SetOptOutTrackingInvocationHandler - optOut을 false로 설정한다`() {
        // given
        val sut = SetOptOutTrackingInvocationHandler(core)
        val params = mapOf<String, Any?>("optOut" to false)

        // when
        val response = sut.invoke(request("setOptOutTracking", params))

        // then
        expectThat(response) {
            get { isSuccess }.isTrue()
            get { data }.isNull()
        }
        verify(exactly = 1) { core.setOptOutTracking(false) }
    }

    @Test
    fun `SetOptOutTrackingInvocationHandler - optOut이 없으면 예외를 던진다`() {
        val sut = SetOptOutTrackingInvocationHandler(core)
        assertThrows<IllegalStateException> {
            sut.invoke(request("setOptOutTracking", emptyMap()))
        }
    }
}
