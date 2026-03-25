package io.hackle.android.internal.invocator.invocation

import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class InvocationResponseTest {

    @Test
    fun `success() - data 없는 성공 응답 JSON`() {
        val json = InvocationResponse.success<Any>().toJsonString()

        expectThat(json).isEqualTo("""{"success":true,"message":"OK"}""")
    }

    @Test
    fun `success(data) - String data 성공 응답 JSON`() {
        val json = InvocationResponse.success("hello").toJsonString()

        expectThat(json).isEqualTo("""{"success":true,"message":"OK","data":"hello"}""")
    }

    @Test
    fun `success(data) - Number data 성공 응답 JSON`() {
        val json = InvocationResponse.success(42).toJsonString()

        expectThat(json).isEqualTo("""{"success":true,"message":"OK","data":42}""")
    }

    @Test
    fun `success(data) - Boolean data 성공 응답 JSON`() {
        val json = InvocationResponse.success(true).toJsonString()

        expectThat(json).isEqualTo("""{"success":true,"message":"OK","data":true}""")
    }

    @Test
    fun `success(data) - Map data 성공 응답 JSON`() {
        val json = InvocationResponse.success(mapOf("key" to "value")).toJsonString()

        expectThat(json).isEqualTo("""{"success":true,"message":"OK","data":{"key":"value"}}""")
    }

    @Test
    fun `failed - 예외 메시지를 포함한 실패 응답 JSON`() {
        val json = InvocationResponse.failed(IllegalArgumentException("test error")).toJsonString()

        expectThat(json).isEqualTo("""{"success":false,"message":"test error"}""")
    }

    @Test
    fun `failed - 예외 메시지가 null이면 FAIL 메시지 JSON`() {
        val json = InvocationResponse.failed(RuntimeException()).toJsonString()

        expectThat(json).isEqualTo("""{"success":false,"message":"FAIL"}""")
    }
}
