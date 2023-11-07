package io.hackle.android.internal.lifecycle

import android.app.Activity
import android.os.Bundle
import io.hackle.android.internal.lifecycle.LifecycleManager.LifecycleState
import io.hackle.android.internal.lifecycle.LifecycleManager.LifecycleStateListener
import io.mockk.Called
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifySequence
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.util.Stack

internal class LifecycleManagerTest {

    private lateinit var lifecycleManager: LifecycleManager
    private lateinit var lifecyclePlayer: LifecyclePlayer

    @Before
    fun setup() {
        lifecycleManager = LifecycleManager()
        lifecyclePlayer = LifecyclePlayer(lifecycleManager)
    }

    @Test
    fun `receive latest lifecycle state right after dispatch start`() {
        val listener = mockk<LifecycleStateListener>()
        lifecycleManager.addStateListener(listener)
        lifecyclePlayer.startActivity(mockk())

        verify { listener wasNot Called }

        val timeInMillis = 12345L
        lifecycleManager.dispatchStart(timeInMillis = timeInMillis)

        verify(exactly = 1) {
            listener.onState(LifecycleState.FOREGROUND, timeInMillis)
        }
    }

    @Test
    fun `should receive foreground state right after dispatch start after activity transition`() {
        val listener = mockk<LifecycleStateListener>()
        lifecycleManager.addStateListener(listener)

        lifecyclePlayer.startActivity(mockk())
        lifecyclePlayer.startActivity(mockk())

        verify { listener wasNot Called }

        val timeInMillis = 12345L
        lifecycleManager.dispatchStart(timeInMillis = timeInMillis)

        verify(exactly = 1) {
            listener.onState(LifecycleState.FOREGROUND, timeInMillis)
        }
    }

    @Test
    fun `should receive background state right after dispatch start after activity hided`() {
        val listener = mockk<LifecycleStateListener>()
        lifecycleManager.addStateListener(listener)

        lifecyclePlayer.startActivity(mockk())

        verify { listener wasNot Called }

        val timeInMillis = 12345L
        lifecycleManager.dispatchStart(timeInMillis = timeInMillis)

        verify(exactly = 1) {
            listener.onState(LifecycleState.FOREGROUND, timeInMillis)
        }
    }

    @Test
    fun `should receive on state once even dispatch start multiple times`() {
        val listener = mockk<LifecycleStateListener>()
        lifecycleManager.addStateListener(listener)

        lifecyclePlayer.startActivity(mockk())

        verify { listener wasNot Called }

        val timeInMillis = 12345L
        lifecycleManager.dispatchStart(timeInMillis = timeInMillis)
        lifecycleManager.dispatchStart()
        lifecycleManager.dispatchStart()
        lifecycleManager.dispatchStart()

        verify(exactly = 1) {
            listener.onState(LifecycleState.FOREGROUND, timeInMillis)
        }
    }

    @Test
    fun `receive sequential lifecycle state`() {
        val listener = mockk<LifecycleStateListener>()
        lifecycleManager.dispatchStart()
        lifecycleManager.addStateListener(listener)

        lifecyclePlayer.startActivity(mockk())
        lifecyclePlayer.finishActivity()

        verifySequence {
            listener.onState(LifecycleState.FOREGROUND, any())
            listener.onState(LifecycleState.BACKGROUND, any())
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
        lifecyclePlayer.startActivity(firstActivity)

        assertThat(lifecycleManager.currentActivity, `is`(firstActivity))

        val secondActivity = mockk<Activity>()
        lifecyclePlayer.startActivity(secondActivity)

        assertThat(lifecycleManager.currentActivity, `is`(secondActivity))
    }


    class LifecyclePlayer(private val lifecycleManager: LifecycleManager) {
        private val activityStack: Stack<Activity> = Stack()

        fun startActivity(activity: Activity, savedInstanceState: Bundle? = null) {
            val peek = if (activityStack.isNotEmpty()) activityStack.peek() else null
            if (peek != null) {
                lifecycleManager.onActivityPaused(peek)
            }

            activityStack.add(activity)
            lifecycleManager.onActivityCreated(activity, savedInstanceState)
            displayActivity(activity)

            if (peek != null) {
                lifecycleManager.onActivityStopped(peek)
            }
        }

        private fun displayActivity(activity: Activity) {
            lifecycleManager.onActivityStarted(activity)
            lifecycleManager.onActivityResumed(activity)
        }

        fun finishActivity() {
            val current = activityStack.pop()
            lifecycleManager.onActivityPaused(current)

            val prev  = if (activityStack.isNotEmpty()) activityStack.peek() else null
            if (prev != null) {
                displayActivity(prev)
            }

            lifecycleManager.onActivityStopped(current)
            lifecycleManager.onActivityDestroyed(current)
        }
    }
}
