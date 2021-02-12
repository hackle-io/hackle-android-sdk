package io.hackle.android

import android.content.Context
import android.content.Context.MODE_PRIVATE
import androidx.lifecycle.ProcessLifecycleOwner
import io.hackle.android.internal.event.DefaultEventProcessor
import io.hackle.android.internal.event.EventDispatcher
import io.hackle.android.internal.http.SdkHeaderInterceptor
import io.hackle.android.internal.lifecycle.AppStateChangeObserver
import io.hackle.android.internal.log.AndroidLogger
import io.hackle.android.internal.workspace.CachedWorkspaceFetcher
import io.hackle.android.internal.workspace.HttpWorkspaceFetcher
import io.hackle.android.internal.workspace.WorkspaceCache
import io.hackle.android.internal.workspace.WorkspaceCacheHandler
import io.hackle.sdk.core.HackleCore
import io.hackle.sdk.core.client
import io.hackle.sdk.core.internal.log.Logger
import io.hackle.sdk.core.internal.scheduler.Schedulers
import io.hackle.sdk.core.internal.utils.minutes
import io.hackle.sdk.core.internal.utils.seconds
import okhttp3.OkHttpClient
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Executors

internal object HackleApps {

    private const val PREFERENCES_NAME = "io.hackle.android"

    fun create(context: Context, sdkKey: String): HackleApp {

        Logger.factory = AndroidLogger.Factory

        val httpClient = OkHttpClient.Builder()
            .connectTimeout(1.seconds)
            .readTimeout(5.seconds)
            .writeTimeout(5.seconds)
            .addInterceptor(SdkHeaderInterceptor(sdkKey, "android-sdk", BuildConfig.VERSION_NAME))
            .build()

        val workspaceCache = WorkspaceCache()

        val httpWorkspaceFetcher = HttpWorkspaceFetcher(
            baseSdkUri = "https://sdk.hackle.io",
            httpClient = httpClient
        )

        val workspaceCacheHandler = WorkspaceCacheHandler(
            executor = Executors.newCachedThreadPool(),
            workspaceCache = workspaceCache,
            httpWorkspaceFetcher = httpWorkspaceFetcher
        )

        val cachedWorkspaceFetcher = CachedWorkspaceFetcher(
            workspaceCache = workspaceCache
        )

        val eventDispatcher = EventDispatcher(
            baseEventUri = "https://event.hackle.io",
            executor = Executors.newCachedThreadPool(),
            httpClient = httpClient
        )

        val defaultEventProcessor = DefaultEventProcessor(
            queue = ArrayBlockingQueue(100),
            flushScheduler = Schedulers.executor(Executors.newSingleThreadScheduledExecutor()),
            flushInterval = 1.minutes,
            eventDispatcher = eventDispatcher,
            maxEventDispatchSize = 20
        )

        val appStateChangeObserver = AppStateChangeObserver().also {
            ProcessLifecycleOwner.get().lifecycle.addObserver(it)
        }

        appStateChangeObserver
            .addListener(defaultEventProcessor)

        val client = HackleCore.client(
            workspaceFetcher = cachedWorkspaceFetcher,
            eventProcessor = defaultEventProcessor.apply { start() }
        )

        val sharedPreferences = context.getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE)

        return HackleApp(client, workspaceCacheHandler, sharedPreferences)
    }
}