package io.hackle.android.internal.user

import android.util.Base64
import io.hackle.android.support.assertThrows
import io.hackle.sdk.common.User
import io.hackle.sdk.core.model.Target
import io.hackle.sdk.core.model.TargetEvent
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

class UserTargetEventFetcherTest {
    private lateinit var httpClient: OkHttpClient
    private lateinit var sut: UserTargetEventFetcher

    @Before
    fun before() {
        httpClient = mockk()
        sut = UserTargetEventFetcher("http://localhost", httpClient)

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
            sut.fetch(User.builder().build())
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
            sut.fetch(User.builder().build())
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
            sut.fetch(User.builder().build())
        }

        // then
        expectThat(exception.message).isEqualTo("Response body is null")
    }

    @Test
    fun `success`() {
        // given
        val body =
            String(Files.readAllBytes(Paths.get("src/test/resources/workspace_target.json")))
        every { httpClient.newCall(any()) }.response(200, body)

        // when
        val actual = sut.fetch(User.builder().id("42").build())

        val events = UserTargetEvents.builder()
            .put(
                TargetEvent(
                "purchase",
                listOf(
                    TargetEvent.Stat(1737361789000, 10),
                    TargetEvent.Stat(1737361790000, 20),
                    TargetEvent.Stat(1737361793000, 30)
                ),
                TargetEvent.Property(
                    "product_name",
                    Target.Key.Type.EVENT_PROPERTY,
                    "shampoo"
                ))
            )
            .put(TargetEvent(
                "cart",
                listOf(
                    TargetEvent.Stat(1737361789000, 10),
                    TargetEvent.Stat(1737361790000, 20),
                    TargetEvent.Stat(1737361793000, 30)
                ),
                null))
            .build()

        expectThat(actual).isEqualTo(events)
    }

    @Test
    fun `request header`() {
        // given
        val body =
            String(Files.readAllBytes(Paths.get("src/test/resources/workspace_target.json")))
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

internal fun MockKStubScope<Call, Call>.response(statusCode: Int, body: String? = null) {

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