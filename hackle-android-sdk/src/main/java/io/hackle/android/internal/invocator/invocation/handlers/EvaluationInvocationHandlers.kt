package io.hackle.android.internal.invocator.invocation.handlers

import io.hackle.android.internal.HackleAppCore
import io.hackle.android.internal.context.HackleAppContext
import io.hackle.android.internal.invocator.checkParameterNotNull
import io.hackle.android.internal.invocator.defaultBooleanValue
import io.hackle.android.internal.invocator.defaultNumberValue
import io.hackle.android.internal.invocator.defaultStringValue
import io.hackle.android.internal.invocator.defaultVariation
import io.hackle.android.internal.invocator.experimentKey
import io.hackle.android.internal.invocator.featureKey
import io.hackle.android.internal.invocator.invocation.InvocationHandler
import io.hackle.android.internal.invocator.invocation.InvocationRequest
import io.hackle.android.internal.invocator.invocation.InvocationResponse
import io.hackle.android.internal.invocator.key
import io.hackle.android.internal.invocator.model.DecisionDto
import io.hackle.android.internal.invocator.model.FeatureFlagDecisionDto
import io.hackle.android.internal.invocator.model.toDto
import io.hackle.android.internal.invocator.user
import io.hackle.android.internal.invocator.userWithUserId
import io.hackle.android.internal.invocator.valueType
import io.hackle.sdk.common.Variation
import io.hackle.sdk.common.decision.Decision
import io.hackle.sdk.common.decision.FeatureFlagDecision
import io.hackle.sdk.core.model.ValueType

// AB_TEST

internal abstract class AbTestInvocationHandler<R>(private val core: HackleAppCore) : InvocationHandler<R> {
    override fun invoke(request: InvocationRequest): InvocationResponse<R> {
        val p = request.parameters
        val experimentKey = checkParameterNotNull(p.experimentKey(), "experimentKey")
        val defaultVariation = Variation.fromOrControl(p.defaultVariation())
        val context = HackleAppContext.create(request.browserProperties)
        val decision = core.variationDetail(experimentKey, p.user(), defaultVariation, context)
        return InvocationResponse.success(transform(decision))
    }

    protected abstract fun transform(decision: Decision): R
}

internal class VariationInvocationHandler(core: HackleAppCore) : AbTestInvocationHandler<String>(core) {
    override fun transform(decision: Decision): String = decision.variation.name
}

internal class VariationDetailInvocationHandler(core: HackleAppCore) :
    AbTestInvocationHandler<DecisionDto>(core) {
    override fun transform(decision: Decision): DecisionDto = decision.toDto()
}

// FEATURE_FLAG

internal abstract class FeatureFlagInvocationHandler<R>(private val core: HackleAppCore) : InvocationHandler<R> {
    override fun invoke(request: InvocationRequest): InvocationResponse<R> {
        val p = request.parameters
        val featureKey = checkParameterNotNull(p.featureKey(), "featureKey")
        val context = HackleAppContext.create(request.browserProperties)
        val decision = core.featureFlagDetail(featureKey, p.user(), context)
        return InvocationResponse.success(transform(decision))
    }

    protected abstract fun transform(decision: FeatureFlagDecision): R
}

internal class IsFeatureOnInvocationHandler(core: HackleAppCore) : FeatureFlagInvocationHandler<Boolean>(core) {
    override fun transform(decision: FeatureFlagDecision): Boolean = decision.isOn
}

internal class FeatureFlagDetailInvocationHandler(core: HackleAppCore) :
    FeatureFlagInvocationHandler<FeatureFlagDecisionDto>(core) {
    override fun transform(decision: FeatureFlagDecision): FeatureFlagDecisionDto = decision.toDto()
}

// REMOTE_CONFIG

internal class RemoteConfigInvocationHandler(private val core: HackleAppCore) : InvocationHandler<Any> {
    override fun invoke(request: InvocationRequest): InvocationResponse<Any> {
        val p = request.parameters
        val user = p.userWithUserId()
        val key = checkParameterNotNull(p.key(), "key")
        val context = HackleAppContext.create(request.browserProperties)
        val data: Any = when (checkParameterNotNull(p.valueType(), "valueType")) {
            "string" -> {
                val defaultValue = checkParameterNotNull(p.defaultStringValue(), "defaultValue")
                core.remoteConfig(key, ValueType.STRING, defaultValue, user, context).value
            }

            "number" -> {
                val defaultValue = checkParameterNotNull(p.defaultNumberValue(), "defaultValue")
                core.remoteConfig(key, ValueType.NUMBER, defaultValue, user, context).value.toDouble()
            }

            "boolean" -> {
                val defaultValue = checkParameterNotNull(p.defaultBooleanValue(), "defaultValue")
                core.remoteConfig(key, ValueType.BOOLEAN, defaultValue, user, context).value
            }

            else -> {
                throw IllegalArgumentException("Valid parameter must be provided.")
            }
        }
        return InvocationResponse.success(data)
    }
}
