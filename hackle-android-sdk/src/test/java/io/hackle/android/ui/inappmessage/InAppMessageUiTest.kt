package io.hackle.android.ui.inappmessage

import android.app.Activity
import io.hackle.android.internal.HackleActivityManager
import io.hackle.android.internal.task.TaskExecutors
import io.hackle.android.support.InAppMessages
import io.hackle.android.ui.inappmessage.event.InAppMessageEventHandler
import io.hackle.android.ui.inappmessage.view.InAppMessageView
import io.hackle.android.ui.inappmessage.view.InAppMessageViewFactory
import io.hackle.sdk.core.model.InAppMessage
import io.mockk.Called
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CyclicBarrier
import kotlin.concurrent.thread

class InAppMessageUiTest {

    @RelaxedMockK
    private lateinit var hackleActivityManager: HackleActivityManager

    @RelaxedMockK
    private lateinit var messageViewFactory: InAppMessageViewFactory

    @RelaxedMockK
    private lateinit var eventHandler: InAppMessageEventHandler

    @InjectMockKs
    private lateinit var sut: InAppMessageUi

    private lateinit var activity: Activity
    private lateinit var view: InAppMessageView

    @Before
    fun before() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        mockkObject(TaskExecutors)
        mockkStatic("io.hackle.android.ui.inappmessage.InAppMessageExtensionsKt")
        every { TaskExecutors.runOnUiThread(any()) } answers { firstArg<() -> Unit>()() }
        activity = mockk() {
            every { orientation } returns InAppMessage.Orientation.VERTICAL
        }
        view = mockk(relaxUnitFun = true)

        every { hackleActivityManager.currentActivity } returns activity
        every { messageViewFactory.create(any(), any()) } returns view
    }

    @Test
    fun `when current activity is null then should not open`() {
        // given
        every { hackleActivityManager.currentActivity } returns null

        // when
        sut.present(InAppMessages.context())

        // then
        verify { view wasNot Called }
    }


    @Test
    fun `when view is already presented then should not open again`() {

        val context = InAppMessages.context()
        sut.present(context)
        sut.present(context)
        sut.present(context)

        verify(exactly = 1) {
            view.open(any())
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
        verify { view wasNot Called }
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
            view.open(any())
        }
    }

    @Test
    fun `when exception occurs while opening then close`() {
        // given
        val context = InAppMessages.context()
        every { view.open(any()) } throws IllegalArgumentException("fail")

        // when
        sut.present(context)

        // then
        verify {
            view.close()
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
            view.open(activity)
        }
    }
}
