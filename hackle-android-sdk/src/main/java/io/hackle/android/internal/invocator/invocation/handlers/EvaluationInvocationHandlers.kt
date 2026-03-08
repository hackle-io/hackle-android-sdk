package io.hackle.android.internal.invocator.invocation.handlers

import io.hackle.android.internal.HackleAppCore
import io.hackle.android.internal.context.HackleAppContext
import io.hackle.android.internal.invocator.*
import io.hackle.android.internal.invocator.invocation.InvocationHandler
import io.hackle.android.internal.invocator.invocation.InvocationRequest
import io.hackle.android.internal.invocator.invocation.InvocationResponse
import io.hackle.android.internal.invocator.model.DecisionDto
import io.hackle.android.internal.invocator.model.FeatureFlagDecisionDto
import io.hackle.android.internal.invocator.model.toDto
import io.hackle.sdk.common.Variation
import io.hackle.sdk.common.decision.Decision
import io.hackle.sdk.common.decision.FeatureFlagDecision
import io.hackle.sdk.core.model.ValueType

// AB_TEST

internal abstract class AbTestInvocationHandler<R>(private val core: HackleAppCore) : InvocationHandler<R> {
    override fun invoke(request: InvocationRequest): InvocationResponse<R> {
        val p = request.parameters
        val experimentKey = checkNotNull(p.experimentKey())
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
        val featureKey = checkNotNull(p.featureKey())
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
        val key = checkNotNull(p.key())
        val context = HackleAppContext.create(request.browserProperties)
        val data: Any = when (checkNotNull(p.valueType())) {
            "string" -> {
                val defaultValue = checkNotNull(p.defaultStringValue())
                core.remoteConfig(key, ValueType.STRING, defaultValue, user, context).value
            }

            "number" -> {
                val defaultValue = checkNotNull(p.defaultNumberValue())
                core.remoteConfig(key, ValueType.NUMBER, defaultValue, user, context).value.toDouble()
            }

            "boolean" -> {
                val defaultValue = checkNotNull(p.defaultBooleanValue())
                core.remoteConfig(key, ValueType.BOOLEAN, defaultValue, user, context).value
            }

            else -> {
                throw IllegalArgumentException("Valid parameter must be provided.")
            }
        }
        return InvocationResponse.success(data)
    }
}
