package io.hackle.android.internal.inappmessage.evaluation

import io.hackle.sdk.core.evaluation.EvaluateProcessor
import io.hackle.sdk.core.evaluation.service.inappmessage.InAppMessageEvaluateScope
import io.hackle.sdk.core.evaluation.service.inappmessage.eligibility.InAppMessageEligibilityEvaluateResponse
import io.hackle.sdk.core.evaluation.service.inappmessage.eligibility.mode.local.InAppMessageEligibilityLocalEvaluateRequest
import io.hackle.sdk.core.evaluation.service.inappmessage.eligibility.mode.remote.InAppMessageEligibilityRemoteEvaluateRequest
import io.hackle.sdk.core.evaluation.service.inappmessage.layout.InAppMessageLayoutEvaluateResponse
import io.hackle.sdk.core.evaluation.service.inappmessage.layout.mode.local.InAppMessageLayoutLocalEvaluateRequest
import io.hackle.sdk.core.evaluation.service.inappmessage.layout.mode.remote.InAppMessageLayoutRemoteEvaluateRequest
import io.hackle.sdk.core.model.PlatformType
import io.hackle.sdk.core.user.HackleUser
import io.hackle.sdk.core.workspace.config.WorkspaceConfig
import io.hackle.sdk.core.workspace.config.entity.InAppMessageConfig
import io.hackle.sdk.core.workspace.evaluation.WorkspaceEvaluation
import io.hackle.sdk.core.workspace.evaluation.entity.InAppMessageEligibilityRemoteEvaluateResult

internal fun EvaluateProcessor.eligibility(
    workspace: WorkspaceConfig,
    inAppMessage: InAppMessageConfig,
    user: HackleUser,
    scope: InAppMessageEvaluateScope,
    timestamp: Long,
): InAppMessageEligibilityEvaluateResponse {
    val request = InAppMessageEligibilityLocalEvaluateRequest.of(
        workspace = workspace,
        entity = inAppMessage,
        user = user,
        scope = scope,
        platformType = PlatformType.ANDROID,
        timestamp = timestamp,
    )
    return inAppMessage(request)
}

internal fun EvaluateProcessor.layout(
    workspace: WorkspaceConfig,
    inAppMessage: InAppMessageConfig,
    user: HackleUser,
    scope: InAppMessageEvaluateScope,
): InAppMessageLayoutEvaluateResponse {
    val request = InAppMessageLayoutLocalEvaluateRequest.of(
        workspace = workspace,
        entity = inAppMessage,
        user = user,
        scope = scope
    )
    return inAppMessage(request)
}

internal fun EvaluateProcessor.eligibility(
    workspace: WorkspaceEvaluation,
    inAppMessage: InAppMessageEligibilityRemoteEvaluateResult,
    user: HackleUser,
    scope: InAppMessageEvaluateScope,
    timestamp: Long,
    record: Boolean = true,
): InAppMessageEligibilityEvaluateResponse {
    val request = InAppMessageEligibilityRemoteEvaluateRequest.of(
        workspace = workspace,
        entity = inAppMessage,
        user = user,
        scope = scope,
        timestamp = timestamp,
        platformType = PlatformType.ANDROID,
        record = record
    )
    return inAppMessage(request)
}

internal fun EvaluateProcessor.layout(
    workspace: WorkspaceEvaluation,
    inAppMessage: InAppMessageEligibilityRemoteEvaluateResult,
    user: HackleUser,
    scope: InAppMessageEvaluateScope,
): InAppMessageLayoutEvaluateResponse {
    val request = InAppMessageLayoutRemoteEvaluateRequest.of(
        workspace = workspace,
        entity = inAppMessage.layout,
        user = user,
        scope = scope
    )
    return inAppMessage(request)
}
