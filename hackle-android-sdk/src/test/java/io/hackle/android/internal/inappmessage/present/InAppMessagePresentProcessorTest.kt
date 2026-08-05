package io.hackle.android.internal.inappmessage.present

import io.hackle.android.internal.inappmessage.present.InAppMessagePresentResponse.Code
import io.hackle.android.internal.inappmessage.present.presentation.InAppMessagePresenter
import io.hackle.android.internal.inappmessage.present.record.InAppMessageRecorder
import io.hackle.android.support.InAppMessages
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.slot
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isSameInstanceAs
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

class InAppMessagePresentProcessorTest {

    @MockK
    private lateinit var coreExecutor: Executor

    @MockK
    private lateinit var presenter: InAppMessagePresenter

    @MockK
    private lateinit var recorder: InAppMessageRecorder

    @InjectMockKs
    private lateinit var sut: InAppMessagePresentProcessor

    @Before
    fun before() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        every { coreExecutor.execute(any()) } answers { firstArg<Runnable>().run() }
    }

    @Test
    fun `process - request로 생성한 context로 present하고 결과를 record한다`() {
        // given
        val request = InAppMessages.presentRequest(dispatchId = "111")
        val response = InAppMessagePresentResponse.of(Code.PRESENT, InAppMessages.context(dispatchId = "111"))
        every { presenter.present(any()) } returns CompletableFuture.completedFuture(response)

        // when
        val actual = sut.process(request).get()

        // then
        expectThat(actual) isSameInstanceAs response
        verify(exactly = 1) {
            presenter.present(withArg {
                expectThat(it.dispatchId) isEqualTo request.dispatchId
                expectThat(it.inAppMessage) isSameInstanceAs request.inAppMessage
                expectThat(it.message) isSameInstanceAs request.message
            })
        }
        verify(exactly = 1) {
            recorder.record(request, response)
        }
    }

    @Test
    fun `process - record는 coreExecutor 에서 실행된다`() {
        // given
        val request = InAppMessages.presentRequest(dispatchId = "111")
        val response = InAppMessagePresentResponse.of(Code.PRESENT, InAppMessages.context(dispatchId = "111"))
        every { presenter.present(any()) } returns CompletableFuture.completedFuture(response)

        val task = slot<Runnable>()
        every { coreExecutor.execute(capture(task)) } returns Unit

        // when
        val actual = sut.process(request)

        // then - coreExecutor 실행 전에는 record되지 않고 future도 완료되지 않는다 (완료 스레드에서 inline 실행 금지)
        expectThat(actual.isDone) isEqualTo false
        verify(exactly = 0) {
            recorder.record(any(), any())
        }

        task.captured.run()
        expectThat(actual.isDone) isEqualTo true
        verify(exactly = 1) {
            recorder.record(request, response)
        }
    }

    @Test
    fun `process - present에 실패하면 record하지 않는다`() {
        // given
        val request = InAppMessages.presentRequest(dispatchId = "111")
        every { presenter.present(any()) } throws IllegalArgumentException("fail")

        // when
        val actual = sut.process(request)

        // then
        expectThat(actual.isCompletedExceptionally) isEqualTo true
        verify(exactly = 0) {
            recorder.record(any(), any())
        }
    }
}
