package io.hackle.android.internal.invocator.invocation.handlers

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.ToNumberPolicy
import io.hackle.android.internal.invocator.invocation.InvocationCommand
import io.hackle.android.internal.invocator.invocation.InvocationRequest
import io.hackle.android.internal.invocator.model.InvocationRequestDto

internal abstract class AbstractInvocationHandlerTest {

    protected val gson: Gson = GsonBuilder()
        .serializeNulls()
        .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
        .create()

    protected fun request(command: InvocationCommand, parameters: Map<String, Any?>? = null): InvocationRequest {
        val dto = InvocationRequestDto(
            hackle = InvocationRequestDto.HackleData(
                command = command.command,
                parameters = parameters,
                browserProperties = mapOf("url" to "https://hackle.io")
            )
        )
        return InvocationRequest.parse(gson.toJson(dto))
    }
}