package io.hackle.android

import android.content.Context
import android.os.Build
import androidx.lifecycle.ProcessLifecycleOwner
import io.hackle.android.internal.database.DatabaseHelper
import io.hackle.android.internal.database.EventRepository
import io.hackle.android.internal.event.DefaultEventProcessor
import io.hackle.android.internal.event.EventDispatcher
import io.hackle.android.internal.event.ExposureEventDeduplicationDeterminer
import io.hackle.android.internal.http.SdkHeaderInterceptor
import io.hackle.android.internal.http.Tls
import io.hackle.android.internal.lifecycle.AppStateChangeObserver
import io.hackle.android.internal.log.AndroidLogger
import io.hackle.android.internal.model.Device
import io.hackle.android.internal.user.HackleUserResolver
import io.hackle.android.internal.utils.runOnMainThread
import io.hackle.android.internal.workspace.CachedWorkspaceFetcher
import io.hackle.android.internal.workspace.HttpWorkspaceFetcher
import io.hackle.android.internal.workspace.WorkspaceCache
import io.hackle.android.internal.workspace.WorkspaceCacheHandler
import io.hackle.sdk.core.HackleCore
import io.hackle.sdk.core.client
import io.hackle.sdk.core.internal.log.Logger
import io.hackle.sdk.core.internal.scheduler.Schedulers
import okhttp3.OkHttpClient
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

internal object HackleApps {

    private val log = Logger<HackleApps>()

    const val PREFERENCES_NAME = "io.hackle.android"

    fun create(context: Context, sdkKey: String, config: HackleConfig): HackleApp {

        Logger.factory = AndroidLogger.Factory

        val httpClient = createHttpClient(context, sdkKey)

        val workspaceCache = WorkspaceCache()

        val httpWorkspaceFetcher = HttpWorkspaceFetcher(
            baseSdkUri = config.sdkUri,
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

        val databaseHelper = DatabaseHelper(context)
        val eventRepository = EventRepository(databaseHelper)
        val eventExecutor = Executors.newSingleThreadExecutor()

        val eventDispatcher = EventDispatcher(
            baseEventUri = config.eventUri,
            eventRepository = eventRepository,
            eventExecutor = eventExecutor,
            dispatchExecutor = Executors.newFixedThreadPool(4),
            httpClient = httpClient
        )

        val defaultEventProcessor = DefaultEventProcessor(
            eventStorageMaxSize = 2000,
            eventRepository = eventRepository,
            eventExecutor = eventExecutor,
            flushScheduler = Schedulers.executor(Executors.newSingleThreadScheduledExecutor()),
            flushIntervalMillis = 30 * 1000,
            eventDispatcher = eventDispatcher,
            eventDispatchThreshold = 20,
            eventDispatchMaxSize = 100,
            deduplicationDeterminer = ExposureEventDeduplicationDeterminer(config.exposureEventDedupIntervalMillis)
        )

        val appStateChangeObserver = AppStateChangeObserver()
        runOnMainThread {
            ProcessLifecycleOwner.get().lifecycle.addObserver(appStateChangeObserver)
        }

        appStateChangeObserver
            .addListener(defaultEventProcessor)

        val client = HackleCore.client(
            workspaceFetcher = cachedWorkspaceFetcher,
            eventProcessor = defaultEventProcessor.apply { start() }
        )

        val device = Device.create(context)
        val userResolver = HackleUserResolver(device)
        return HackleApp(client, workspaceCacheHandler, userResolver, device)
    }

    private fun createHttpClient(context: Context, sdkKey: String): OkHttpClient {

        val builder = OkHttpClient.Builder()
            .connectTimeout(1, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .addInterceptor(SdkHeaderInterceptor(sdkKey, "android-sdk", BuildConfig.VERSION_NAME))

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {

            try {
                Tls.update(context)
                builder.sslSocketFactory(Tls.tls12SocketFactory(), Tls.defaultTrustManager())
            } catch (e: Exception) {
                log.error { "TLS is not available: $e" }
            }
        }

        return builder.build()
    }
}
