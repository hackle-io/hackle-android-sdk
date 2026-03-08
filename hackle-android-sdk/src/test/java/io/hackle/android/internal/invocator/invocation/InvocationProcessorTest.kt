package io.hackle.android.internal.invocator.invocation

import io.hackle.android.internal.HackleAppCore
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNull
import strikt.assertions.isTrue

class InvocationProcessorTest {

    private lateinit var core: HackleAppCore
    private lateinit var sut: InvocationProcessor

    @Before
    fun setup() {
        core = mockk(relaxUnitFun = true)
        val factory = InvocationHandlerFactory(core)
        sut = InvocationProcessor(factory)
    }

    @Test
    fun `process - handler가 성공 응답을 반환하면 그대로 반환한다`() {
        // given
        every { core.sessionId } returns "test-session"
        val request = InvocationRequest.parse("""{"_hackle":{"command":"getSessionId"}}""")

        // when
        val response = sut.process(request)

        // then
        expectThat(response) {
            get { isSuccess }.isTrue()
            get { message }.isEqualTo("OK")
            get { data }.isEqualTo("test-session")
        }
    }

    @Test
    fun `process - handler에서 예외가 발생하면 실패 응답을 반환한다`() {
        // given
        val request = InvocationRequest.parse(
            """{"_hackle":{"command":"setDeviceId","parameters":{}}}"""
        )

        // when
        val response = sut.process(request)

        // then
        expectThat(response) {
            get { isSuccess }.isFalse()
            get { data }.isNull()
        }
    }

    @Test
    fun `process - 파라미터 없는 command도 정상 처리한다`() {
        // given
        every { core.showUserExplorer() } returns Unit
        val request = InvocationRequest.parse("""{"_hackle":{"command":"showUserExplorer"}}""")

        // when
        val response = sut.process(request)

        // then
        expectThat(response.isSuccess).isTrue()
    }
}
