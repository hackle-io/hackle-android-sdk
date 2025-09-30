package io.hackle.android.internal.lifecycle

import android.app.Activity
import android.app.Application
import io.hackle.android.internal.activity.ActivityLifecycle
import io.hackle.android.internal.activity.ActivityLifecycleListener
import io.hackle.android.internal.activity.ActivityLifecycleManager
import io.hackle.android.internal.activity.ActivityState
import io.hackle.android.ui.HackleActivity
import io.hackle.sdk.core.internal.time.Clock
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifySequence
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.any
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isNull
import strikt.assertions.isSameInstanceAs

class ActivityLifecycleManager {

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
    fun `instance`() {
        val instance = ActivityLifecycleManager.instance
        expectThat(instance) isSameInstanceAs ActivityLifecycleManager.instance
    }

    private class InternalActivity : Activity(), HackleActivity
    private class CustomActivity : Activity()
}
