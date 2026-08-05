package io.hackle.android.internal.workspace.config

import io.hackle.android.internal.model.Sdk
import io.hackle.android.support.assertThrows
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import okhttp3.Call
import okhttp3.Headers.Companion.toHeaders
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor

class HttpWorkspaceConfigFetcherTest {

    private lateinit var httpClient: OkHttpClient
    private lateinit var sut: HttpWorkspaceConfigFetcher

    @Before
    fun before() {
        httpClient = mockk()
        sut = HttpWorkspaceConfigFetcher(
            sdk = Sdk("key", "name", "version"),
            sdkUri = "http://localhost",
            executor = Executor { it.run() },
            httpClient = httpClient
        )
    }

    @Test
    fun `http 호출에 실패하면 future가 예외로 완료된다`() {
        // given
        every { httpClient.newCall(any()) } returns mockk {
            every { execute() } throws IllegalArgumentException("http fail")
        }

        // when
        val exception = assertThrows<ExecutionException> {
            sut.fetchIfModified(null).get()
        }

        // then
        expectThat(exception.cause).isA<IllegalArgumentException>()
            .get { message } isEqualTo "http fail"
    }

    @Test
    fun `workspace config가 변경되지 않았으면 null을 리턴한다`() {
        // given
        every { httpClient.newCall(any()) } returns mockCall(304, null)

        // when
        val actual = sut.fetchIfModified(null).get()

        // then
        expectThat(actual).isNull()
    }

    @Test
    fun `http 응답이 실패 상태면 예외로 완료된다`() {
        // given
        every { httpClient.newCall(any()) } returns mockCall(500)

        // when
        val exception = assertThrows<ExecutionException> {
            sut.fetchIfModified(null).get()
        }

        // then
        expectThat(exception.cause).isA<IllegalStateException>()
            .get { message } isEqualTo "Http status code: 500"
    }

    @Test
    fun `응답 body가 없으면 예외로 완료된다`() {
        // given (execute() 응답의 body는 항상 존재하지만, 방어 로직을 고정하기 위해 body 없는 응답을 흉내낸다)
        val response = mockk<Response>(relaxed = true) {
            every { networkResponse } returns null
            every { isSuccessful } returns true
            every { body } returns null
        }
        every { httpClient.newCall(any()) } returns mockk {
            every { execute() } returns response
        }

        // when
        val exception = assertThrows<ExecutionException> {
            sut.fetchIfModified(null).get()
        }

        // then
        expectThat(exception.cause).isA<IllegalStateException>()
            .get { message } isEqualTo "Response body is null"
    }

    @Test
    fun `성공하면 응답을 파싱해 WorkspaceConfigContext를 만든다`() {
        // given
        val body = String(Files.readAllBytes(Paths.get("src/test/resources/workspace_response.json")))
        every { httpClient.newCall(any()) } returns mockCall(
            statusCode = 200,
            body = body,
            headers = mapOf("Last-Modified" to "LAST_MODIFIED_HEADER_VALUE")
        )

        // when
        val actual = sut.fetchIfModified(null).get()

        // then
        expectThat(actual).isNotNull().and {
            get { modifiedAt } isEqualTo "LAST_MODIFIED_HEADER_VALUE"
            get { workspace.metadata.id } isEqualTo 7356L
            get { workspace.metadata.environmentId } isEqualTo 112712L
            get { dto.workspace.id } isEqualTo 7356L
        }
    }

    @Test
    fun `lastModified를 If-Modified-Since 헤더로 전달한다`() {
        // given
        every { httpClient.newCall(any()) } returns mockCall(304, "")

        // when
        sut.fetchIfModified("LAST_MODIFIED_HEADER_VALUE").get()

        // then
        verify(exactly = 1) {
            httpClient.newCall(withArg {
                expectThat(it.header("If-Modified-Since")) isEqualTo "LAST_MODIFIED_HEADER_VALUE"
            })
        }
    }

    private fun mockCall(
        statusCode: Int,
        body: String? = null,
        headers: Map<String, String> = emptyMap()
    ): Call {
        val response = Response.Builder()
            .request(mockk())
            .protocol(mockk())
            .code(statusCode)
            .headers(headers.toHeaders())
            .networkResponse(
                Response.Builder()
                    .request(mockk())
                    .protocol(mockk())
                    .code(statusCode)
                    .message(statusCode.toString())
                    .build()
            )
            .body((body ?: "").toResponseBody(null))
            .message(statusCode.toString())
            .build()
        return mockk {
            every { execute() } returns response
        }
    }
}
