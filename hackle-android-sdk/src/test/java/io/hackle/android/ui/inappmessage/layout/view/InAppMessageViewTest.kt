package io.hackle.android.ui.inappmessage.layout.view

import android.app.Activity
import android.view.KeyEvent
import io.hackle.android.internal.inappmessage.present.presentation.InAppMessagePresentationContext
import io.hackle.android.support.InAppMessages
import io.hackle.android.ui.inappmessage.InAppMessageUi
import io.hackle.android.ui.inappmessage.layout.InAppMessageAnimator
import io.mockk.*
import org.junit.Before
import org.junit.Test

class InAppMessageViewTest {

    private lateinit var view: TestInAppMessageView
    private lateinit var controller: InAppMessageViewController
    private lateinit var activity: Activity
    private lateinit var context: InAppMessagePresentationContext

    @Before
    fun before() {
        activity = mockk(relaxed = true)
        controller = mockk(relaxed = true)
        context = InAppMessages.context()
        
        view = TestInAppMessageView(activity)
        view.setController(controller)
        view.setContext(context)
        view.setActivity(activity)
        
        mockkObject(InAppMessageUi)
        every { InAppMessageUi.instance } returns mockk(relaxed = true)
    }

    @Test
    fun `when back button is pressed and isBackButtonDismisses is true then controller close should be called`() {
        // given
        every { InAppMessageUi.instance.isBackButtonDismisses } returns true

        // when
        val result = view.onKeyDown(KeyEvent.KEYCODE_BACK, null)

        // then
        verify { controller.close() }
        assert(result)
    }

    @Test
    fun `when back button is pressed and isBackButtonDismisses is false then controller close should not be called`() {
        // given
        every { InAppMessageUi.instance.isBackButtonDismisses } returns false

        // when
        val result = view.onKeyDown(KeyEvent.KEYCODE_BACK, null)

        // then
        verify(exactly = 0) { controller.close() }
        assert(!result) // should call super.onKeyDown which returns false
    }

    @Test
    fun `when non-back button is pressed then controller close should not be called`() {
        // given
        every { InAppMessageUi.instance.isBackButtonDismisses } returns true

        // when
        val result = view.onKeyDown(KeyEvent.KEYCODE_HOME, null)

        // then
        verify(exactly = 0) { controller.close() }
        assert(!result) // should call super.onKeyDown which returns false
    }

    // Test implementation of InAppMessageView for testing purposes
    private class TestInAppMessageView(context: android.content.Context) : InAppMessageView(context) {
        override val openAnimator: InAppMessageAnimator? = null
        override val closeAnimator: InAppMessageAnimator? = null
        override fun configure() {}
    }
}