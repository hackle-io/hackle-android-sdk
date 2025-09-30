package io.hackle.android.internal.application

import io.hackle.android.internal.core.listener.ApplicationListenerRegistry
import io.hackle.sdk.core.internal.log.Logger
import io.hackle.sdk.core.internal.time.Clock
import java.util.concurrent.Executor

internal class ApplicationInstallStateManager(
    private val clock: Clock,
) : ApplicationListenerRegistry<ApplicationInstallStateListener>() {

    private var executor: Executor? = null
    private var applicationInstallDeterminer: ApplicationInstallDeterminer? = null

    fun setExecutor(executor: Executor) {
        this.executor = executor
    }

    fun setApplicationInstallDeterminer(applicationInstallDeterminer: ApplicationInstallDeterminer) {
        this.applicationInstallDeterminer = applicationInstallDeterminer
    }

    internal fun checkApplicationInstall() {
        val state = applicationInstallDeterminer?.determine()

        execute {
            if (state != null && state != ApplicationInstallState.NONE) {
                log.debug { "application($state)" }
                when (state) {
                    ApplicationInstallState.INSTALL -> publishInstall(clock.currentMillis())
                    ApplicationInstallState.UPDATE -> publishUpdate(clock.currentMillis())
                    else -> Unit
                }
            }
        }
    }

    private fun publishInstall(timestamp: Long) {
        listeners.forEach { listener ->
            listener.onInstall(timestamp)
        }
    }

    private fun publishUpdate(timestamp: Long) {
        listeners.forEach { listener ->
            listener.onUpdate(timestamp)
        }
    }

    private fun execute(block: () -> Unit) {
        val executor = executor
        if (executor != null) {
            executor.execute(block)
        } else {
            block()
        }
    }

    companion object Companion {
        private val log = Logger<ApplicationInstallStateManager>()

        private var INSTANCE: ApplicationInstallStateManager? = null

        val instance: ApplicationInstallStateManager
            get() {
                return INSTANCE ?: synchronized(this) {
                    INSTANCE ?: ApplicationInstallStateManager(Clock.SYSTEM).also { INSTANCE = it }
                }
            }
    }
}
