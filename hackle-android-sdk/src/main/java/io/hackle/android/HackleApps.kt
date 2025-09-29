package io.hackle.android

import android.content.Context
import android.os.Build
import io.hackle.android.internal.HackleAppCore
import io.hackle.android.internal.core.Ordered
import io.hackle.android.internal.database.DatabaseHelper
import io.hackle.android.internal.database.repository.AndroidKeyValueRepository
import io.hackle.android.internal.database.repository.EventRepository
import io.hackle.android.internal.database.repository.NotificationHistoryRepositoryImpl
import io.hackle.android.internal.devtools.DevToolsApi
import io.hackle.android.internal.engagement.EngagementEventTracker
import io.hackle.android.internal.engagement.EngagementManager
import io.hackle.android.internal.event.DefaultEventProcessor
import io.hackle.android.internal.event.EventDispatcher
import io.hackle.android.internal.event.UserEventBackoffController
import io.hackle.android.internal.event.UserEventPublisher
import io.hackle.android.internal.event.dedup.DedupUserEventFilter
import io.hackle.android.internal.event.dedup.DelegatingUserEventDedupDeterminer
import io.hackle.android.internal.event.dedup.ExposureEventDedupDeterminer
import io.hackle.android.internal.event.dedup.RemoteConfigEventDedupDeterminer
import io.hackle.android.internal.http.SdkHeaderInterceptor
import io.hackle.android.internal.http.Tls
import io.hackle.android.internal.inappmessage.InAppMessageManager
import io.hackle.android.internal.inappmessage.delay.InAppMessageDelayManager
import io.hackle.android.internal.inappmessage.delay.InAppMessageDelayScheduler
import io.hackle.android.internal.inappmessage.deliver.InAppMessageDeliverProcessor
import io.hackle.android.internal.inappmessage.evaluation.InAppMessageEvaluateProcessor
import io.hackle.android.internal.inappmessage.evaluation.InAppMessageIdentifierChecker
import io.hackle.android.internal.inappmessage.evaluation.InAppMessageLayoutResolver
import io.hackle.android.internal.inappmessage.present.InAppMessagePresentProcessor
import io.hackle.android.internal.inappmessage.present.record.InAppMessageRecorder
import io.hackle.android.internal.inappmessage.reset.InAppMessageResetProcessor
import io.hackle.android.internal.inappmessage.schedule.InAppMessageScheduleProcessor
import io.hackle.android.internal.inappmessage.schedule.action.InAppMessageScheduleActionDeterminer
import io.hackle.android.internal.inappmessage.schedule.scheduler.DelayedInAppMessageScheduler
import io.hackle.android.internal.inappmessage.schedule.scheduler.InAppMessageSchedulerFactory
import io.hackle.android.internal.inappmessage.schedule.scheduler.TriggeredInAppMessageScheduler
import io.hackle.android.internal.inappmessage.storage.AndroidInAppMessageHiddenStorage
import io.hackle.android.internal.inappmessage.storage.AndroidInAppMessageImpressionStorage
import io.hackle.android.internal.inappmessage.trigger.*
import io.hackle.android.internal.invocator.HackleInvocatorImpl
import io.hackle.android.internal.lifecycle.ActivityStateManager
import io.hackle.android.internal.application.ApplicationEventTracker
import io.hackle.android.internal.application.ApplicationInstallDeterminer
import io.hackle.android.internal.application.ApplicationLifecycleManager
import io.hackle.android.internal.application.ApplicationStateManager
import io.hackle.android.internal.lifecycle.LifecycleManager
import io.hackle.android.internal.log.AndroidLogger
import io.hackle.android.internal.mode.webview.WebViewWrapperUserEventDecorator
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
import io.hackle.android.internal.screen.ScreenUserEventDecorator
import io.hackle.android.internal.session.SessionEventTracker
import io.hackle.android.internal.session.SessionManager
import io.hackle.android.internal.session.SessionUserEventDecorator
import io.hackle.android.internal.storage.DefaultFileStorage
import io.hackle.android.internal.sync.CompositeSynchronizer
import io.hackle.android.internal.sync.PollingSynchronizer
import io.hackle.android.internal.task.TaskExecutors
import io.hackle.android.internal.user.UserCohortFetcher
import io.hackle.android.internal.user.UserManager
import io.hackle.android.internal.user.UserTargetEventFetcher
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
import io.hackle.sdk.core.evaluation.evaluator.EvaluationEventRecorder
import io.hackle.sdk.core.evaluation.evaluator.inappmessage.eligibility.InAppMessageEligibilityFlowFactory
import io.hackle.sdk.core.evaluation.evaluator.inappmessage.layout.InAppMessageExperimentEvaluator
import io.hackle.sdk.core.evaluation.evaluator.inappmessage.layout.InAppMessageLayoutEvaluator
import io.hackle.sdk.core.evaluation.evaluator.inappmessage.layout.InAppMessageLayoutSelector
import io.hackle.sdk.core.evaluation.get
import io.hackle.sdk.core.event.UserEventFactory
import io.hackle.sdk.core.internal.log.Logger
import io.hackle.sdk.core.internal.log.metrics.MetricLoggerFactory
import io.hackle.sdk.core.internal.metrics.Metrics
import io.hackle.sdk.core.internal.scheduler.Schedulers
import io.hackle.sdk.core.internal.time.Clock
import okhttp3.OkHttpClient
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

internal object HackleApps {

    private val log = Logger<HackleApps>()

    private const val PREFERENCES_NAME = "io.hackle.android"

    fun create(context: Context, sdkKey: String, config: HackleConfig): HackleApp {
        val clock = Clock.SYSTEM
        val sdk = Sdk.of(sdkKey, config)
        loggerConfiguration(config)

        val globalKeyValueRepository = AndroidKeyValueRepository.create(context, PREFERENCES_NAME)
        val keyValueRepositoryBySdkKey =
            AndroidKeyValueRepository.create(context, "${PREFERENCES_NAME}_$sdkKey")
        val device = Device.create(context, globalKeyValueRepository)
        val applicationInstallDeterminer = ApplicationInstallDeterminer(globalKeyValueRepository, device)

        val httpClient = createHttpClient(context, sdk)

        // Lifecycle, AppState

        val lifecycleManager = LifecycleManager.instance
        val activityStateManager = ActivityStateManager.instance
        val applicationLifecycleManager = ApplicationLifecycleManager.instance
        val applicationStateManager = ApplicationStateManager.instance

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
        val targetEventFetcher = UserTargetEventFetcher(config.sdkUri, httpClient)
        val userManager = UserManager(
            device = device,
            repository = keyValueRepositoryBySdkKey,
            cohortFetcher = cohortFetcher,
            targetEventFetcher = targetEventFetcher
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
        val eventBackoffController = UserEventBackoffController(config.eventFlushIntervalMillis, clock)

        val eventDispatcher = EventDispatcher(
            baseEventUri = config.eventUri,
            eventExecutor = eventExecutor,
            eventRepository = eventRepository,
            httpExecutor = httpExecutor,
            httpClient = httpClient,
            eventBackoffController = eventBackoffController
        )

        val eventPublisher = UserEventPublisher()
        val screenUserEventDecorator = ScreenUserEventDecorator(screenManager)

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
            activityStateManager = activityStateManager,
            screenUserEventDecorator = screenUserEventDecorator,
            eventBackoffController = eventBackoffController
        )

        val rcEventDedupRepository =
            AndroidKeyValueRepository.create(context, "${PREFERENCES_NAME}_rc_event_dedup_$sdkKey")
        val exposureEventDedupRepository =
            AndroidKeyValueRepository.create(
                context,
                "${PREFERENCES_NAME}_exposure_event_dedup_$sdkKey"
            )

        val rcEventDedupDeterminer = RemoteConfigEventDedupDeterminer(
            rcEventDedupRepository,
            config.exposureEventDedupIntervalMillis.toLong(),
            clock
        )
        val exposureEventDedupDeterminer = ExposureEventDedupDeterminer(
            exposureEventDedupRepository,
            config.exposureEventDedupIntervalMillis.toLong(),
            clock
        )

        activityStateManager.addListener(rcEventDedupDeterminer)
        activityStateManager.addListener(exposureEventDedupDeterminer)

        val eventDedupDeterminer = DelegatingUserEventDedupDeterminer(
            listOf(
                rcEventDedupDeterminer,
                exposureEventDedupDeterminer
            )
        )
        val dedupUserEventFilter = DedupUserEventFilter(eventDedupDeterminer)
        eventProcessor.addFilter(dedupUserEventFilter)

        val sessionUserEventDecorator = SessionUserEventDecorator(sessionManager)
        eventProcessor.addDecorator(sessionUserEventDecorator)

        if (config.mode == HackleAppMode.WEB_VIEW_WRAPPER) {
            eventProcessor.addFilter(WebViewWrapperUserEventFilter())
            eventProcessor.addDecorator(WebViewWrapperUserEventDecorator())
        }

        // Evaluation Event

        val eventFactory = UserEventFactory(
            clock = clock
        )
        val evaluationEventRecorder = EvaluationEventRecorder(
            eventFactory = eventFactory,
            eventProcessor = eventProcessor
        )

        // Core

        val abOverrideStorage = create(context, "${PREFERENCES_NAME}_ab_override_$sdkKey")
        val ffOverrideStorage = create(context, "${PREFERENCES_NAME}_ff_override_$sdkKey")
        val inAppMessageHiddenStorage = AndroidInAppMessageHiddenStorage.create(
            context, "${PREFERENCES_NAME}_in_app_message_$sdkKey"
        )
        val inAppMessageImpressionStorage =
            AndroidInAppMessageImpressionStorage.create(
                context,
                "${PREFERENCES_NAME}_iam_impression_$sdkKey"
            )

        EvaluationContext.GLOBAL.register(inAppMessageHiddenStorage)
        EvaluationContext.GLOBAL.register(inAppMessageImpressionStorage)

        val core = HackleCore.create(
            context = EvaluationContext.GLOBAL,
            workspaceFetcher = workspaceManager,
            eventFactory = eventFactory,
            eventProcessor = eventProcessor,
            manualOverrideStorages = arrayOf(abOverrideStorage, ffOverrideStorage)
        )

        // ActivityStateListener

        activityStateManager.setExecutor(eventExecutor)
        activityStateManager.addListener(pollingSynchronizer)
        activityStateManager.addListener(sessionManager)
        activityStateManager.addListener(userManager)
        activityStateManager.addListener(eventProcessor, order = Ordered.LOWEST - 1)

        // ApplicationStateListener

        applicationStateManager.setExecutor(eventExecutor)
        applicationStateManager.setApplicationInstallDeterminer(applicationInstallDeterminer)

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

        val applicationEventTracker = ApplicationEventTracker(
            userManager = userManager,
            core = core,
            device = device
        )
        applicationStateManager.addListener(applicationEventTracker)

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
                InAppMessageHideActionHandler(inAppMessageHiddenStorage, clock)
            )
        )
        val inAppMessageEventProcessorFactory = InAppMessageEventProcessorFactory(
            processors = listOf(
                InAppMessageImpressionEventProcessor(),
                InAppMessageActionEventProcessor(inAppMessageActionHandlerFactory),
                InAppMessageCloseEventProcessor()
            )
        )
        val inAppMessageEventHandler = InAppMessageEventHandler(
            clock = clock,
            eventTracker = inAppMessageEventTracker,
            processorFactory = inAppMessageEventProcessorFactory
        )
        val imageLoader = GlideImageLoader()
        val inAppMessageUi = InAppMessageUi.create(
            activityProvider = lifecycleManager,
            messageControllerFactory = InAppMessageControllerFactory(InAppMessageViewFactory()),
            scheduler = Schedulers.executor(Executors.newSingleThreadScheduledExecutor()),
            eventHandler = inAppMessageEventHandler,
            imageLoader = imageLoader
        )

        val inAppMessageRecorder = InAppMessageRecorder(
            storage = inAppMessageImpressionStorage
        )
        val inAppMessagePresentProcessor = InAppMessagePresentProcessor(
            presenter = inAppMessageUi,
            recorder = inAppMessageRecorder
        )

        val inAppMessageLayoutEvaluator = InAppMessageLayoutEvaluator(
            experimentEvaluator = InAppMessageExperimentEvaluator(EvaluationContext.GLOBAL.get()),
            selector = InAppMessageLayoutSelector(),
            eventRecorder = evaluationEventRecorder
        )
        val inAppMessageEligibilityFlowFactory = InAppMessageEligibilityFlowFactory(
            context = EvaluationContext.GLOBAL,
            layoutEvaluator = inAppMessageLayoutEvaluator
        )

        val inAppMessageEvaluateProcessor = InAppMessageEvaluateProcessor(
            core = core,
            flowFactory = inAppMessageEligibilityFlowFactory,
            eventRecorder = evaluationEventRecorder
        )
        val inAppMessageIdentifierChecker = InAppMessageIdentifierChecker()

        val inAppMessageLayoutResolver = InAppMessageLayoutResolver(
            core = core,
            layoutEvaluator = inAppMessageLayoutEvaluator
        )

        val inAppMessageDeliverProcessor = InAppMessageDeliverProcessor(
            activityProvider = lifecycleManager,
            workspaceFetcher = workspaceManager,
            userManager = userManager,
            identifierChecker = inAppMessageIdentifierChecker,
            layoutResolver = inAppMessageLayoutResolver,
            evaluateProcessor = inAppMessageEvaluateProcessor,
            presentProcessor = inAppMessagePresentProcessor
        )

        val inAppMessageDelayScheduler = InAppMessageDelayScheduler(
            clock = clock,
            scheduler = Schedulers.executor(Executors.newSingleThreadScheduledExecutor())
        )
        val inAppMessageDelayManager = InAppMessageDelayManager(
            scheduler = inAppMessageDelayScheduler,
            tasks = ConcurrentHashMap()
        )

        val inAppMessageSchedulerFactory = InAppMessageSchedulerFactory(
            listOf(
                TriggeredInAppMessageScheduler(inAppMessageDeliverProcessor, inAppMessageDelayManager),
                DelayedInAppMessageScheduler(inAppMessageDeliverProcessor, inAppMessageDelayManager)
            )
        )
        val inAppMessageScheduleProcessor = InAppMessageScheduleProcessor(
            actionDeterminer = InAppMessageScheduleActionDeterminer(),
            schedulerFactory = inAppMessageSchedulerFactory
        )
        inAppMessageDelayScheduler.listener = inAppMessageScheduleProcessor

        val inAppMessageEventMatcher = InAppMessageEventMatcher(
            ruleMatcher = InAppMessageEventTriggerRuleMatcher(EvaluationContext.GLOBAL.get()),
        )
        val inAppMessageTriggerDeterminer = InAppMessageTriggerDeterminer(
            workspaceFetcher = workspaceManager,
            eventMatcher = inAppMessageEventMatcher,
            evaluateProcessor = inAppMessageEvaluateProcessor
        )
        val inAppMessageTriggerHandler = InAppMessageTriggerHandler(
            scheduleProcessor = inAppMessageScheduleProcessor
        )
        val inAppMessageTriggerProcessor = InAppMessageTriggerProcessor(
            determiner = inAppMessageTriggerDeterminer,
            handler = inAppMessageTriggerHandler
        )
        val inAppMessageResetProcessor = InAppMessageResetProcessor(
            identifierChecker = inAppMessageIdentifierChecker,
            delayManager = inAppMessageDelayManager
        )
        val inAppMessageManager = InAppMessageManager(
            triggerProcessor = inAppMessageTriggerProcessor,
            resetProcessor = inAppMessageResetProcessor
        )

        if (config.inAppMessageEnabled) {
            eventPublisher.add(inAppMessageManager)
            userManager.addListener(inAppMessageManager)
        }

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
        val devToolsApi = DevToolsApi(
            sdk = sdk,
            url = config.apiUrl,
            httpClient = httpClient
        )

        val userExplorer = HackleUserExplorer(
            explorerService = HackleUserExplorerService(
                core = core,
                userManager = userManager,
                abTestOverrideStorage = abOverrideStorage,
                featureFlagOverrideStorage = ffOverrideStorage,
                pushTokenManager = pushTokenManager,
                devToolsApi = devToolsApi,
            ),
            activityProvider = lifecycleManager
        )

        // Metrics

        metricConfiguration(config, activityStateManager, eventExecutor, httpExecutor, httpClient)

        // LifecycleListener

        if (config.automaticScreenTracking) {
            lifecycleManager.addListener(screenManager, order = Ordered.HIGHEST)
        }
        lifecycleManager.addListener(engagementManager, order = Ordered.HIGHEST + 1)
        lifecycleManager.addListener(inAppMessageUi, order = Ordered.LOWEST)
        lifecycleManager.addListener(userExplorer, order = Ordered.LOWEST - 1)
        lifecycleManager.registerTo(context)

        applicationLifecycleManager.registerTo(context)

        val throttleLimiter = ThrottleLimiter(
            intervalMillis = 60 * 1000,
            limit = 1,
            clock = clock
        )
        val fetchThrottler = Throttler(throttleLimiter)

        // Instantiate
        val hackleAppCore = HackleAppCore(
            clock = clock,
            core = core,
            eventExecutor = eventExecutor,
            backgroundExecutor = TaskExecutors.default(),
            synchronizer = pollingSynchronizer,
            userManager = userManager,
            workspaceManager = workspaceManager,
            sessionManager = sessionManager,
            screenManager = screenManager,
            eventProcessor = eventProcessor,
            pushTokenManager = pushTokenManager,
            notificationManager = notificationManager,
            fetchThrottler = fetchThrottler,
            device = device,
            userExplorer = userExplorer,
        )

        val hackleInvocator = HackleInvocatorImpl(hackleAppCore)

        return HackleApp(
            hackleAppCore = hackleAppCore,
            sdk = sdk,
            mode = config.mode,
            invocator = hackleInvocator
        )
    }

    private fun loggerConfiguration(config: HackleConfig) {
        Logger.add(AndroidLogger.Factory.logLevel(config.logLevel))
        Logger.add(MetricLoggerFactory(Metrics.globalRegistry))
    }

    private fun metricConfiguration(
        config: HackleConfig,
        activityStateManager: ActivityStateManager,
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

        activityStateManager.addListener(monitoringMetricRegistry, order = Ordered.LOWEST)
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

    private val HackleConfig.inAppMessageEnabled: Boolean
        get() {
            val disableFlag = get("\$disable_inappmessage") ?: return true
            return disableFlag.toBoolean()
        }
}
