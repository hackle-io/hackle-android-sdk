package io.hackle.android.internal.inappmessage.present

import io.hackle.android.internal.inappmessage.present.presentation.InAppMessagePresenter
import io.hackle.android.internal.inappmessage.present.record.InAppMessageRecorder
import io.hackle.android.support.InAppMessages
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class InAppMessagePresentProcessorTest {


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
        val request = InAppMessages.presentRequest(
            dispatchId = "111"
        )

        // when
        val actual = sut.process(request)

        // then
        expectThat(actual) {
            get { this.dispatchId } isEqualTo "111"
            get { this.context.dispatchId } isEqualTo "111"
        }
        verify(exactly = 1) {
            presenter.present(any())
        }
        verify(exactly = 1) {
            recorder.record(request, actual)
        }
    }
}
