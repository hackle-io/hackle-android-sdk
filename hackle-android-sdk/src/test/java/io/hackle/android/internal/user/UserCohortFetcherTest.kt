package io.hackle.android.internal.user

import android.util.Base64
import io.hackle.android.support.assertThrows
import io.hackle.sdk.common.User
import io.hackle.sdk.core.model.Cohort
import io.hackle.sdk.core.model.Identifier
import io.mockk.MockKStubScope
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.ResponseBody
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import java.nio.file.Files
import java.nio.file.Paths

class UserCohortFetcherTest {

    private lateinit var httpClient: OkHttpClient
    private lateinit var sut: UserCohortFetcher

    @Before
    fun before() {
        httpClient = mockk()
        sut = UserCohortFetcher("http://localhost", httpClient)

        mockkStatic(Base64::class)
        every { Base64.encodeToString(any(), any()) } answers {
            java.util.Base64.getUrlEncoder().encodeToString(firstArg())
        }
    }

    @Test
    fun `when exception on fetch then throw exception`() {
        // given
        every { httpClient.newCall(any()) } returns mockk {
            every { execute() } throws IllegalArgumentException("http fail")
        }

        // when
        val exception = assertThrows<IllegalArgumentException> {
            sut.fetch(User.builder().id("42").build())
        }

        // then
        expectThat(exception.message).isEqualTo("http fail")
    }

    @Test
    fun `when failed to fetch then throw exception`() {
        // given
        every { httpClient.newCall(any()) }.response(500)

        // when
        val exception = assertThrows<IllegalStateException> {
            sut.fetch(User.builder().id("42").build())
        }

        // then
        expectThat(exception.message).isEqualTo("Http status code: 500")
    }

    @Test
    fun `when body is null then throw exception`() {
        // given
        every { httpClient.newCall(any()) }.response(200, null)

        // when
        val exception = assertThrows<IllegalStateException> {
            sut.fetch(User.builder().id("42").build())
        }

        // then
        expectThat(exception.message).isEqualTo("Response body is null")
    }

    @Test
    fun `success`() {
        // given
        val body =
            String(Files.readAllBytes(Paths.get("src/test/resources/workspace_cohorts.json")))
        every { httpClient.newCall(any()) }.response(200, body)

        // when
        val actual = sut.fetch(User.builder().id("42").build())

        // then
        val cohorts = UserCohorts.builder()
            .put(UserCohort(Identifier("\$id", "id"), listOf(Cohort(1), Cohort(2))))
            .put(UserCohort(Identifier("\$userId", "user_id"), listOf()))
            .build()

        expectThat(actual).isEqualTo(cohorts)
    }

    @Test
    fun `request header`() {
        // given
        val body =
            String(Files.readAllBytes(Paths.get("src/test/resources/workspace_cohorts.json")))
        every { httpClient.newCall(any()) }.response(200, body)

        // when
        sut.fetch(User.builder().id("42").build())

        // then
        verify {
            httpClient.newCall(withArg {
                expectThat(it) {
                    get { it.header("X-HACKLE-USER") }.isNotNull()
                }
            })
        }
    }
}

private fun MockKStubScope<Call, Call>.response(statusCode: Int, body: String? = null) {

    val response = Response.Builder()
        .request(mockk())
        .protocol(mockk())
        .code(statusCode)
        .message(statusCode.toString())
        .body(body?.let { ResponseBody.create(null, it) })
        .build()
    val call = mockk<Call> {
        every { execute() } returns response
    }
    returns(call)
}