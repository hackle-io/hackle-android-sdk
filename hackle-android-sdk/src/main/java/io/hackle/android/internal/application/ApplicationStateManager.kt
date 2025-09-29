package io.hackle.android.internal.application

import io.hackle.android.internal.core.listener.ApplicationListenerRegistry
import io.hackle.sdk.core.internal.log.Logger
import io.hackle.sdk.core.internal.time.Clock
import java.util.concurrent.Executor

internal class ApplicationStateManager(
    private val clock: Clock,
) : ApplicationListenerRegistry<ApplicationStateListener>(), ApplicationLifecycleListener {

    private var executor: Executor? = null
    private var applicationInstallDeterminer: ApplicationInstallDeterminer? = null

    fun setExecutor(executor: Executor) {
        this.executor = executor
    }

    fun setApplicationInstallDeterminer(applicationInstallDeterminer: ApplicationInstallDeterminer) {
        this.applicationInstallDeterminer = applicationInstallDeterminer
    }

    override fun onApplicationForeground(timestamp: Long, isFromBackground: Boolean) {
        execute {
            listeners.forEach { listener ->
                listener.onForeground(timestamp, isFromBackground)
            }
        }
    }

    override fun onApplicationBackground(timestamp: Long) {
        execute {
            listeners.forEach { listener ->
                listener.onBackground(timestamp)
            }
        }
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
        private val log = Logger<ApplicationStateManager>()

        private var INSTANCE: ApplicationStateManager? = null

        val instance: ApplicationStateManager
            get() {
                return INSTANCE ?: synchronized(this) {
                    INSTANCE ?: ApplicationStateManager(Clock.SYSTEM).also { INSTANCE = it }
                }
            }
    }
}
