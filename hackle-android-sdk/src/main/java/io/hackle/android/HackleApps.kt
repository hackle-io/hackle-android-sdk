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
import io.hackle.android.internal.model.Sdk
import io.hackle.android.internal.monitoring.metric.MonitoringMetricRegistry
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
import io.hackle.sdk.core.internal.log.metrics.MetricLoggerFactory
import io.hackle.sdk.core.internal.metrics.Metrics
import io.hackle.sdk.core.internal.scheduler.Schedulers
import okhttp3.OkHttpClient
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

internal object HackleApps {

    private val log = Logger<HackleApps>()

    private const val PREFERENCES_NAME = "io.hackle.android"

    fun create(context: Context, sdkKey: String, config: HackleConfig): HackleApp {

        val sdk = Sdk.of(sdkKey, config)
        loggerConfiguration(config)

        val sharedPreferences = context.getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE)
        val keyValueRepository = AndroidKeyValueRepository(sharedPreferences)

        val httpClient = createHttpClient(context, sdk)

        val workspaceCache = WorkspaceCache()

        val httpWorkspaceFetcher = HttpWorkspaceFetcher(
            baseSdkUri = config.sdkUri,
            httpClient = httpClient
        )

        val workspaceCacheHandler = WorkspaceCacheHandler(
            executor = Executors.newSingleThreadExecutor(),
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

        metricConfiguration(config, lifecycleCallbacks, eventExecutor, httpExecutor, httpClient)

        val client = HackleCore.client(
            workspaceFetcher = cachedWorkspaceFetcher,
            eventProcessor = eventProcessor
        )

        val device = Device.create(context, keyValueRepository)
        val userResolver = HackleUserResolver(device)
        val listeners = listOf(sessionManager, eventProcessor)
        return HackleApp(
            client = client,
            workspaceCacheHandler = workspaceCacheHandler,
            userResolver = userResolver,
            device = device,
            sessionManager = sessionManager,
            listeners = listeners
        )
    }

    private fun loggerConfiguration(config: HackleConfig) {
        Logger.add(AndroidLogger.Factory.logLevel(config.logLevel))
        Logger.add(MetricLoggerFactory(Metrics.globalRegistry))
    }

    private fun metricConfiguration(
        config: HackleConfig,
        callbacks: HackleActivityLifecycleCallbacks,
        eventExecutor: Executor,
        httpExecutor: Executor,
        httpClient: OkHttpClient,
    ) {
        val monitoringMetricRegistry = MonitoringMetricRegistry(
            monitoringBaseUrl = config.monitoringUri,
            eventExecutor = eventExecutor,
            httpExecutor = httpExecutor,
            httpClient = httpClient
        )

        callbacks.addListener(monitoringMetricRegistry)
        Metrics.addRegistry(monitoringMetricRegistry)
    }

    private fun createHttpClient(context: Context, sdk: Sdk): OkHttpClient {

        val builder = OkHttpClient.Builder()
            .connectTimeout(1, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .addInterceptor(SdkHeaderInterceptor(sdk.key, sdk.name, sdk.version))

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
