package io.hackle.android.internal.bridge.model

import com.google.gson.annotations.SerializedName
import io.hackle.sdk.common.HackleExperiment
import io.hackle.sdk.common.User
import io.hackle.sdk.common.decision.Decision
import io.hackle.sdk.common.decision.FeatureFlagDecision

internal data class UserDto(
    @SerializedName("id")
    val id: String?,
    @SerializedName("userId")
    val userId: String?,
    @SerializedName("deviceId")
    val deviceId: String?,
    @SerializedName("identifiers")
    val identifiers: Map<String, String>,
    @SerializedName("properties")
    val properties: Map<String, Any>,
)

internal fun User.toDto() = UserDto(
    id = id,
    userId = userId,
    deviceId = deviceId,
    identifiers = identifiers,
    properties = properties
)

internal data class HackleExperimentDto(
    @SerializedName("key")
    val key: Long,
    @SerializedName("version")
    val version: Int
)

internal fun HackleExperiment.toDto() = HackleExperimentDto(
    key = key,
    version = version
)

internal data class DecisionDto(
    @SerializedName("experiment")
    val experiment: HackleExperimentDto?,
    @SerializedName("variation")
    val variation: String,
    @SerializedName("reason")
    val reason: String,
    @SerializedName("config")
    val config: Map<String, Any>
)

internal fun Decision.toDto() = DecisionDto(
    experiment = experiment?.toDto(),
    variation = variation.name,
    reason = reason.name,
    config = config.parameters
)

internal data class FeatureFlagDecisionDto(
    @SerializedName("featureFlag")
    val featureFlag: HackleExperimentDto?,
    @SerializedName("isOn")
    val isOn: Boolean,
    @SerializedName("reason")
    val reason: String,
    @SerializedName("config")
    val config: Map<String, Any>
)

internal fun FeatureFlagDecision.toDto() = FeatureFlagDecisionDto(
    featureFlag = featureFlag?.toDto(),
    isOn = isOn,
    reason = reason.name,
    config = config.parameters
)