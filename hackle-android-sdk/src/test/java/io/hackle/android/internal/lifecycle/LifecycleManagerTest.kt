package io.hackle.android.internal.lifecycle

import android.app.Activity
import io.hackle.android.internal.lifecycle.LifecycleManager.*
import io.mockk.Called
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifySequence
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class LifecycleManagerTest {

    private lateinit var lifecycleManager: LifecycleManager

    @Before
    fun setup() {
        lifecycleManager = LifecycleManager()
    }

    @Test
    fun `receive latest lifecycle event right after dispatch start`() {
        val listener = mockk<LifecycleEventListener>()
        lifecycleManager.addEventListener(listener)

        lifecycleManager.onActivityCreated(mockk(), mockk())
        lifecycleManager.onActivityStarted(mockk())
        lifecycleManager.onActivityResumed(mockk())

        verify { listener wasNot Called }

        val timeInMillis = 12345L
        lifecycleManager.dispatchStart(timeInMillis = timeInMillis)

        verify(exactly = 1) {
            listener.onEvent(LifecycleEvent.ON_RESUME, timeInMillis)
        }
    }

    @Test
    fun `should receive on event once even dispatch start multiple times`() {
        val listener = mockk<LifecycleEventListener>()
        lifecycleManager.addEventListener(listener)

        lifecycleManager.onActivityCreated(mockk(), mockk())
        lifecycleManager.onActivityStarted(mockk())
        lifecycleManager.onActivityResumed(mockk())

        verify { listener wasNot Called }

        val timeInMillis = 12345L
        lifecycleManager.dispatchStart(timeInMillis = timeInMillis)
        lifecycleManager.dispatchStart()
        lifecycleManager.dispatchStart()
        lifecycleManager.dispatchStart()

        verify(exactly = 1) {
            listener.onEvent(LifecycleEvent.ON_RESUME, timeInMillis)
        }
    }

    @Test
    fun `receive sequential lifecycle event`() {
        val listener = mockk<LifecycleEventListener>()
        lifecycleManager.dispatchStart()
        lifecycleManager.addEventListener(listener)

        lifecycleManager.onActivityCreated(mockk(), mockk())
        lifecycleManager.onActivityStarted(mockk())
        lifecycleManager.onActivityResumed(mockk())
        lifecycleManager.onActivityPaused(mockk())
        lifecycleManager.onActivityStopped(mockk())
        lifecycleManager.onActivityDestroyed(mockk())

        verifySequence {
            listener.onEvent(LifecycleEvent.ON_CREATE, any())
            listener.onEvent(LifecycleEvent.ON_START, any())
            listener.onEvent(LifecycleEvent.ON_RESUME, any())
            listener.onEvent(LifecycleEvent.ON_PAUSE, any())
            listener.onEvent(LifecycleEvent.ON_STOP, any())
            listener.onEvent(LifecycleEvent.ON_DESTROY, any())
        }
    }

    @Test
    fun `current activity should not null after activity created`() {
        assertNull(lifecycleManager.currentActivity)

        val activity = mockk<Activity>()
        lifecycleManager.onActivityCreated(activity, mockk())

        assertThat(lifecycleManager.currentActivity, `is`(activity))
    }

    @Test
    fun `current activity should not null after activity started`() {
        assertNull(lifecycleManager.currentActivity)

        val activity = mockk<Activity>()
        lifecycleManager.onActivityCreated(activity, mockk())
        lifecycleManager.onActivityStarted(activity)

        assertThat(lifecycleManager.currentActivity, `is`(activity))
    }

    @Test
    fun `current activity should not null after activity resumed`() {
        assertNull(lifecycleManager.currentActivity)

        val activity = mockk<Activity>()
        lifecycleManager.onActivityCreated(activity, mockk())
        lifecycleManager.onActivityStarted(activity)
        lifecycleManager.onActivityResumed(activity)

        assertThat(lifecycleManager.currentActivity, `is`(activity))
    }

    @Test
    fun `change current activity after activity transition`() {
        assertNull(lifecycleManager.currentActivity)

        val firstActivity = mockk<Activity>()
        lifecycleManager.onActivityCreated(firstActivity, mockk())
        lifecycleManager.onActivityStarted(firstActivity)
        lifecycleManager.onActivityResumed(firstActivity)

        assertThat(lifecycleManager.currentActivity, `is`(firstActivity))

        val secondActivity = mockk<Activity>()
        lifecycleManager.onActivityPaused(firstActivity)
        lifecycleManager.onActivityCreated(secondActivity, mockk())
        lifecycleManager.onActivityStarted(secondActivity)
        lifecycleManager.onActivityResumed(secondActivity)
        lifecycleManager.onActivityStopped(firstActivity)

        assertThat(lifecycleManager.currentActivity, `is`(secondActivity))
    }
}
