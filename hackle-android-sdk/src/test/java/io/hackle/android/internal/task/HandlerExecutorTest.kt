package io.hackle.android.internal.task

import android.os.Handler
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class HandlerExecutorTest {

    @Test
    fun `execute - handler 에게 전달한다`() {
        // given
        val handler = mockk<Handler>(relaxed = true)
        val sut = HandlerExecutor(handler)

        val command = mockk<Runnable>()

        // when
        sut.execute(command)

        // then
        verify(exactly = 1) { handler.post(command) }
    }
}