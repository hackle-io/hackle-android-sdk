package io.hackle.android.internal.inappmessage.present

import io.hackle.android.internal.inappmessage.present.presentation.InAppMessagePresentationContext
import io.hackle.android.internal.inappmessage.present.presentation.InAppMessagePresentationContextResolver
import io.hackle.android.internal.inappmessage.present.presentation.InAppMessagePresenter
import io.hackle.android.internal.inappmessage.present.record.InAppMessageRecorder
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
import strikt.assertions.isEqualTo

class InAppMessagePresentProcessorTest {

    @MockK
    private lateinit var contextResolver: InAppMessagePresentationContextResolver

    @RelaxedMockK
    private lateinit var presenter: InAppMessagePresenter

    @RelaxedMockK
    private lateinit var recorder: InAppMessageRecorder

    @InjectMockKs
    private lateinit var sut: InAppMessagePresentProcessor

    @Before
    fun before() {
        MockKAnnotations.init(this, relaxUnitFun = true)
    }

    @Test
    fun `present`() {
        // given
        val request = mockk<InAppMessagePresentRequest> {
            every { dispatchId } returns "111"
        }
        val context = mockk<InAppMessagePresentationContext>()
        every { contextResolver.resolve(request) } returns context

        // when
        val actual = sut.process(request)

        // then
        expectThat(actual) {
            get { this.dispatchId } isEqualTo "111"
            get { this.context } isEqualTo context
        }
        verify(exactly = 1) {
            presenter.present(context)
        }
        verify(exactly = 1) {
            recorder.record(request, actual)
        }
    }
}
