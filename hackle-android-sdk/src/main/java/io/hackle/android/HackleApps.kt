package io.hackle.android

import android.app.Application
import android.content.Context
import android.os.Build
import io.hackle.android.explorer.HackleUserExplorer
import io.hackle.android.explorer.base.HackleUserExplorerService
import io.hackle.android.explorer.storage.HackleUserManualOverrideStorage.Companion.create
import io.hackle.android.inappmessage.InAppMessageRenderer
import io.hackle.android.inappmessage.storage.HackleInAppMessageStorage
import io.hackle.android.internal.database.AndroidKeyValueRepository
import io.hackle.android.internal.database.DatabaseHelper
import io.hackle.android.internal.database.EventRepository
import io.hackle.android.internal.event.DefaultEventProcessor
import io.hackle.android.internal.event.EventDispatcher
import io.hackle.android.internal.event.ExposureEventDeduplicationDeterminer
import io.hackle.android.internal.http.SdkHeaderInterceptor
import io.hackle.android.internal.http.Tls
import io.hackle.android.internal.lifecycle.AppStateManager
import io.hackle.android.internal.inappmessage.InAppMessageManager
import io.hackle.android.internal.inappmessage.InAppMessageTriggerDeterminer
import io.hackle.android.internal.lifecycle.HackleActivityLifecycleCallbacks
import io.hackle.android.internal.log.AndroidLogger
import io.hackle.android.internal.model.Device
import io.hackle.android.internal.model.Sdk
import io.hackle.android.internal.monitoring.metric.MonitoringMetricRegistry
import io.hackle.android.internal.session.SessionEventTracker
import io.hackle.android.internal.session.SessionManager
import io.hackle.android.internal.task.TaskExecutors
import io.hackle.android.internal.user.HackleUserResolver
import io.hackle.android.internal.user.UserManager
import io.hackle.android.internal.workspace.CachedWorkspaceFetcher
import io.hackle.android.internal.workspace.HttpWorkspaceFetcher
import io.hackle.android.internal.workspace.PollingWorkspaceHandler
import io.hackle.android.internal.workspace.WorkspaceCache
import io.hackle.sdk.core.HackleCore
import io.hackle.sdk.core.HackleCoreContext
import io.hackle.sdk.core.evaluation.match.TargetMatcher
import io.hackle.sdk.core.internal.log.Logger
import io.hackle.sdk.core.internal.log.metrics.MetricLoggerFactory
import io.hackle.sdk.core.internal.metrics.Metrics
import io.hackle.sdk.core.internal.scheduler.Schedulers
import io.hackle.sdk.core.internal.time.Clock
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

        val globalKeyValueRepository = AndroidKeyValueRepository.create(context, PREFERENCES_NAME)
        val device = Device.create(context, globalKeyValueRepository)

        val httpClient = createHttpClient(context, sdk)

        val workspaceCache = WorkspaceCache()

        val httpWorkspaceFetcher = HttpWorkspaceFetcher(
            baseSdkUri = config.sdkUri,
            httpClient = httpClient
        )

        val workspaceHandler = PollingWorkspaceHandler(
            workspaceCache = workspaceCache,
            httpWorkspaceFetcher = httpWorkspaceFetcher,
            pollingScheduler = Schedulers.executor(Executors.newSingleThreadScheduledExecutor()),
            pollingIntervalMillis = config.pollingIntervalMillis.toLong()
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

        val userManager = UserManager(
            device = device,
            repository = AndroidKeyValueRepository.create(context, "${PREFERENCES_NAME}_$sdkKey")
        )
        val sessionManager = SessionManager(
            userManager = userManager,
            sessionTimeoutMillis = config.sessionTimeoutMillis.toLong(),
            keyValueRepository = globalKeyValueRepository,
        )
        val dedupDeterminer = ExposureEventDeduplicationDeterminer(
            exposureEventDedupIntervalMillis = config.exposureEventDedupIntervalMillis
        )

        val appStateManager = AppStateManager()

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
            sessionManager = sessionManager,
            userManager = userManager,
            appStateManager = appStateManager
        )


        val abOverrideStorage = create(context, "${PREFERENCES_NAME}_ab_override_$sdkKey")
        val ffOverrideStorage = create(context, "${PREFERENCES_NAME}_ff_override_$sdkKey")
        val inAppMessageStorage =
            HackleInAppMessageStorage.create(context, "${PREFERENCES_NAME}_in_app_message_$sdkKey")

        HackleCoreContext.registerInstance(inAppMessageStorage)

        val core = HackleCore.create(
            workspaceFetcher = cachedWorkspaceFetcher,
            eventProcessor = eventProcessor,
            manualOverrideStorages = arrayOf(
                abOverrideStorage,
                ffOverrideStorage
            )
        )


        val inAppMessageRenderer = InAppMessageRenderer()
        val targetMatcher = HackleCoreContext.get(TargetMatcher::class.java)
        val inAppMessageTriggerDeterminer = InAppMessageTriggerDeterminer(targetMatcher)
        val inAppMessageManager = InAppMessageManager(
            cachedWorkspaceFetcher,
            core,
            appStateManager,
            inAppMessageRenderer,
            inAppMessageTriggerDeterminer
        )
        eventProcessor.addListener(inAppMessageManager)


        val hackleUserResolver = HackleUserResolver(device)

        val lifecycleCallbacks = HackleActivityLifecycleCallbacks(
            eventExecutor = eventExecutor,
            appStateManager = appStateManager
        )
            .addListener(workspaceHandler)
            .addListener(sessionManager)
            .addListener(userManager)
            .addListener(eventProcessor)


        val userExplorer = HackleUserExplorer(
            HackleUserExplorerService(
                core = core,
                userManager = userManager,
                hackleUserResolver = hackleUserResolver,
                abTestOverrideStorage = abOverrideStorage,
                featureFlagOverrideStorage = ffOverrideStorage
            )
        )


        (context as? Application)?.let {
            it.registerActivityLifecycleCallbacks(lifecycleCallbacks)
            it.registerActivityLifecycleCallbacks(userExplorer)
            it.registerActivityLifecycleCallbacks(inAppMessageRenderer)
            it.registerActivityLifecycleCallbacks(eventProcessor)
        }

        userManager.addListener(sessionManager)

        val sessionEventTracker = SessionEventTracker(
            hackleUserResolver = hackleUserResolver,
            core = core
        )
        sessionManager.addListener(sessionEventTracker)

        metricConfiguration(config, lifecycleCallbacks, eventExecutor, httpExecutor, httpClient)

        return HackleApp(
            clock = Clock.SYSTEM,
            core = core,
            eventExecutor = eventExecutor,
            workspaceHandler = workspaceHandler,
            hackleUserResolver = hackleUserResolver,
            userManager = userManager,
            sessionManager = sessionManager,
            eventProcessor = eventProcessor,
            device = device,
            userExplorer = userExplorer
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
