package io.hackle.android.internal.invocator.invocation

import io.hackle.android.internal.invocator.HackleBrowserProperties
import io.hackle.android.internal.invocator.InvocationParameters
import io.hackle.android.internal.invocator.model.InvocationRequestDto
import io.hackle.android.internal.invocator.model.InvocationRequestDto.Companion.KEY_COMMAND
import io.hackle.android.internal.invocator.model.InvocationRequestDto.Companion.KEY_HACKLE
import io.hackle.android.internal.utils.json.parseJson
import io.hackle.android.internal.utils.json.toJson

internal class InvocationRequest private constructor(
    val command: InvocationCommand,
    val parameters: InvocationParameters,
    val browserProperties: HackleBrowserProperties,
) {

    override fun toString(): String {
        return "InvocationRequest(command=$command, parameters=$parameters, browserProperties=$browserProperties)"
    }

    companion object {
        fun parse(string: String): InvocationRequest {
            val dto = try {
                string.parseJson<InvocationRequestDto>()
            } catch (e: Exception) {
                throw IllegalArgumentException("Invalid invocation format")
            }

            val invocation = requireNotNull(dto.hackle) { "Invalid invocation format (missing: $KEY_HACKLE)" }
            val command = requireNotNull(invocation.command) { "Invalid invocation format (missing: $KEY_COMMAND)" }

            return InvocationRequest(
                command = InvocationCommand.from(command),
                parameters = invocation.parameters ?: hashMapOf(),
                browserProperties = invocation.browserProperties ?: hashMapOf()
            )
        }

        fun isInvocableString(string: String): Boolean {
            return try {
                val dto = string.parseJson<InvocationRequestDto>()
                val hackle = dto.hackle ?: return false
                val command = hackle.command ?: return false
                command.isNotBlank()
            } catch (_: Exception) {
                false
            }
        }
    }
}

internal inline fun <reified T> InvocationRequest.parameters(): T {
    return parameters.toJson().parseJson<T>()
}
