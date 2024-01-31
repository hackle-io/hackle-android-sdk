package io.hackle.android.internal.workspace

import io.hackle.android.internal.model.Sdk
import io.hackle.android.support.assertThrows
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import okhttp3.Call
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.ResponseBody
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import java.nio.file.Files
import java.nio.file.Paths


class HttpWorkspaceFetcherTest {

    private lateinit var httpClient: OkHttpClient
    private lateinit var sut: HttpWorkspaceFetcher

    @Before
    fun before() {
        httpClient = mockk()
        sut = HttpWorkspaceFetcher(Sdk("key", "name", "version"), "http://localhost", httpClient)
    }

    @Test
    fun `when exception on http call then throw exception`() {
        // given
        every { httpClient.newCall(any()) } returns mockk {
            every { execute() } throws IllegalArgumentException("http fail")
        }

        // when
        val exception = assertThrows<IllegalArgumentException> {
            sut.fetchIfModified()
        }

        // then
        expectThat(exception.message).isEqualTo("http fail")
    }

    @Test
    fun `when workspace config not modified then return null`() {
        // given
        every { httpClient.newCall(any()) } returns mockCall(304, "")

        // when
        val actual = sut.fetchIfModified()

        // then
        expectThat(actual).isNull()
    }

    @Test
    fun `when http call is not successful then throw exception`() {
        // given
        every { httpClient.newCall(any()) } returns mockCall(500)

        // when
        val exception = assertThrows<IllegalStateException> {
            sut.fetchIfModified()
        }

        // then
        expectThat(exception.message).isEqualTo("Http status code: 500")
    }

    @Test
    fun `when response body is bull then throw exception`() {
        // given
        every { httpClient.newCall(any()) } returns mockCall(200, null)

        // when
        val exception = assertThrows<IllegalStateException> {
            sut.fetchIfModified()
        }

        // then
        expectThat(exception.message).isEqualTo("Response body is null")
    }

    @Test
    fun `success`() {
        // given
        val body = String(Files.readAllBytes(Paths.get("src/test/resources/workspace_response.json")))
        every { httpClient.newCall(any()) } returns mockCall(200, body)

        // when
        val actual = sut.fetchIfModified()

        // then
        expectThat(actual)
            .isNotNull()
            .get { config.workspace.id }
            .isEqualTo(7356)
    }

    @Test
    fun `last modified`() {
        val body = String(Files.readAllBytes(Paths.get("src/test/resources/workspace_response.json")))
        every { httpClient.newCall(any()) }.returnsMany(
            mockCall(200, body, mapOf("Last-Modified" to "LAST_MODIFIED_HEADER_VALUE")),
            mockCall(304, "")
        )

        expectThat(sut.fetchIfModified("LAST_MODIFIED_HEADER_VALUE")).isNotNull()

        verify(exactly = 1) {
            httpClient.newCall(withArg {
                expectThat(it) {
                    get { header("If-Modified-Since") } isEqualTo "LAST_MODIFIED_HEADER_VALUE"
                }
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
            .headers(Headers.of(headers))
            .networkResponse(
                Response.Builder()
                    .request(mockk())
                    .protocol(mockk())
                    .code(statusCode)
                    .message(statusCode.toString())
                    .build()
            )
            .body(body?.let { ResponseBody.create(null, it) })
            .message(statusCode.toString())
            .build()
        return mockk {
            every { execute() } returns response
        }
    }
}