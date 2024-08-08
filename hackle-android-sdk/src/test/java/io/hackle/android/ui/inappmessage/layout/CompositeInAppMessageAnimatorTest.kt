package io.hackle.android.ui.inappmessage.layout

import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.all
import strikt.assertions.isSameInstanceAs

class CompositeInAppMessageAnimatorTest {

    @Test
    fun `set listener on all animators`() {
        val animators = listOf(
            InAppMessageAnimatorStub(),
            InAppMessageAnimatorStub(),
            InAppMessageAnimatorStub()
        )
        val sut = CompositeInAppMessageAnimator(animators)

        expectThat(animators).all {
            get { listener } isSameInstanceAs sut
        }
    }

    @Test
    fun `start`() {
        val animators = listOf(
            InAppMessageAnimatorStub(),
            InAppMessageAnimatorStub(),
            InAppMessageAnimatorStub()
        )

        val listener = mockk<InAppMessageAnimator.Listener>(relaxed = true)
        val sut = CompositeInAppMessageAnimator(animators)
        sut.setListener(listener)

        sut.start()

        verify(exactly = 1) {
            listener.onAnimationEnd()
        }
    }

    private class InAppMessageAnimatorStub : InAppMessageAnimator {
        var listener: InAppMessageAnimator.Listener? = null
            private set

        override fun start() {
            listener?.onAnimationEnd()
        }

        override fun setListener(listener: InAppMessageAnimator.Listener?) {
            this.listener = listener
        }
    }
}