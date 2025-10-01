package io.hackle.android.internal.application

import io.hackle.android.internal.core.listener.ApplicationListenerRegistry
import io.hackle.sdk.core.internal.log.Logger
import io.hackle.sdk.core.internal.time.Clock
import java.util.concurrent.Executor

internal class ApplicationInstallStateManager(
    private val executor: Executor,
    private val clock: Clock,
) : ApplicationListenerRegistry<ApplicationInstallStateListener>() {
    private var applicationInstallDeterminer: ApplicationInstallDeterminer? = null
    
    fun setApplicationInstallDeterminer(applicationInstallDeterminer: ApplicationInstallDeterminer) {
        this.applicationInstallDeterminer = applicationInstallDeterminer
    }

    internal fun checkApplicationInstall() {
        val state = applicationInstallDeterminer?.determine()
        execute {
            if (state != null && state != ApplicationInstallState.NONE) {
                log.debug { "application($state)" }
                val timestamp = clock.currentMillis()
                when (state) {
                    ApplicationInstallState.INSTALL -> publishInstall(timestamp)
                    ApplicationInstallState.UPDATE -> publishUpdate(timestamp)
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
        executor.execute(block)
    }

    companion object Companion {
        private val log = Logger<ApplicationInstallStateManager>()
    }
}
