package io.hackle.android.internal.invocator.invocation.handlers

import io.hackle.android.internal.HackleAppCore
import io.hackle.android.internal.context.HackleAppContext
import io.hackle.android.internal.invocator.checkParameterNotNull
import io.hackle.android.internal.invocator.deviceId
import io.hackle.android.internal.invocator.hackleSubscriptionOperationDto
import io.hackle.android.internal.invocator.invocation.InvocationHandler
import io.hackle.android.internal.invocator.invocation.InvocationRequest
import io.hackle.android.internal.invocator.invocation.InvocationResponse
import io.hackle.android.internal.invocator.key
import io.hackle.android.internal.invocator.model.UserDto
import io.hackle.android.internal.invocator.model.from
import io.hackle.android.internal.invocator.model.toDto
import io.hackle.android.internal.invocator.phoneNumber
import io.hackle.android.internal.invocator.propertyOperationDto
import io.hackle.android.internal.invocator.userAsMap
import io.hackle.android.internal.invocator.userId
import io.hackle.android.internal.invocator.value
import io.hackle.sdk.common.PropertyOperations
import io.hackle.sdk.common.User
import io.hackle.sdk.common.subscription.HackleSubscriptionOperations

// Session

internal class GetSessionIdInvocationHandler(private val core: HackleAppCore) : InvocationHandler<String> {
    override fun invoke(request: InvocationRequest): InvocationResponse<String> {
        return InvocationResponse.success(core.sessionId)
    }
}

// User

internal class GetUserInvocationHandler(private val core: HackleAppCore) : InvocationHandler<UserDto> {
    override fun invoke(request: InvocationRequest): InvocationResponse<UserDto> {
        return InvocationResponse.success(core.user.toDto())
    }
}

internal class SetUserInvocationHandler(private val core: HackleAppCore) : InvocationHandler<Unit> {
    override fun invoke(request: InvocationRequest): InvocationResponse<Unit> {
        val data = checkParameterNotNull(request.parameters.userAsMap(), "user")
        val dto = UserDto.from(data)
        val user = User.from(dto)
        core.setUser(user, null)
        return InvocationResponse.success()
    }
}

internal class ResetUserInvocationHandler(private val core: HackleAppCore) : InvocationHandler<Unit> {
    override fun invoke(request: InvocationRequest): InvocationResponse<Unit> {
        val context = HackleAppContext.create(request.browserProperties)
        core.resetUser(context, null)
        return InvocationResponse.success()
    }
}

// UserIdentifiers

internal class SetUserIdInvocationHandler(private val core: HackleAppCore) : InvocationHandler<Unit> {
    override fun invoke(request: InvocationRequest): InvocationResponse<Unit> {
        check(request.parameters.containsKey("userId"))
        val userId = request.parameters.userId()
        core.setUserId(userId, null)
        return InvocationResponse.success()
    }
}

internal class SetDeviceIdInvocationHandler(private val core: HackleAppCore) : InvocationHandler<Unit> {
    override fun invoke(request: InvocationRequest): InvocationResponse<Unit> {
        val deviceId = checkParameterNotNull(request.parameters.deviceId(), "deviceId")
        core.setDeviceId(deviceId, null)
        return InvocationResponse.success()
    }
}

// UserProperties

internal class SetUserPropertyInvocationHandler(private val core: HackleAppCore) : InvocationHandler<Unit> {
    override fun invoke(request: InvocationRequest): InvocationResponse<Unit> {
        val key = checkParameterNotNull(request.parameters.key(), "key")
        val value = request.parameters.value()
        val operations = PropertyOperations.builder()
            .set(key, value)
            .build()
        val context = HackleAppContext.create(request.browserProperties)
        core.updateUserProperties(operations, context, null)
        return InvocationResponse.success()
    }
}

internal class UpdateUserPropertiesInvocationHandler(private val core: HackleAppCore) : InvocationHandler<Unit> {
    override fun invoke(request: InvocationRequest): InvocationResponse<Unit> {
        val dto = checkParameterNotNull(request.parameters.propertyOperationDto(), "operations")
        val operations = PropertyOperations.from(dto)
        val context = HackleAppContext.create(request.browserProperties)
        core.updateUserProperties(operations, context, null)
        return InvocationResponse.success()
    }
}

// PhoneNumber

internal class SetPhoneNumberInvocationHandler(private val core: HackleAppCore) : InvocationHandler<Unit> {
    override fun invoke(request: InvocationRequest): InvocationResponse<Unit> {
        val phoneNumber = checkParameterNotNull(request.parameters.phoneNumber(), "phoneNumber")
        val context = HackleAppContext.create(request.browserProperties)
        core.setPhoneNumber(phoneNumber, context, null)
        return InvocationResponse.success()
    }
}

internal class UnsetPhoneNumberInvocationHandler(private val core: HackleAppCore) : InvocationHandler<Unit> {
    override fun invoke(request: InvocationRequest): InvocationResponse<Unit> {
        val context = HackleAppContext.create(request.browserProperties)
        core.unsetPhoneNumber(context, null)
        return InvocationResponse.success()
    }
}

// Subscriptions

internal abstract class SubscriptionInvocationHandler(private val core: HackleAppCore) : InvocationHandler<Unit> {
    override fun invoke(request: InvocationRequest): InvocationResponse<Unit> {
        val dto = checkParameterNotNull(request.parameters.hackleSubscriptionOperationDto(), "operations")
        val operations = HackleSubscriptionOperations.from(dto)
        val context = HackleAppContext.create(request.browserProperties)
        update(core, operations, context)
        return InvocationResponse.success()
    }

    protected abstract fun update(
        core: HackleAppCore,
        operations: HackleSubscriptionOperations,
        context: HackleAppContext,
    )
}

internal class UpdatePushSubscriptionsInvocationHandler(core: HackleAppCore) :
    SubscriptionInvocationHandler(core) {
    override fun update(core: HackleAppCore, operations: HackleSubscriptionOperations, context: HackleAppContext) {
        core.updatePushSubscriptions(operations, context)
    }
}

internal class UpdateSmsSubscriptionsInvocationHandler(core: HackleAppCore) :
    SubscriptionInvocationHandler(core) {
    override fun update(core: HackleAppCore, operations: HackleSubscriptionOperations, context: HackleAppContext) {
        core.updateSmsSubscriptions(operations, context)
    }
}

internal class UpdateKakaoSubscriptionsInvocationHandler(core: HackleAppCore) :
    SubscriptionInvocationHandler(core) {
    override fun update(core: HackleAppCore, operations: HackleSubscriptionOperations, context: HackleAppContext) {
        core.updateKakaoSubscriptions(operations, context)
    }
}
