package io.hackle.android.ui.inappmessage.view

import android.app.Activity
import android.content.Context
import io.hackle.android.internal.inappmessage.present.presentation.InAppMessagePresentationContext
import io.hackle.android.internal.task.TaskExecutors
import io.hackle.android.support.InAppMessages
import io.hackle.android.ui.inappmessage.InAppMessageLifecycle
import io.hackle.android.ui.inappmessage.InAppMessageUi
import io.hackle.sdk.core.internal.scheduler.ScheduledJob
import io.hackle.sdk.core.internal.scheduler.Scheduler
import io.hackle.sdk.core.model.InAppMessage
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isFalse
import java.util.concurrent.TimeUnit

internal class InAppMessageViewControllerTest {

    private lateinit var scheduler: TestScheduler
    private lateinit var ui: InAppMessageUi
    private lateinit var activity: Activity

    @Before
    fun before() {
        mockkObject(TaskExecutors)
        every { TaskExecutors.runOnUiThread(any()) } answers { firstArg<() -> Unit>()() }

        scheduler = TestScheduler()
        ui = mockk(relaxed = true)
        every { ui.scheduler } returns scheduler

        activity = mockk(relaxed = true)
    }

    @After
    fun after() {
        unmockkObject(TaskExecutors)
    }

    @Test
    fun `open does not schedule opening timeout`() {
        val view = TestInAppMessageView(activity, context(displayType = InAppMessage.DisplayType.HTML))
        val sut = InAppMessageViewController(view, ui)
        view.setController(sut)

        sut.open(activity)

        expectThat(scheduler.hasScheduledTask).isFalse()
    }

    private fun context(displayType: InAppMessage.DisplayType): InAppMessagePresentationContext {
        val message = InAppMessages.message(
            layout = InAppMessages.layout(
                displayType = displayType,
                layoutType = if (displayType == InAppMessage.DisplayType.HTML) {
                    InAppMessage.LayoutType.NONE
                } else {
                    InAppMessage.LayoutType.IMAGE_ONLY
                }
            ),
            html = if (displayType == InAppMessage.DisplayType.HTML) {
                InAppMessage.Message.Html.TextHtml("<html></html>")
            } else {
                null
            }
        )
        val inAppMessage = InAppMessages.create(
            messageContext = InAppMessages.messageContext(messages = listOf(message))
        )
        return InAppMessages.context(inAppMessage = inAppMessage, message = message)
    }

    private class TestInAppMessageView(
        context: Context,
        presentationContext: InAppMessagePresentationContext,
    ) : InAppMessageBaseView(context), InAppMessageView.LifecycleListener {

        val lifecycles = mutableListOf<InAppMessageLifecycle>()

        override val openAnimator: InAppMessageAnimator? = null
        override val closeAnimator: InAppMessageAnimator? = null

        init {
            setPresentationContext(presentationContext)
        }

        override fun onConfigure(listener: InAppMessageView.ReadyListener) {
        }

        override fun beforeInAppMessageOpen() {
            lifecycles.add(InAppMessageLifecycle.BEFORE_OPEN)
            throw IllegalStateException("stop after lifecycle")
        }
    }

    private class TestScheduler : Scheduler {
        private var scheduledTask: (() -> Unit)? = null
        val hasScheduledTask: Boolean get() = scheduledTask != null

        override fun schedule(delay: Long, unit: TimeUnit, task: () -> Unit): ScheduledJob {
            scheduledTask = task
            return TestScheduledJob()
        }

        override fun schedulePeriodically(
            delay: Long,
            period: Long,
            unit: TimeUnit,
            task: () -> Unit
        ): ScheduledJob {
            throw UnsupportedOperationException()
        }
    }

    private class TestScheduledJob : ScheduledJob {
        override fun cancel() {
        }
    }
}
