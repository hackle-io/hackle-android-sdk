package io.hackle.android.internal.workspace


import io.hackle.sdk.core.internal.log.Logger
import io.hackle.sdk.core.internal.utils.enumValueOfOrNull
import io.hackle.sdk.core.model.*
import io.hackle.sdk.core.model.Target

private val log = Logger<WorkspaceImpl>()

// Experiment
internal fun ExperimentDto.toExperimentOrNull(type: Experiment.Type): Experiment? {

    val variations = variations.map { it.toVariation() }
    val overrides = execution.userOverrides.associate { it.userId to it.variationId }

    return when (execution.status) {
        "READY" -> Experiment.Draft(
            id = id,
            key = key,
            type = type,
            variations = variations,
            overrides = overrides
        )
        "RUNNING" -> Experiment.Running(
            id = id,
            key = key,
            type = type,
            variations = variations,
            overrides = overrides,
            targetAudiences = execution.targetAudiences.mapNotNull { it.toTargetOrNull() },
            targetRules = execution.targetRules.mapNotNull { it.toTargetRuleOrNull() },
            defaultRule = execution.defaultRule.toActionOrNull() ?: return null
        )
        "PAUSED" -> Experiment.Paused(
            id = id,
            key = key,
            type = type,
            variations = variations,
            overrides = overrides
        )
        "STOPPED" -> Experiment.Completed(
            id = id,
            key = key,
            type = type,
            variations = variations,
            overrides = overrides,
            winnerVariationId = requireNotNull(winnerVariationId)
        )
        else -> {
            log.debug { "Unknown experiment status [$status]" }
            null
        }
    }
}

internal fun VariationDto.toVariation() = Variation(
    id = id,
    key = key,
    isDropped = status == "DROPPED"
)

internal fun TargetDto.toTargetOrNull(): Target? {
    val conditions = conditions.mapNotNull { it.toConditionOrNull() }
    return if (conditions.isEmpty()) {
        null
    } else {
        Target(conditions)
    }
}

internal fun TargetDto.ConditionDto.toConditionOrNull(): Target.Condition? {
    return Target.Condition(
        key = key.toTargetKeyOrNull() ?: return null,
        match = match.toMatchOrNull() ?: return null
    )

}

internal fun TargetDto.KeyDto.toTargetKeyOrNull(): Target.Key? {
    return Target.Key(
        type = parseEnumOrNull<Target.Key.Type>(type) ?: return null,
        name = name
    )
}

internal fun TargetDto.MatchDto.toMatchOrNull(): Target.Match? {
    return Target.Match(
        type = parseEnumOrNull<Target.Match.Type>(type) ?: return null,
        operator = parseEnumOrNull<Target.Match.Operator>(operator) ?: return null,
        valueType = parseEnumOrNull<Target.Match.ValueType>(valueType) ?: return null,
        values = values
    )
}

internal fun TargetActionDto.toActionOrNull(): Action? {
    return when (type) {
        "VARIATION" -> Action.Variation(requireNotNull(variationId))
        "BUCKET" -> Action.Bucket(requireNotNull(bucketId))
        else -> {
            log.debug { "Unsupported action type[$type]. Please use the latest version of sdk" }
            return null
        }
    }
}

internal fun TargetRuleDto.toTargetRuleOrNull(): TargetRule? {
    return TargetRule(
        target = target.toTargetOrNull() ?: return null,
        action = action.toActionOrNull() ?: return null,
    )
}


private inline fun <reified E : Enum<E>> parseEnumOrNull(name: String): E? {
    val enum = enumValueOfOrNull<E>(name)
    if (enum == null) {
        log.debug { "Unsupported type[${E::class.java.name}.$name]. Please use the latest version of sdk." }
        return null
    }
    return enum
}

// Bucket
internal fun BucketDto.toBucket() = Bucket(
    seed = seed,
    slotSize = slotSize,
    slots = slots.map { it.toSlot() }
)

internal fun SlotDto.toSlot() = Slot(
    startInclusive = startInclusive,
    endExclusive = endExclusive,
    variationId = variationId
)

// EventType
internal fun EventTypeDto.toEventType() = EventType.Custom(id, key)
