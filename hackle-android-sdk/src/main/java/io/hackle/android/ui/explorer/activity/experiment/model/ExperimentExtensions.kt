package io.hackle.android.ui.explorer.activity.experiment.model

import io.hackle.sdk.common.decision.DecisionReason
import io.hackle.sdk.common.decision.DecisionReason.*
import io.hackle.sdk.core.model.Experiment

internal val Experiment.Type.position get() = ordinal
internal fun experimentType(position: Int): Experiment.Type = Experiment.Type.values()[position]

internal val DecisionReason.isManualOverridable
    get() = when (this) {
        SDK_NOT_READY,
        EXCEPTION,
        INVALID_INPUT,
        EXPERIMENT_NOT_FOUND,
        IDENTIFIER_NOT_FOUND,
        FEATURE_FLAG_NOT_FOUND,
        FEATURE_FLAG_INACTIVE,
        REMOTE_CONFIG_PARAMETER_NOT_FOUND,
        TYPE_MISMATCH,
        IN_APP_MESSAGE_NOT_FOUND,
        UNSUPPORTED_PLATFORM,
        IN_APP_MESSAGE_DRAFT,
        IN_APP_MESSAGE_PAUSED,
        IN_APP_MESSAGE_HIDDEN,
        IN_APP_MESSAGE_TARGET,
        NOT_IN_IN_APP_MESSAGE_PERIOD,
        NOT_IN_IN_APP_MESSAGE_TARGET,
        IN_APP_MESSAGE_FREQUENCY_CAPPED,
        EXPERIMENT_CONTROL_GROUP
        -> false

        EXPERIMENT_DRAFT,
        EXPERIMENT_PAUSED,
        EXPERIMENT_COMPLETED,
        OVERRIDDEN,
        TRAFFIC_NOT_ALLOCATED,
        TRAFFIC_ALLOCATED,
        TRAFFIC_ALLOCATED_BY_TARGETING,
        NOT_IN_MUTUAL_EXCLUSION_EXPERIMENT,
        VARIATION_DROPPED,
        NOT_IN_EXPERIMENT_TARGET,
        INDIVIDUAL_TARGET_MATCH,
        TARGET_RULE_MATCH,
        DEFAULT_RULE,
        -> true
    }
