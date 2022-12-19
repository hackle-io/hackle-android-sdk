package io.hackle.android

import android.app.Application
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.os.Build
import io.hackle.android.internal.database.AndroidKeyValueRepository
import io.hackle.android.internal.database.DatabaseHelper
import io.hackle.android.internal.database.EventRepository
import io.hackle.android.internal.event.DefaultEventProcessor
import io.hackle.android.internal.event.EventDispatcher
import io.hackle.android.internal.event.ExposureEventDeduplicationDeterminer
import io.hackle.android.internal.http.SdkHeaderInterceptor
import io.hackle.android.internal.http.Tls
import io.hackle.android.internal.lifecycle.HackleActivityLifecycleCallbacks
import io.hackle.android.internal.log.AndroidLogger
import io.hackle.android.internal.model.Device
import io.hackle.android.internal.session.SessionManager
import io.hackle.android.internal.task.TaskExecutors
import io.hackle.android.internal.user.HackleUserResolver
import io.hackle.android.internal.user.UserManager
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

    private const val PREFERENCES_NAME = "io.hackle.android"

    fun create(context: Context, sdkKey: String, config: HackleConfig): HackleApp {

        Logger.factory = AndroidLogger.Factory.also {
            it.logLevel = config.logLevel
        }

        val sharedPreferences = context.getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE)
        val keyValueRepository = AndroidKeyValueRepository(sharedPreferences)

        val httpClient = createHttpClient(context, sdkKey)

        val workspaceCache = WorkspaceCache()

        val httpWorkspaceFetcher = HttpWorkspaceFetcher(
            baseSdkUri = config.sdkUri,
            httpClient = httpClient
        )

        val workspaceCacheHandler = WorkspaceCacheHandler(
            workspaceCache = workspaceCache,
            httpWorkspaceFetcher = httpWorkspaceFetcher
        )

        val cachedWorkspaceFetcher = CachedWorkspaceFetcher(
            workspaceCache = workspaceCache
        )

        val databaseHelper = DatabaseHelper[context, sdkKey]
        val eventRepository = EventRepository(databaseHelper)
        val eventExecutor = TaskExecutors.handler("io.hackle.EventExecutor")
        val httpExecutor = TaskExecutors.handler("io.hackle.HttpExecutor")

        val eventDispatcher = EventDispatcher(
            baseEventUri = config.eventUri,
            eventExecutor = eventExecutor,
            eventRepository = eventRepository,
            httpExecutor = httpExecutor,
            httpClient = httpClient
        )

        val userManager = UserManager()
        val sessionManager = SessionManager(
            sessionTimeoutMillis = config.sessionTimeoutMillis.toLong(),
            keyValueRepository = keyValueRepository,
            eventExecutor = eventExecutor
        )
        val dedupDeterminer = ExposureEventDeduplicationDeterminer(
            exposureEventDedupIntervalMillis = config.exposureEventDedupIntervalMillis
        )

        val eventProcessor = DefaultEventProcessor(
            deduplicationDeterminer = dedupDeterminer,
            eventExecutor = eventExecutor,
            eventRepository = eventRepository,
            eventRepositoryMaxSize = HackleConfig.DEFAULT_EVENT_REPOSITORY_MAX_SIZE,
            eventFlushScheduler = Schedulers.executor(Executors.newSingleThreadScheduledExecutor()),
            eventFlushIntervalMillis = config.eventFlushIntervalMillis.toLong(),
            eventFlushThreshold = config.eventFlushThreshold,
            eventFlushMaxBatchSize = config.eventFlushThreshold * 2 + 1,
            eventDispatcher = eventDispatcher,
            userManager = userManager,
            sessionManager = sessionManager
        )


        val lifecycleCallbacks = HackleActivityLifecycleCallbacks().apply {
            addListener(sessionManager)
            addListener(eventProcessor)
        }
        (context as? Application)?.registerActivityLifecycleCallbacks(lifecycleCallbacks)

        userManager.addListener(sessionManager)

        val client = HackleCore.client(
            workspaceFetcher = cachedWorkspaceFetcher,
            eventProcessor = eventProcessor
        )

        val device = Device.create(context, keyValueRepository)
        val userResolver = HackleUserResolver(device)
        val listeners = listOf(sessionManager, eventProcessor, workspaceCacheHandler)
        return HackleApp(
            client = client,
            userResolver = userResolver,
            device = device,
            eventExecutor = eventExecutor,
            sessionManager = sessionManager,
            listeners = listeners
        )
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
