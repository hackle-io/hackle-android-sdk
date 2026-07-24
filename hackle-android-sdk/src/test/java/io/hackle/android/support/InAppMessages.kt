package io.hackle.android.support

import io.hackle.android.internal.inappmessage.deliver.InAppMessageDeliverRequest
import io.hackle.android.internal.inappmessage.present.InAppMessagePresentRequest
import io.hackle.android.internal.inappmessage.present.presentation.InAppMessagePresentationContext
import io.hackle.android.internal.inappmessage.schedule.InAppMessageSchedule
import io.hackle.sdk.common.Event
import io.hackle.sdk.common.User
import io.hackle.sdk.common.decision.DecisionReason
import io.hackle.sdk.core.evaluation.Evaluation
import io.hackle.sdk.core.evaluation.EvaluationPhase
import io.hackle.sdk.core.evaluation.service.experiment.ExperimentEvaluation
import io.hackle.sdk.core.evaluation.service.inappmessage.InAppMessageEvaluateScope
import io.hackle.sdk.core.evaluation.service.inappmessage.eligibility.InAppMessageEligibilityEvaluateResponse
import io.hackle.sdk.core.evaluation.service.inappmessage.eligibility.InAppMessageEligibilityEvaluateResult
import io.hackle.sdk.core.evaluation.service.inappmessage.eligibility.InAppMessageEligibilityEvaluation
import io.hackle.sdk.core.evaluation.service.inappmessage.eligibility.match.InAppMessageImpression
import io.hackle.sdk.core.evaluation.service.inappmessage.eligibility.mode.local.InAppMessageEligibilityLocalEvaluateRequest
import io.hackle.sdk.core.evaluation.service.inappmessage.eligibility.mode.remote.InAppMessageEligibilityRemoteEvaluateRequest
import io.hackle.sdk.core.evaluation.service.inappmessage.layout.InAppMessageLayoutEvaluateResponse
import io.hackle.sdk.core.evaluation.service.inappmessage.layout.InAppMessageLayoutEvaluateResult
import io.hackle.sdk.core.evaluation.service.inappmessage.layout.InAppMessageLayoutEvaluation
import io.hackle.sdk.core.evaluation.service.inappmessage.layout.mode.local.InAppMessageLayoutLocalEvaluateRequest
import io.hackle.sdk.core.evaluation.service.inappmessage.layout.mode.remote.InAppMessageLayoutRemoteEvaluateRequest
import io.hackle.sdk.core.model.*
import io.hackle.sdk.core.model.Target
import io.hackle.sdk.core.user.HackleUser
import io.hackle.sdk.core.user.IdentifierType
import io.hackle.sdk.core.workspace.Workspace
import io.hackle.sdk.core.workspace.config.WorkspaceConfig
import io.hackle.sdk.core.workspace.config.entity.InAppMessageConfig
import io.hackle.sdk.core.workspace.evaluation.WorkspaceEvaluation
import io.hackle.sdk.core.workspace.evaluation.entity.InAppMessageEligibilityRemoteEvaluateResult
import io.hackle.sdk.core.workspace.evaluation.entity.InAppMessageLayoutRemoteEvaluateResult
import java.util.UUID

internal object InAppMessages {

    // ── LOCAL 완성품 ──

    fun config(
        id: Long = 1,
        key: Long = 1,
        order: Long = id,
        status: InAppMessage.Status = InAppMessage.Status.ACTIVE,
        period: InAppMessage.Period = InAppMessage.Period.Always,
        timetable: InAppMessage.Timetable = InAppMessage.Timetable.All,
        eventTrigger: InAppMessage.EventTrigger = eventTrigger(),
        evaluateContext: InAppMessage.EvaluateContext = evaluateContext(),
        messageContext: InAppMessage.MessageContext = messageContext(),
        targetContext: InAppMessage.TargetContext = targetContext(),
    ): InAppMessageConfig {
        return InAppMessageConfig(
            id = id,
            key = key,
            order = order,
            period = period,
            timetable = timetable,
            eventTrigger = eventTrigger,
            evaluateContext = evaluateContext,
            messageContext = messageContext,
            status = status,
            targetContext = targetContext
        )
    }

    // ── REMOTE 완성품 ──

    fun eligibilityRemoteResult(
        id: Long = 1,
        key: Long = 1,
        order: Long = id,
        period: InAppMessage.Period = InAppMessage.Period.Always,
        timetable: InAppMessage.Timetable = InAppMessage.Timetable.All,
        eventTrigger: InAppMessage.EventTrigger = eventTrigger(),
        evaluateContext: InAppMessage.EvaluateContext = evaluateContext(),
        messageContext: InAppMessage.MessageContext = messageContext(),
        isEligible: Boolean = true,
        reason: DecisionReason = DecisionReason.IN_APP_MESSAGE_TARGET,
        references: List<Entity> = emptyList(),
        layout: InAppMessageLayoutRemoteEvaluateResult = layoutRemoteResult(
            id = id,
            key = key,
            order = order,
            period = period,
            timetable = timetable,
            eventTrigger = eventTrigger,
            evaluateContext = evaluateContext,
            messageContext = messageContext
        ),
    ): InAppMessageEligibilityRemoteEvaluateResult {
        return InAppMessageEligibilityRemoteEvaluateResult(
            id = id,
            key = key,
            order = order,
            period = period,
            timetable = timetable,
            eventTrigger = eventTrigger,
            evaluateContext = evaluateContext,
            messageContext = messageContext,
            isEligible = isEligible,
            reason = reason,
            references = references,
            layout = layout
        )
    }

    fun layoutRemoteResult(
        id: Long = 1,
        key: Long = 1,
        order: Long = id,
        period: InAppMessage.Period = InAppMessage.Period.Always,
        timetable: InAppMessage.Timetable = InAppMessage.Timetable.All,
        eventTrigger: InAppMessage.EventTrigger = eventTrigger(),
        evaluateContext: InAppMessage.EvaluateContext = evaluateContext(),
        messageContext: InAppMessage.MessageContext = messageContext(),
        message: InAppMessage.Message = messageContext.messages.first(),
        reason: DecisionReason = DecisionReason.IN_APP_MESSAGE_TARGET,
        references: List<Entity> = emptyList(),
    ): InAppMessageLayoutRemoteEvaluateResult {
        return InAppMessageLayoutRemoteEvaluateResult(
            id = id,
            key = key,
            order = order,
            period = period,
            timetable = timetable,
            eventTrigger = eventTrigger,
            evaluateContext = evaluateContext,
            messageContext = messageContext,
            message = message,
            reason = reason,
            references = references
        )
    }

    // ── Request ──

    fun eligibilityLocalRequest(
        workspace: WorkspaceConfig = Workspaces.config(),
        inAppMessage: InAppMessageConfig = config(),
        user: HackleUser = HackleUser.builder().identifier(IdentifierType.ID, "user").build(),
        scope: InAppMessageEvaluateScope = InAppMessageEvaluateScope.TRIGGER,
        platformType: PlatformType? = PlatformType.ANDROID,
        timestamp: Long = 42,
        phase: EvaluationPhase = EvaluationPhase.RUNTIME,
        record: Boolean = true,
    ): InAppMessageEligibilityLocalEvaluateRequest {
        return InAppMessageEligibilityLocalEvaluateRequest.of(
            workspace = workspace,
            entity = inAppMessage,
            user = user,
            scope = scope,
            platformType = platformType,
            timestamp = timestamp,
            phase = phase,
            record = record
        )
    }

    fun eligibilityRemoteRequest(
        workspace: WorkspaceEvaluation = Workspaces.evaluation(),
        inAppMessage: InAppMessageEligibilityRemoteEvaluateResult = eligibilityRemoteResult(),
        user: HackleUser = HackleUser.builder().identifier(IdentifierType.ID, "user").build(),
        scope: InAppMessageEvaluateScope = InAppMessageEvaluateScope.TRIGGER,
        platformType: PlatformType? = PlatformType.ANDROID,
        timestamp: Long = 42,
        record: Boolean = true,
    ): InAppMessageEligibilityRemoteEvaluateRequest {
        return InAppMessageEligibilityRemoteEvaluateRequest.of(
            workspace = workspace,
            entity = inAppMessage,
            user = user,
            scope = scope,
            platformType = platformType,
            timestamp = timestamp,
            record = record
        )
    }

    fun layoutLocalRequest(
        workspace: WorkspaceConfig = Workspaces.config(),
        inAppMessage: InAppMessageConfig = config(),
        user: HackleUser = HackleUser.builder().identifier(IdentifierType.ID, "user").build(),
        scope: InAppMessageEvaluateScope = InAppMessageEvaluateScope.TRIGGER,
        phase: EvaluationPhase = EvaluationPhase.RUNTIME,
        record: Boolean = true,
    ): InAppMessageLayoutLocalEvaluateRequest {
        return InAppMessageLayoutLocalEvaluateRequest.of(
            workspace = workspace,
            entity = inAppMessage,
            user = user,
            scope = scope,
            phase = phase,
            record = record
        )
    }

    fun layoutRemoteRequest(
        workspace: WorkspaceEvaluation = Workspaces.evaluation(),
        inAppMessage: InAppMessageLayoutRemoteEvaluateResult = layoutRemoteResult(),
        user: HackleUser = HackleUser.builder().identifier(IdentifierType.ID, "user").build(),
        scope: InAppMessageEvaluateScope = InAppMessageEvaluateScope.TRIGGER,
        record: Boolean = true,
    ): InAppMessageLayoutRemoteEvaluateRequest {
        return InAppMessageLayoutRemoteEvaluateRequest.of(
            workspace = workspace,
            entity = inAppMessage,
            user = user,
            scope = scope,
            record = record
        )
    }

    // ── Result / Evaluation / Response ──

    fun eligibilityResult(
        isEligible: Boolean = true,
        reason: DecisionReason = DecisionReason.IN_APP_MESSAGE_TARGET,
    ): InAppMessageEligibilityEvaluateResult {
        return if (isEligible) {
            InAppMessageEligibilityEvaluateResult.eligible(reason)
        } else {
            InAppMessageEligibilityEvaluateResult.ineligible(reason)
        }
    }

    fun eligibilityEvaluation(
        inAppMessage: InAppMessage = config(),
        result: InAppMessageEligibilityEvaluateResult = eligibilityResult(),
    ): InAppMessageEligibilityEvaluation {
        return InAppMessageEligibilityEvaluation(entity = inAppMessage, result = result)
    }

    fun eligibilityResponse(
        user: HackleUser = HackleUser.builder().identifier(IdentifierType.ID, "user").build(),
        workspace: Workspace = Workspaces.config(),
        evaluation: InAppMessageEligibilityEvaluation = eligibilityEvaluation(),
        references: List<Evaluation> = emptyList(),
        layout: InAppMessageLayoutEvaluateResponse? = null,
    ): InAppMessageEligibilityEvaluateResponse {
        return InAppMessageEligibilityEvaluateResponse(
            user = user,
            workspace = workspace,
            evaluation = evaluation,
            references = references,
            layout = layout
        )
    }

    fun layoutResult(
        reason: DecisionReason = DecisionReason.IN_APP_MESSAGE_TARGET,
        message: InAppMessage.Message = message(),
    ): InAppMessageLayoutEvaluateResult {
        return InAppMessageLayoutEvaluateResult.of(reason, message)
    }

    fun layoutEvaluation(
        inAppMessage: InAppMessage = config(),
        result: InAppMessageLayoutEvaluateResult = layoutResult(),
    ): InAppMessageLayoutEvaluation {
        return InAppMessageLayoutEvaluation(entity = inAppMessage, result = result)
    }

    fun layoutResponse(
        user: HackleUser = HackleUser.builder().identifier(IdentifierType.ID, "user").build(),
        workspace: Workspace = Workspaces.config(),
        evaluation: InAppMessageLayoutEvaluation = layoutEvaluation(),
        references: List<Evaluation> = emptyList(),
        experiment: ExperimentEvaluation? = null,
    ): InAppMessageLayoutEvaluateResponse {
        return InAppMessageLayoutEvaluateResponse(
            user = user,
            workspace = workspace,
            evaluation = evaluation,
            references = references,
            experiment = experiment
        )
    }

    // ── 공통 부품 ──

    fun eventTrigger(
        rules: List<InAppMessage.EventTrigger.Rule> = listOf(InAppMessage.EventTrigger.Rule("test", emptyList())),
        frequencyCap: InAppMessage.EventTrigger.FrequencyCap? = null,
        delay: InAppMessage.Delay = delay(),
    ): InAppMessage.EventTrigger {
        return InAppMessage.EventTrigger(rules = rules, frequencyCap = frequencyCap, delay = delay)
    }

    fun eventTriggerRule(
        eventKey: String = "test",
        targets: List<Target> = emptyList(),
    ): InAppMessage.EventTrigger.Rule {
        return InAppMessage.EventTrigger.Rule(eventKey, targets)
    }

    fun frequencyCap(
        identifierCaps: List<InAppMessage.EventTrigger.IdentifierCap> = emptyList(),
        durationCap: InAppMessage.EventTrigger.DurationCap? = null,
    ): InAppMessage.EventTrigger.FrequencyCap {
        return InAppMessage.EventTrigger.FrequencyCap(identifierCaps, durationCap)
    }

    fun identifierCap(
        identifierType: String = "\$id",
        count: Int = 1,
    ): InAppMessage.EventTrigger.IdentifierCap {
        return InAppMessage.EventTrigger.IdentifierCap(identifierType, count)
    }

    fun durationCap(
        durationMillis: Long = 60,
        count: Int = 1,
    ): InAppMessage.EventTrigger.DurationCap {
        return InAppMessage.EventTrigger.DurationCap(durationMillis, count)
    }

    fun delay(
        type: InAppMessage.Delay.Type = InAppMessage.Delay.Type.IMMEDIATE,
        afterCondition: InAppMessage.Delay.AfterCondition? = null,
    ): InAppMessage.Delay {
        return InAppMessage.Delay(type, afterCondition)
    }

    fun afterCondition(
        durationMillis: Long = 1000,
    ): InAppMessage.Delay.AfterCondition {
        return InAppMessage.Delay.AfterCondition(durationMillis)
    }

    fun evaluateContext(
        atDeliverTime: Boolean = false,
    ): InAppMessage.EvaluateContext {
        return InAppMessage.EvaluateContext(atDeliverTime)
    }

    fun experimentContext(
        key: Long = 1,
    ): InAppMessage.ExperimentContext {
        return InAppMessage.ExperimentContext(key)
    }

    fun targetContext(
        targets: List<Target> = emptyList(),
        overrides: List<InAppMessage.UserOverride> = emptyList(),
    ): InAppMessage.TargetContext {
        return InAppMessage.TargetContext(targets, overrides)
    }

    fun userOverride(
        identifierType: String = "\$id",
        identifiers: List<String> = listOf("user"),
    ): InAppMessage.UserOverride {
        return InAppMessage.UserOverride(identifierType, identifiers)
    }

    fun messageContext(
        defaultLang: String = "ko",
        experimentContext: InAppMessage.ExperimentContext? = null,
        platformTypes: List<PlatformType> = listOf(PlatformType.ANDROID),
        orientations: List<InAppMessage.Orientation> = listOf(InAppMessage.Orientation.VERTICAL),
        messages: List<InAppMessage.Message> = listOf(message()),
    ): InAppMessage.MessageContext {
        return InAppMessage.MessageContext(
            defaultLang = defaultLang,
            experimentContext = experimentContext,
            platformTypes = platformTypes,
            orientations = orientations,
            messages = messages
        )
    }

    fun message(
        variationKey: String? = null,
        lang: String = "ko",
        layout: InAppMessage.Message.Layout = layout(),
        images: List<InAppMessage.Message.Image> = listOf(image()),
        imageAutoScroll: InAppMessage.Message.ImageAutoScroll? = null,
        text: InAppMessage.Message.Text? = text(),
        buttons: List<InAppMessage.Message.Button> = listOf(button()),
        closeButton: InAppMessage.Message.Button? = null,
        background: InAppMessage.Message.Background = InAppMessage.Message.Background("#FFFFFF"),
        action: InAppMessage.Action? = null,
        outerButtons: List<InAppMessage.Message.PositionalButton> = emptyList(),
        innerButtons: List<InAppMessage.Message.PositionalButton> = emptyList(),
        html: InAppMessage.Message.Html? = null,
    ): InAppMessage.Message {
        return InAppMessage.Message(
            variationKey = variationKey,
            lang = lang,
            layout = layout,
            images = images,
            imageAutoScroll = imageAutoScroll,
            text = text,
            buttons = buttons,
            closeButton = closeButton,
            background = background,
            action = action,
            outerButtons = outerButtons,
            innerButtons = innerButtons,
            html = html
        )
    }

    fun layout(
        displayType: InAppMessage.DisplayType = InAppMessage.DisplayType.MODAL,
        layoutType: InAppMessage.LayoutType = InAppMessage.LayoutType.IMAGE_ONLY,
        alignment: InAppMessage.Message.Alignment? = null,
    ): InAppMessage.Message.Layout {
        return InAppMessage.Message.Layout(
            displayType = displayType,
            layoutType = layoutType,
            alignment = alignment
        )
    }

    fun action(
        behavior: InAppMessage.Behavior = InAppMessage.Behavior.CLICK,
        type: InAppMessage.ActionType = InAppMessage.ActionType.CLOSE,
        value: String? = null,
    ): InAppMessage.Action {
        return InAppMessage.Action(
            behavior = behavior,
            actionType = type,
            value = value
        )
    }

    fun button(
        text: String = "button",
        textColor: String = "#000000",
        bgColor: String = "#FFFFFF",
        borderColor: String = "#FFFFFF",
        action: InAppMessage.Action = action(),
    ): InAppMessage.Message.Button {
        return InAppMessage.Message.Button(
            text = text,
            style = InAppMessage.Message.Button.Style(
                textColor = textColor,
                bgColor = bgColor,
                borderColor = borderColor
            ),
            action = action
        )
    }

    fun image(
        orientation: InAppMessage.Orientation = InAppMessage.Orientation.VERTICAL,
        imagePath: String = "image_path",
        action: InAppMessage.Action? = null,
    ): InAppMessage.Message.Image {
        return InAppMessage.Message.Image(
            orientation = orientation,
            imagePath = imagePath,
            action = action
        )
    }

    fun text(
        title: String = "title",
        titleColor: String = "#000000",
        body: String = "body",
        bodyColor: String = "#FFFFFF",
    ): InAppMessage.Message.Text {
        return InAppMessage.Message.Text(
            title = InAppMessage.Message.Text.Attribute(title, InAppMessage.Message.Text.Style(titleColor)),
            body = InAppMessage.Message.Text.Attribute(body, InAppMessage.Message.Text.Style(bodyColor))
        )
    }

    fun impression(
        identifiers: Map<String, String> = mapOf("\$id" to "user"),
        timestamp: Long = 42,
    ): InAppMessageImpression {
        return InAppMessageImpression(identifiers, timestamp)
    }

    // ── Android 완성품 ──

    fun schedule(
        dispatchId: String = UUID.randomUUID().toString(),
        inAppMessageKey: Long = 1,
        identifiers: Identifiers = Identifiers.from(User.builder().deviceId("device_id").build()),
        time: InAppMessageSchedule.Time = InAppMessageSchedule.Time(42, 42),
        reason: DecisionReason = DecisionReason.IN_APP_MESSAGE_TARGET,
        eventBasedContext: InAppMessageSchedule.EventBasedContext = InAppMessageSchedule.EventBasedContext(
            UUID.randomUUID().toString(), Event.of("test")
        ),
    ): InAppMessageSchedule {
        return InAppMessageSchedule(dispatchId, inAppMessageKey, identifiers, time, reason, eventBasedContext)
    }

    fun deliverRequest(
        dispatchId: String = UUID.randomUUID().toString(),
        inAppMessageKey: Long = 1,
        identifiers: Identifiers = Identifiers.from(User.builder().deviceId("device_id").build()),
        requestedAt: Long = 42,
        reason: DecisionReason = DecisionReason.IN_APP_MESSAGE_TARGET,
        properties: Map<String, Any> = emptyMap(),
        triggerEvent: Event = Event.of("test"),
    ): InAppMessageDeliverRequest {
        return InAppMessageDeliverRequest(
            dispatchId,
            inAppMessageKey,
            identifiers,
            requestedAt,
            reason,
            properties,
            triggerEvent
        )
    }

    fun presentRequest(
        dispatchId: String = UUID.randomUUID().toString(),
        inAppMessage: InAppMessage = config(),
        message: InAppMessage.Message = inAppMessage.messageContext.messages.first(),
        user: HackleUser = HackleUser.builder().identifier(IdentifierType.ID, "user").build(),
        requestedAt: Long = 42,
        reason: DecisionReason = DecisionReason.IN_APP_MESSAGE_TARGET,
        properties: Map<String, Any> = emptyMap(),
        triggerEvent: Event = Event.of("test"),
    ): InAppMessagePresentRequest {
        return InAppMessagePresentRequest(
            dispatchId,
            inAppMessage,
            message,
            user,
            requestedAt,
            reason,
            properties,
            triggerEvent
        )
    }

    fun context(
        dispatchId: String = UUID.randomUUID().toString(),
        inAppMessage: InAppMessage = config(),
        message: InAppMessage.Message = inAppMessage.messageContext.messages.first(),
        user: HackleUser = HackleUser.builder().identifier(IdentifierType.ID, "user").build(),
        decisionReason: DecisionReason = DecisionReason.IN_APP_MESSAGE_TARGET,
        properties: Map<String, Any> = emptyMap(),
        triggerEvent: Event = Event.of("test"),
    ): InAppMessagePresentationContext {
        return InAppMessagePresentationContext(
            dispatchId,
            inAppMessage,
            message,
            user,
            decisionReason,
            properties,
            triggerEvent
        )
    }
}
