package io.hackle.android.ui.inappmessage

import android.app.Activity
import io.hackle.android.internal.activity.lifecycle.ActivityLifecycle
import io.hackle.android.internal.activity.lifecycle.ActivityProvider
import io.hackle.android.internal.task.TaskExecutors
import io.hackle.android.support.InAppMessages
import io.hackle.android.ui.core.ImageLoader
import io.hackle.android.ui.inappmessage.event.InAppMessageEventHandler
import io.hackle.android.ui.inappmessage.layout.InAppMessageLayout
import io.hackle.android.ui.inappmessage.layout.activity.InAppMessageActivity
import io.hackle.sdk.common.HackleInAppMessageListener
import io.hackle.sdk.core.internal.scheduler.Scheduler
import io.hackle.sdk.core.model.InAppMessage
import io.mockk.*
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CyclicBarrier
import kotlin.concurrent.thread

class InAppMessageUiTest {

    @RelaxedMockK
    private lateinit var activityProvider: ActivityProvider

    @RelaxedMockK
    private lateinit var messageControllerFactory: InAppMessageControllerFactory

    @RelaxedMockK
    private lateinit var defaultListener: HackleInAppMessageListener

    @RelaxedMockK
    private lateinit var scheduler: Scheduler

    @RelaxedMockK
    private lateinit var eventHandler: InAppMessageEventHandler

    @RelaxedMockK
    private lateinit var imageLoader: ImageLoader

    @InjectMockKs
    private lateinit var sut: InAppMessageUi

    private lateinit var activity: Activity
    private lateinit var controller: InAppMessageController

    @Before
    fun before() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        mockkObject(TaskExecutors)
        mockkStatic("io.hackle.android.ui.inappmessage.InAppMessageExtensionsKt")
        every { TaskExecutors.runOnUiThread(any()) } answers { firstArg<() -> Unit>()() }
        activity = mockk {
            every { orientation } returns InAppMessage.Orientation.VERTICAL
        }
        controller = mockk(relaxUnitFun = true)

        every { activityProvider.currentActivity } returns activity
        every { messageControllerFactory.create(any(), any(), any()) } returns controller
    }

    @Test
    fun `when current activity is null then should not open`() {
        // given
        every { activityProvider.currentActivity } returns null

        // when
        sut.present(InAppMessages.context())

        // then
        verify { controller wasNot Called }
    }


    @Test
    fun `when message is already presented then should not open again`() {

        val context = InAppMessages.context()
        sut.present(context)
        sut.present(context)
        sut.present(context)

        verify(exactly = 1) {
            controller.open(any())
        }
    }

    @Test
    fun `when not supported orientation then should not open`() {
        // given
        val context = InAppMessages.context()
        every { activity.orientation } returns InAppMessage.Orientation.HORIZONTAL

        // when
        sut.present(context)

        // then
        verify { controller wasNot Called }
    }

    @Test
    fun `only one InAppMessage is presented at a time`() {
        val context = InAppMessages.context()

        val barrier = CyclicBarrier(32)
        val jobs = List(32) {
            thread {
                barrier.await()
                sut.present(context)
            }
        }

        jobs.forEach { it.join() }

        verify(exactly = 1) {
            controller.open(any())
        }
    }

    @Test
    fun `when exception occurs while opening then close`() {
        // given
        val context = InAppMessages.context()
        every { controller.open(any()) } throws IllegalArgumentException("fail")

        // when
        sut.present(context)

        // then
        verify {
            controller.close()
        }
    }

    @Test
    fun `open view`() {
        // given
        val context = InAppMessages.context()

        // when
        sut.present(context)

        // then
        verify {
            controller.open(activity)
        }
    }

    @Test
    fun `when setBackButtonDismisses is called with true then isBackButtonDismisses should return true`() {
        // when
        sut.setBackButtonDismisses(true)

        // then
        assert(sut.isBackButtonDismisses)
    }

    @Test
    fun `when setBackButtonDismisses is called with false then isBackButtonDismisses should return false`() {
        // when
        sut.setBackButtonDismisses(false)

        // then
        assert(!sut.isBackButtonDismisses)
    }

    @Test
    fun `default value of isBackButtonDismisses should be true`() {
        // then
        assert(sut.isBackButtonDismisses)
    }

    @Test
    fun `when InAppMessageActivity destroyed then do not close`() {
        // given
        val context = InAppMessages.context()
        sut.present(context)
        val inAppMessageActivity = mockk<InAppMessageActivity>()
        val layout = mockk<InAppMessageLayout> {
            every { this@mockk.activity } returns inAppMessageActivity
        }
        every { controller.layout } returns layout

        // when
        sut.onLifecycle(ActivityLifecycle.DESTROYED, inAppMessageActivity, System.currentTimeMillis())

        // then
        verify(exactly = 0) { controller.close(any()) }
    }

    @Test
    fun `when controller layout activity is null then do close`() {
        // given
        val context = InAppMessages.context()
        sut.present(context)
        val layout = mockk<InAppMessageLayout> {
            every { this@mockk.activity } returns null
        }
        every { controller.layout } returns layout

        // when
        sut.onLifecycle(ActivityLifecycle.DESTROYED, activity, System.currentTimeMillis())

        // then
        verify(exactly = 1) { controller.close(true) }
    }

    @Test
    fun `when controller layout activity is this activity then do close`() {
        // given
        val context = InAppMessages.context()
        val mockkActivity = mockk<Activity>()
        sut.present(context)
        val layout = mockk<InAppMessageLayout> {
            every { this@mockk.activity } returns mockkActivity
        }
        every { controller.layout } returns layout

        // when
        sut.onLifecycle(ActivityLifecycle.DESTROYED, mockkActivity, System.currentTimeMillis())

        // then
        verify(exactly = 1) { controller.close(true) }
    }

    @Test
    fun `when currentMessageController is null then do not close`() {
        // given - no message presented

        // when
        sut.onLifecycle(ActivityLifecycle.DESTROYED, activity, System.currentTimeMillis())

        // then
        verify(exactly = 0) { controller.close(any()) }
    }
}
