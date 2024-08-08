package io.hackle.android

import android.content.Context
import android.os.Build
import io.hackle.android.internal.core.Ordered
import io.hackle.android.internal.database.DatabaseHelper
import io.hackle.android.internal.database.repository.AndroidKeyValueRepository
import io.hackle.android.internal.database.repository.EventRepository
import io.hackle.android.internal.database.repository.NotificationHistoryRepositoryImpl
import io.hackle.android.internal.engagement.EngagementEventTracker
import io.hackle.android.internal.engagement.EngagementManager
import io.hackle.android.internal.event.DefaultEventProcessor
import io.hackle.android.internal.event.EventDispatcher
import io.hackle.android.internal.event.UserEventPublisher
import io.hackle.android.internal.event.dedup.DedupUserEventFilter
import io.hackle.android.internal.event.dedup.DelegatingUserEventDedupDeterminer
import io.hackle.android.internal.event.dedup.ExposureEventDedupDeterminer
import io.hackle.android.internal.event.dedup.RemoteConfigEventDedupDeterminer
import io.hackle.android.internal.http.SdkHeaderInterceptor
import io.hackle.android.internal.http.Tls
import io.hackle.android.internal.inappmessage.storage.AndroidInAppMessageHiddenStorage
import io.hackle.android.internal.inappmessage.storage.InAppMessageImpressionStorage
import io.hackle.android.internal.inappmessage.trigger.*
import io.hackle.android.internal.lifecycle.AppStateManager
import io.hackle.android.internal.lifecycle.LifecycleManager
import io.hackle.android.internal.log.AndroidLogger
import io.hackle.android.internal.mode.webview.WebViewWrapperUserEventFilter
import io.hackle.android.internal.model.Device
import io.hackle.android.internal.model.Sdk
import io.hackle.android.internal.monitoring.metric.MonitoringMetricRegistry
import io.hackle.android.internal.notification.NotificationManager
import io.hackle.android.internal.push.PushEventTracker
import io.hackle.android.internal.push.token.PushTokenFetchers
import io.hackle.android.internal.push.token.PushTokenManager
import io.hackle.android.internal.screen.ScreenEventTracker
import io.hackle.android.internal.screen.ScreenManager
import io.hackle.android.internal.session.SessionEventTracker
import io.hackle.android.internal.session.SessionManager
import io.hackle.android.internal.storage.DefaultFileStorage
import io.hackle.android.internal.sync.CompositeSynchronizer
import io.hackle.android.internal.sync.PollingSynchronizer
import io.hackle.android.internal.task.TaskExecutors
import io.hackle.android.internal.user.UserCohortFetcher
import io.hackle.android.internal.user.UserManager
import io.hackle.android.internal.utils.concurrent.ThrottleLimiter
import io.hackle.android.internal.utils.concurrent.Throttler
import io.hackle.android.internal.workspace.HttpWorkspaceFetcher
import io.hackle.android.internal.workspace.WorkspaceManager
import io.hackle.android.internal.workspace.repository.DefaultWorkspaceConfigRepository
import io.hackle.android.ui.core.GlideImageLoader
import io.hackle.android.ui.explorer.HackleUserExplorer
import io.hackle.android.ui.explorer.base.HackleUserExplorerService
import io.hackle.android.ui.explorer.storage.HackleUserManualOverrideStorage.Companion.create
import io.hackle.android.ui.inappmessage.InAppMessageControllerFactory
import io.hackle.android.ui.inappmessage.InAppMessageUi
import io.hackle.android.ui.inappmessage.event.*
import io.hackle.android.ui.inappmessage.layout.view.InAppMessageViewFactory
import io.hackle.android.ui.notification.NotificationHandler
import io.hackle.sdk.core.HackleCore
import io.hackle.sdk.core.evaluation.EvaluationContext
import io.hackle.sdk.core.evaluation.get
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
        val keyValueRepositoryBySdkKey = AndroidKeyValueRepository.create(context, "${PREFERENCES_NAME}_$sdkKey")
        val device = Device.create(context, globalKeyValueRepository)

        val httpClient = createHttpClient(context, sdk)

        // Lifecycle, AppState

        val lifecycleManager = LifecycleManager.instance
        val appStateManager = AppStateManager.instance

        // Synchronizer

        val compositeSynchronizer = CompositeSynchronizer(
            executor = TaskExecutors.default()
        )
        val pollingSynchronizer = PollingSynchronizer(
            delegate = compositeSynchronizer,
            scheduler = Schedulers.executor("HacklePollingSynchronizer-"),
            intervalMillis = config.pollingIntervalMillis.toLong()
        )

        // WorkspaceManager (fetcher, synchronizer)

        val httpWorkspaceFetcher = HttpWorkspaceFetcher(
            sdk = sdk,
            sdkUri = config.sdkUri,
            httpClient = httpClient
        )

        val fileStorage = DefaultFileStorage(
            context = context,
            sdkKey = sdkKey
        )
        val workspaceConfigRepository = DefaultWorkspaceConfigRepository(fileStorage)
        val workspaceManager = WorkspaceManager(
            httpWorkspaceFetcher = httpWorkspaceFetcher,
            repository = workspaceConfigRepository
        )
        compositeSynchronizer.add(workspaceManager)

        // UserManager

        val cohortFetcher = UserCohortFetcher(config.sdkUri, httpClient)
        val userManager = UserManager(
            device = device,
            repository = keyValueRepositoryBySdkKey,
            cohortFetcher = cohortFetcher
        )
        compositeSynchronizer.add(userManager)

        // SessionManager

        val sessionManager = SessionManager(
            userManager = userManager,
            sessionTimeoutMillis = config.sessionTimeoutMillis.toLong(),
            keyValueRepository = globalKeyValueRepository,
        )
        userManager.addListener(sessionManager)

        // ScreenManager

        val screenManager = ScreenManager(
            userManager = userManager,
            activityProvider = lifecycleManager
        )

        // EngagementManager

        val engagementManager = EngagementManager(
            userManager = userManager,
            screenManager = screenManager,
            minimumEngagementDurationMillis = 1000L
        )
        screenManager.addListener(engagementManager)

        // EventProcessor

        val workspaceDatabase = DatabaseHelper.getWorkspaceDatabase(context, sdkKey)
        val eventRepository = EventRepository(workspaceDatabase)
        val eventExecutor = TaskExecutors.handler("io.hackle.EventExecutor")
        val httpExecutor = TaskExecutors.handler("io.hackle.HttpExecutor")

        val eventDispatcher = EventDispatcher(
            baseEventUri = config.eventUri,
            eventExecutor = eventExecutor,
            eventRepository = eventRepository,
            httpExecutor = httpExecutor,
            httpClient = httpClient
        )

        val eventPublisher = UserEventPublisher()

        val eventProcessor = DefaultEventProcessor(
            eventPublisher = eventPublisher,
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
            appStateManager = appStateManager,
            screenManager = screenManager
        )

        val eventDedupDeterminer = DelegatingUserEventDedupDeterminer(
            listOf(
                RemoteConfigEventDedupDeterminer(config.exposureEventDedupIntervalMillis.toLong(), Clock.SYSTEM),
                ExposureEventDedupDeterminer(config.exposureEventDedupIntervalMillis.toLong(), Clock.SYSTEM)
            )
        )
        val dedupUserEventFilter = DedupUserEventFilter(eventDedupDeterminer)
        eventProcessor.addFilter(dedupUserEventFilter)

        if (config.mode == HackleAppMode.WEB_VIEW_WRAPPER) {
            eventProcessor.addFilter(WebViewWrapperUserEventFilter())
        }

        // Core

        val abOverrideStorage = create(context, "${PREFERENCES_NAME}_ab_override_$sdkKey")
        val ffOverrideStorage = create(context, "${PREFERENCES_NAME}_ff_override_$sdkKey")
        val inAppMessageHiddenStorage = AndroidInAppMessageHiddenStorage.create(
            context, "${PREFERENCES_NAME}_in_app_message_$sdkKey"
        )
        val inAppMessageImpressionStorage =
            InAppMessageImpressionStorage.create(
                context,
                "${PREFERENCES_NAME}_iam_impression_$sdkKey"
            )

        EvaluationContext.GLOBAL.register(inAppMessageHiddenStorage)

        val core = HackleCore.create(
            context = EvaluationContext.GLOBAL,
            workspaceFetcher = workspaceManager,
            eventProcessor = eventProcessor,
            manualOverrideStorages = arrayOf(abOverrideStorage, ffOverrideStorage)
        )

        // AppStateListener

        appStateManager.setExecutor(eventExecutor)
        appStateManager.addListener(pollingSynchronizer)
        appStateManager.addListener(sessionManager)
        appStateManager.addListener(userManager)
        appStateManager.addListener(eventProcessor, order = Ordered.LOWEST - 1)

        // SessionEventTracker

        val sessionEventTracker = SessionEventTracker(
            userManager = userManager,
            core = core
        )
        if (config.sessionTracking) {
            sessionManager.addListener(sessionEventTracker)
        }

        // ScreenEventTracker

        val screenEventTracker = ScreenEventTracker(
            userManager = userManager,
            core = core
        )
        screenManager.addListener(screenEventTracker)

        // EngagementEventTracker

        val engagementEventTracker = EngagementEventTracker(
            userManager = userManager,
            core = core
        )
        engagementManager.addListener(engagementEventTracker)

        // InAppMessage

        val inAppMessageEventTracker = InAppMessageEventTracker(
            core = core
        )
        val uriHandler = UriHandler()
        val inAppMessageActionHandlerFactory = InAppMessageActionHandlerFactory(
            handlers = listOf(
                InAppMessageCloseActionHandler(),
                InAppMessageLinkActionHandler(uriHandler),
                InAppMessageLinkAndCloseActionHandler(uriHandler),
                InAppMessageHideActionHandler(inAppMessageHiddenStorage, Clock.SYSTEM)
            )
        )
        val inAppMessageEventProcessorFactory = InAppMessageEventProcessorFactory(
            processors = listOf(
                InAppMessageImpressionEventProcessor(inAppMessageImpressionStorage),
                InAppMessageActionEventProcessor(inAppMessageActionHandlerFactory),
                InAppMessageCloseEventProcessor()
            )
        )
        val inAppMessageEventHandler = InAppMessageEventHandler(
            clock = Clock.SYSTEM,
            eventTracker = inAppMessageEventTracker,
            processorFactory = inAppMessageEventProcessorFactory
        )
        val imageLoader = GlideImageLoader()
        val inAppMessageUi = InAppMessageUi.create(
            activityProvider = lifecycleManager,
            messageControllerFactory = InAppMessageControllerFactory(InAppMessageViewFactory()),
            eventHandler = inAppMessageEventHandler,
            imageLoader = imageLoader
        )
        val inAppMessageEventMatcher = InAppMessageEventMatcher(
            ruleDeterminer = InAppMessageEventTriggerRuleDeterminer(EvaluationContext.GLOBAL.get()),
            frequencyCapDeterminer = InAppMessageEventTriggerFrequencyCapDeterminer(
                inAppMessageImpressionStorage
            )
        )
        val inAppMessageDeterminer = InAppMessageDeterminer(
            workspaceFetcher = workspaceManager,
            eventMatcher = inAppMessageEventMatcher,
            core = core,
        )
        val inAppMessageManager = InAppMessageManager(
            determiner = inAppMessageDeterminer,
            presenter = inAppMessageUi,
            activityProvider = lifecycleManager
        )
        eventPublisher.add(inAppMessageManager)

        // PushToken
        val pushTokenFetcher = PushTokenFetchers.create(context)
        val pushEventTracker = PushEventTracker(
            userManager = userManager,
            core = core
        )
        val pushTokenManager = PushTokenManager(
            repository = keyValueRepositoryBySdkKey,
            pushTokenFetcher = pushTokenFetcher,
            pushEventTracker = pushEventTracker
        )
        sessionManager.addListener(pushTokenManager)

        // Notification

        val notificationManager = NotificationManager(
            core = core,
            executor = eventExecutor,
            workspaceFetcher = workspaceManager,
            userManager = userManager,
            repository = NotificationHistoryRepositoryImpl(
                DatabaseHelper.getSharedDatabase(context)
            )
        )
        NotificationHandler.getInstance(context)
            .setNotificationDataReceiver(notificationManager)

        // UserExplorer

        val userExplorer = HackleUserExplorer(
            explorerService = HackleUserExplorerService(
                core = core,
                userManager = userManager,
                abTestOverrideStorage = abOverrideStorage,
                featureFlagOverrideStorage = ffOverrideStorage,
                pushTokenManager = pushTokenManager
            ),
            activityProvider = lifecycleManager
        )

        // Metrics

        metricConfiguration(config, appStateManager, eventExecutor, httpExecutor, httpClient)

        // LifecycleListener

        if (config.automaticScreenTracking) {
            lifecycleManager.addListener(screenManager, order = Ordered.HIGHEST)
        }
        lifecycleManager.addListener(engagementManager, order = Ordered.HIGHEST + 1)
        lifecycleManager.addListener(userExplorer, order = Ordered.LOWEST - 1)
        lifecycleManager.registerTo(context)

        val throttleLimiter = ThrottleLimiter(
            intervalMillis = 60 * 1000,
            limit = 1,
            clock = Clock.SYSTEM
        )
        val fetchThrottler = Throttler(throttleLimiter)

        // Instantiate

        return HackleApp(
            clock = Clock.SYSTEM,
            core = core,
            eventExecutor = eventExecutor,
            backgroundExecutor = TaskExecutors.default(),
            synchronizer = pollingSynchronizer,
            userManager = userManager,
            workspaceManager = workspaceManager,
            sessionManager = sessionManager,
            eventProcessor = eventProcessor,
            pushTokenManager = pushTokenManager,
            notificationManager = notificationManager,
            fetchThrottler = fetchThrottler,
            device = device,
            userExplorer = userExplorer,
            sdk = sdk,
            mode = config.mode
        )
    }

    private fun loggerConfiguration(config: HackleConfig) {
        Logger.add(AndroidLogger.Factory.logLevel(config.logLevel))
        Logger.add(MetricLoggerFactory(Metrics.globalRegistry))
    }

    private fun metricConfiguration(
        config: HackleConfig,
        appStateManager: AppStateManager,
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

        appStateManager.addListener(monitoringMetricRegistry, order = Ordered.LOWEST)
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
