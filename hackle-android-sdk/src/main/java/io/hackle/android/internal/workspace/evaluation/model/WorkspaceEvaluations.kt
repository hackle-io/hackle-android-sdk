package io.hackle.android.internal.workspace.evaluation.model

import io.hackle.android.internal.workspace.config.*
import io.hackle.sdk.common.decision.DecisionReason
import io.hackle.sdk.core.model.*
import io.hackle.sdk.core.workspace.evaluation.entity.ExperimentRemoteEvaluateResult
import io.hackle.sdk.core.workspace.evaluation.entity.InAppMessageEligibilityRemoteEvaluateResult
import io.hackle.sdk.core.workspace.evaluation.entity.InAppMessageLayoutRemoteEvaluateResult
import io.hackle.sdk.core.workspace.evaluation.entity.RemoteConfigParameterRemoteEvaluateResult


internal fun ExperimentEvaluateResultDto.toResultOrNull(experimentType: Experiment.Type): ExperimentRemoteEvaluateResult? {
    return ExperimentRemoteEvaluateResult(
        id = id,
        key = key,
        status = Experiment.Status.from(execution.status) ?: return null,
        order = order,
        type = experimentType,
        version = version,
        executionVersion = execution.version,
        variation = variation.toVariation(config?.toParameterConfiguration()),
        reason = DecisionReason.from(reason),
        references = references.mapNotNull { it.toEntityOrNull() }
    )
}

internal fun RemoteConfigParameterEvaluateResultDto.toResultOrNull(): RemoteConfigParameterRemoteEvaluateResult? {
    val valueType = parseEnumOrNull<ValueType>(valueType) ?: return null
    return RemoteConfigParameterRemoteEvaluateResult(
        id = id,
        key = key,
        type = valueType,
        value = value?.toValue(),
        reason = DecisionReason.from(reason),
        references = references.mapNotNull { it.toEntityOrNull() }
    )
}

internal fun InAppMessageEligibilityEvaluateResultDto.toResultOrNull(): InAppMessageEligibilityRemoteEvaluateResult? {
    val period = period?.let { it.toPeriodOrNull() ?: return null } ?: InAppMessage.Period.Always
    val timetable = timetable?.let { it.toTimetableOrNull() ?: return null } ?: InAppMessage.Timetable.All
    val eventTrigger = InAppMessage.EventTrigger(
        rules = eventTriggerRules.map { it.toTriggerRule() },
        frequencyCap = eventFrequencyCap?.toFrequencyCap(),
        delay = eventTriggerDelay
            ?.let { it.toDelayOrNull() ?: return null }
            ?: InAppMessage.Delay(InAppMessage.Delay.Type.IMMEDIATE, null)
    )
    val evaluateContext = InAppMessage.EvaluateContext(atDeliverTime = evaluateContext?.atDeliverTime ?: false)
    val messageContext = messageContext.toMessageContextOrNull() ?: return null

    val layout = InAppMessageLayoutRemoteEvaluateResult(
        id = id,
        key = key,
        order = order,
        period = period,
        timetable = timetable,
        eventTrigger = eventTrigger,
        evaluateContext = evaluateContext,
        messageContext = messageContext,
        message = layout.message.toMessageOrNull() ?: return null,
        reason = DecisionReason.from(layout.reason),
        references = layout.references.mapNotNull { it.toEntityOrNull() },
    )

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
        reason = DecisionReason.from(reason),
        references = references.mapNotNull { it.toEntityOrNull() },
        layout = layout
    )
}

internal fun EntityDto.toEntityOrNull(): Entity? {
    val serviceType = parseEnumOrNull<ServiceType>(type) ?: return null
    return DefaultEntity(serviceType, id)
}
