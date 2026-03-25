package io.hackle.android.internal.invocator.invocation

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isFalse
import strikt.assertions.isTrue

internal class InvocationProcessorTest {


    @MockK
    private lateinit var handlerFactory: InvocationHandlerFactory

    @RelaxedMockK
    private lateinit var handler: InvocationHandler<Any>

    @InjectMockKs
    private lateinit var sut: InvocationProcessor

    @Before
    fun before() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        every { handlerFactory.get(any()) } returns handler
        every { handler.invoke(any()) } returns InvocationResponse.success()
    }

    @Test
    fun `process request with handler`() {
        // given
        val request = mockk<InvocationRequest>(relaxed = true)

        // when
        val actual = sut.process(request)

        // then
        expectThat(actual.isSuccess).isTrue()
        verify(exactly = 1) {
            handler.invoke(any())
        }
    }

    @Test
    fun `when failed to process invocation then return failed response`() {
        // given
        val request = mockk<InvocationRequest>(relaxed = true)
        every { handler.invoke(any()) } throws IllegalArgumentException("failed")

        // when
        val actual = sut.process(request)

        // then
        expectThat(actual.isSuccess).isFalse()
    }
}
