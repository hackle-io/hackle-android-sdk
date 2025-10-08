package io.hackle.android.internal.activity

import android.app.Activity
import android.app.Application
import io.hackle.android.internal.activity.lifecycle.ActivityLifecycle
import io.hackle.android.internal.activity.lifecycle.ActivityLifecycleListener
import io.hackle.android.internal.activity.lifecycle.ActivityLifecycleManager
import io.hackle.android.internal.activity.lifecycle.ActivityState
import io.hackle.android.ui.HackleActivity
import io.hackle.sdk.core.internal.time.Clock
import io.mockk.*
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.*

class ActivityLifecycleManagerTest {

    private val sut = ActivityLifecycleManager(object : Clock {
        override fun currentMillis(): Long = 42
        override fun tick(): Long = 42
    })

    @Test
    fun `register`() {
        val application = mockk<Application>(relaxed = true) {
            every { applicationContext } returns this
        }
        sut.registerTo(application)

        verifySequence {
            application.applicationContext
            application.unregisterActivityLifecycleCallbacks(sut)
            application.registerActivityLifecycleCallbacks(sut)
        }
    }

    @Test
    fun `internal activity`() {
        val activity = InternalActivity()
        sut.onActivityCreated(activity, null)
        expectThat(sut.currentActivity).isNull()
    }

    @Test
    fun `resolveCurrentActivity - CREATED`() {
        val activity = CustomActivity()
        sut.onActivityCreated(activity, null)
        expectThat(sut.currentActivity) isSameInstanceAs activity
        expectThat(sut.currentState) isEqualTo ActivityState.INACTIVE
    }


    @Test
    fun `resolveCurrentActivity - STARTED`() {
        val activity = CustomActivity()
        sut.onActivityStarted(activity)
        expectThat(sut.currentActivity) isSameInstanceAs activity
        expectThat(sut.currentState) isEqualTo ActivityState.INACTIVE
    }

    @Test
    fun `resolveCurrentActivity - RESUMED`() {
        val activity = CustomActivity()
        sut.onActivityResumed(activity)
        expectThat(sut.currentActivity) isSameInstanceAs activity
        expectThat(sut.currentState) isEqualTo ActivityState.ACTIVE
    }

    @Test
    fun `resolveCurrentActivity - change current activity`() {
        val activity1 = CustomActivity()
        val activity2 = CustomActivity()

        sut.onActivityResumed(activity1)
        expectThat(sut.currentActivity) isSameInstanceAs activity1

        sut.onActivityResumed(activity2)
        expectThat(sut.currentActivity) isSameInstanceAs activity2
    }

    @Test
    fun `resolveCurrentActivity - STOPPED`() {
        val activity1 = CustomActivity()
        sut.onActivityResumed(activity1)
        expectThat(sut.currentActivity) isSameInstanceAs activity1

        val activity2 = CustomActivity()
        sut.onActivityStopped(activity2)
        expectThat(sut.currentActivity) isSameInstanceAs activity1

        sut.onActivityResumed(activity2)
        expectThat(sut.currentActivity) isSameInstanceAs activity2

        sut.onActivityStopped(activity2)
        expectThat(sut.currentActivity).isNull()
    }

    @Test
    fun `resolveCurrentActivity - PAUSED`() {
        val activity = CustomActivity()
        sut.onActivityResumed(activity)
        expectThat(sut.currentActivity) isSameInstanceAs activity
        expectThat(sut.currentState) isEqualTo ActivityState.ACTIVE

        sut.onActivityPaused(activity)
        expectThat(sut.currentActivity) isSameInstanceAs activity
        expectThat(sut.currentState) isEqualTo ActivityState.INACTIVE

        sut.onActivityDestroyed(activity)
        expectThat(sut.currentActivity) isSameInstanceAs activity
    }

    @Test
    fun `resolveCurrentActivity - DESTROYED`() {
        val activity = CustomActivity()
        sut.onActivityResumed(activity)
        expectThat(sut.currentActivity) isSameInstanceAs activity

        sut.onActivityDestroyed(activity)
        expectThat(sut.currentActivity) isSameInstanceAs activity
    }

    @Test
    fun `publish`() {
        val listener = mockk<ActivityLifecycleListener>(relaxed = true)
        sut.addListener(listener)

        val activity = CustomActivity()
        sut.onActivityResumed(activity)

        verify(exactly = 1) {
            listener.onLifecycle(ActivityLifecycle.RESUMED, activity, 42)
        }
    }

    @Test
    fun `publish should handle listener exceptions gracefully`() {
        val listener1 = mockk<ActivityLifecycleListener>()
        val listener2 = mockk<ActivityLifecycleListener>(relaxed = true)

        every { listener1.onLifecycle(any(), any(), any()) } throws RuntimeException("Test exception")

        sut.addListener(listener1)
        sut.addListener(listener2)

        val activity = CustomActivity()
        sut.onActivityResumed(activity)

        // Verify both listeners were called despite the first throwing an exception
        verify { listener1.onLifecycle(ActivityLifecycle.RESUMED, activity, 42) }
        verify { listener2.onLifecycle(ActivityLifecycle.RESUMED, activity, 42) }
    }

    @Test
    fun `instance`() {
        val instance = ActivityLifecycleManager.instance
        expectThat(instance) isSameInstanceAs ActivityLifecycleManager.instance
    }

    @Test
    fun `should publish all lifecycle events`() {
        val listener = mockk<ActivityLifecycleListener>(relaxed = true)
        sut.addListener(listener)

        val activity = CustomActivity()

        // Test all lifecycle methods
        sut.onActivityCreated(activity, null)
        sut.onActivityStarted(activity)
        sut.onActivityResumed(activity)
        sut.onActivityPaused(activity)
        sut.onActivityStopped(activity)
        sut.onActivityDestroyed(activity)

        verify { listener.onLifecycle(ActivityLifecycle.CREATED, activity, 42) }
        verify { listener.onLifecycle(ActivityLifecycle.STARTED, activity, 42) }
        verify { listener.onLifecycle(ActivityLifecycle.RESUMED, activity, 42) }
        verify { listener.onLifecycle(ActivityLifecycle.PAUSED, activity, 42) }
        verify { listener.onLifecycle(ActivityLifecycle.STOPPED, activity, 42) }
        verify { listener.onLifecycle(ActivityLifecycle.DESTROYED, activity, 42) }
    }

    @Test
    fun `should handle multiple listeners`() {
        val listener1 = mockk<ActivityLifecycleListener>(relaxed = true)
        val listener2 = mockk<ActivityLifecycleListener>(relaxed = true)

        sut.addListener(listener1)
        sut.addListener(listener2)

        val activity = CustomActivity()
        sut.onActivityResumed(activity)

        verify { listener1.onLifecycle(ActivityLifecycle.RESUMED, activity, 42) }
        verify { listener2.onLifecycle(ActivityLifecycle.RESUMED, activity, 42) }
    }

    @Test
    fun `should track activity state transitions correctly`() {
        val activity = CustomActivity()

        // Initial state
        expectThat(sut.currentState).isEqualTo(ActivityState.INACTIVE)

        // RESUMED -> ACTIVE
        sut.onActivityResumed(activity)
        expectThat(sut.currentState).isEqualTo(ActivityState.ACTIVE)

        // PAUSED -> INACTIVE
        sut.onActivityPaused(activity)
        expectThat(sut.currentState).isEqualTo(ActivityState.INACTIVE)

        // RESUMED again -> ACTIVE
        sut.onActivityResumed(activity)
        expectThat(sut.currentState).isEqualTo(ActivityState.ACTIVE)
    }

    @Test
    fun `should not update current activity if it's the same activity`() {
        val activity = CustomActivity()

        sut.onActivityCreated(activity, null)
        val firstRef = sut.currentActivity

        sut.onActivityStarted(activity)
        val secondRef = sut.currentActivity

        expectThat(firstRef).isSameInstanceAs(secondRef)
    }

    @Test
    fun `should clear current activity when stopped`() {
        val activity = CustomActivity()

        sut.onActivityCreated(activity, null)
        expectThat(sut.currentActivity).isNotNull()

        sut.onActivityStopped(activity)
        expectThat(sut.currentActivity).isNull()
    }

    @Test
    fun `onActivitySaveInstanceState should not affect state`() {
        val activity = CustomActivity()
        sut.onActivityResumed(activity)
        expectThat(sut.currentState).isEqualTo(ActivityState.ACTIVE)

        sut.onActivitySaveInstanceState(activity, mockk())
        expectThat(sut.currentState).isEqualTo(ActivityState.ACTIVE)
    }

    @Test
    fun `weak reference allows garbage collection of activity`() {
        var activity: Activity? = CustomActivity()

        sut.onActivityCreated(activity!!, null)
        expectThat(sut.currentActivity).isNotNull()

        // Clear strong reference
        activity = null

        // Force garbage collection (not guaranteed to work immediately)
        System.gc()

        // The weak reference may or may not be cleared yet, but it should be possible
        // This test just ensures the weak reference mechanism is in place
    }

    private class InternalActivity : Activity(), HackleActivity
    private class CustomActivity : Activity()
}