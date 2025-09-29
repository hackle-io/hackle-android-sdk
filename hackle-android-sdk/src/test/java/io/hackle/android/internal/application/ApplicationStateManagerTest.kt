package io.hackle.android.internal.application

import android.app.Activity
import io.hackle.android.internal.lifecycle.AppState
import io.hackle.android.internal.lifecycle.Lifecycle
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isSameInstanceAs
import java.util.concurrent.Executor

class ApplicationStateManagerTest {

    private var manager: ApplicationStateManager = ApplicationStateManager.instance
    private val mockListener = mockk<ApplicationStateListener>(relaxed = true)
    private val mockExecutor = mockk<Executor>()
    private val mockInstallDeterminer = mockk<ApplicationInstallDeterminer>()
    private val mockActivity1 = TestActivity()
    private val mockActivity2 = TestActivity2()

    private class TestActivity : Activity()
    private class TestActivity2 : Activity()
    
    @Before
    fun setUp() {
        manager.addListener(mockListener)

        // Setup default mock behaviors
        every { mockInstallDeterminer.determine() } returns ApplicationInstallState.NONE
    }
}