package io.hackle.android.internal.invocator.invocation.handlers

import com.google.gson.GsonBuilder
import io.hackle.android.internal.HackleAppCore
import io.hackle.android.internal.context.HackleAppContext
import io.hackle.android.internal.invocator.invocation.InvocationRequest
import io.hackle.android.support.assertThrows
import io.hackle.sdk.common.Event
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNull
import strikt.assertions.isTrue

class EventInvocationHandlersTest {

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
    fun `TrackInvocationHandler - event 문자열로 이벤트를 전송한다`() {
        // given
        val sut = TrackInvocationHandler(core)
        val params = mapOf<String, Any?>("event" to "purchase")

        // when
        val response = sut.invoke(request("track", params))

        // then
        expectThat(response) {
            get { isSuccess }.isTrue()
            get { data }.isNull()
        }
        verify(exactly = 1) {
            core.track(
                withArg<Event> { expectThat(it).isEqualTo(Event.of("purchase")) },
                any<HackleAppContext>()
            )
        }
    }

    @Test
    fun `TrackInvocationHandler - event 객체로 이벤트를 전송한다`() {
        // given
        val sut = TrackInvocationHandler(core)
        val params = mapOf<String, Any?>(
            "event" to mapOf(
                "key" to "purchase",
                "value" to 99.9,
                "properties" to mapOf("item" to "shirt")
            )
        )

        // when
        val response = sut.invoke(request("track", params))

        // then
        expectThat(response.isSuccess).isTrue()
        verify(exactly = 1) {
            core.track(
                withArg<Event> {
                    expectThat(it) {
                        get { key }.isEqualTo("purchase")
                        get { value }.isEqualTo(99.9)
                        get { properties["item"] }.isEqualTo("shirt")
                    }
                },
                any<HackleAppContext>()
            )
        }
    }

    @Test
    fun `TrackInvocationHandler - user 파라미터는 무시된다`() {
        // given
        val sut = TrackInvocationHandler(core)
        val params = mapOf<String, Any?>("event" to "click", "user" to "user-abc")

        // when
        sut.invoke(request("track", params))

        // then
        verify(exactly = 1) {
            core.track(any<Event>(), any<HackleAppContext>())
        }
    }

    @Test
    fun `TrackInvocationHandler - browserProperties가 context에 전달된다`() {
        // given
        val sut = TrackInvocationHandler(core)
        val params = mapOf<String, Any?>("event" to "view")

        // when
        sut.invoke(request("track", params))

        // then
        verify(exactly = 1) {
            core.track(
                any<Event>(),
                withArg<HackleAppContext> {
                    expectThat(it.browserProperties["url"]).isEqualTo("https://hackle.io")
                }
            )
        }
    }

    @Test
    fun `TrackInvocationHandler - event가 없으면 예외를 던진다`() {
        val sut = TrackInvocationHandler(core)
        assertThrows<IllegalStateException> {
            sut.invoke(request("track", emptyMap()))
        }
    }
}
