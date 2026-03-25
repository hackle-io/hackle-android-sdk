package io.hackle.android.internal.invocator.invocation

import com.google.gson.GsonBuilder
import io.hackle.android.support.assertThrows
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue

class InvocationRequestTest {

    private val gson = GsonBuilder().serializeNulls().create()

    private fun json(
        command: String? = "getSessionId",
        parameters: Map<String, Any?>? = null,
        browserProperties: Map<String, Any>? = null,
    ): String {
        val hackle = mutableMapOf<String, Any?>()
        if (command != null) hackle["command"] = command
        if (parameters != null) hackle["parameters"] = parameters
        if (browserProperties != null) hackle["browserProperties"] = browserProperties
        return gson.toJson(mapOf("_hackle" to hackle))
    }

    // parse

    @Test
    fun `parse - command, parameters, browserProperties를 파싱한다`() {
        val input = json(
            command = "track",
            parameters = mapOf("event" to "purchase"),
            browserProperties = mapOf("url" to "https://hackle.io"),
        )

        val request = InvocationRequest.parse(input)

        expectThat(request) {
            get { command }.isEqualTo(InvocationCommand.TRACK)
            get { parameters["event"] }.isEqualTo("purchase")
            get { browserProperties["url"] }.isEqualTo("https://hackle.io")
        }
    }

    @Test
    fun `parse - parameters가 없으면 빈 맵으로 설정한다`() {
        val request = InvocationRequest.parse(json(command = "getSessionId"))

        expectThat(request.parameters).isEqualTo(emptyMap())
    }

    @Test
    fun `parse - browserProperties가 없으면 빈 맵으로 설정한다`() {
        val request = InvocationRequest.parse(json(command = "getSessionId"))

        expectThat(request.browserProperties).isEqualTo(emptyMap())
    }

    @Test
    fun `parse - 유효하지 않은 JSON이면 예외를 던진다`() {
        assertThrows<IllegalArgumentException> {
            InvocationRequest.parse("not a json")
        }
    }

    @Test
    fun `parse - _hackle 키가 없으면 예외를 던진다`() {
        assertThrows<IllegalArgumentException> {
            InvocationRequest.parse(gson.toJson(mapOf("other" to "value")))
        }
    }

    @Test
    fun `parse - command가 없으면 예외를 던진다`() {
        val input = gson.toJson(mapOf("_hackle" to mapOf("parameters" to emptyMap<String, Any>())))
        assertThrows<IllegalArgumentException> {
            InvocationRequest.parse(input)
        }
    }

    @Test
    fun `parse - 지원하지 않는 command이면 예외를 던진다`() {
        assertThrows<IllegalArgumentException> {
            InvocationRequest.parse(json(command = "unknownCommand"))
        }
    }

    // isInvocableString

    @Test
    fun `isInvocableString - 유효한 invocation 문자열이면 true를 반환한다`() {
        expectThat(InvocationRequest.isInvocableString(json(command = "getSessionId"))).isTrue()
    }

    @Test
    fun `isInvocableString - JSON이 아니면 false를 반환한다`() {
        expectThat(InvocationRequest.isInvocableString("not json")).isFalse()
    }

    @Test
    fun `isInvocableString - _hackle 키가 없으면 false를 반환한다`() {
        val input = gson.toJson(mapOf("other" to "value"))
        expectThat(InvocationRequest.isInvocableString(input)).isFalse()
    }

    @Test
    fun `isInvocableString - command가 없으면 false를 반환한다`() {
        val input = gson.toJson(mapOf("_hackle" to mapOf("parameters" to emptyMap<String, Any>())))
        expectThat(InvocationRequest.isInvocableString(input)).isFalse()
    }

    @Test
    fun `isInvocableString - command가 빈 문자열이면 false를 반환한다`() {
        expectThat(InvocationRequest.isInvocableString(json(command = ""))).isFalse()
    }

    @Test
    fun `isInvocableString - command가 공백 문자열이면 false를 반환한다`() {
        expectThat(InvocationRequest.isInvocableString(json(command = "   "))).isFalse()
    }
}
