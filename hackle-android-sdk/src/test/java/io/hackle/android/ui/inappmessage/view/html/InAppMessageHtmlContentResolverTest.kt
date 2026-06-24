package io.hackle.android.ui.inappmessage.view.html

import io.hackle.sdk.core.model.InAppMessage
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Timeout
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.io.InterruptedIOException
import java.util.concurrent.TimeUnit

internal class InAppMessageHtmlContentResolverTest {

    @Test
    fun `text resolver returns inline html without http timeout`() {
        val sut = TextInAppMessageHtmlContentResolver()
        val html = InAppMessage.Message.Html.TextHtml("<html>inline</html>")

        val actual = sut.resolve(html)

        expectThat(actual).isEqualTo("<html>inline</html>")
    }

    @Test
    fun `path resolver applies five second timeout to remote html call`() {
        val httpClient = mockk<OkHttpClient>()
        val call = mockk<Call>()
        val timeout = mockk<Timeout>(relaxed = true)
        val requestSlot = mutableListOf<Request>()
        val response = Response.Builder()
            .request(Request.Builder().url("https://example.com/iam.html").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body("<html>remote</html>".toResponseBody("text/html".toMediaType()))
            .build()

        every { httpClient.newCall(capture(requestSlot)) } returns call
        every { call.timeout() } returns timeout
        every { timeout.timeout(5, TimeUnit.SECONDS) } returns timeout
        every { call.execute() } returns response

        val sut = PathInAppMessageHtmlContentResolver(httpClient)
        val actual = sut.resolve(InAppMessage.Message.Html.PathHtml("https://example.com/iam.html"))

        expectThat(actual).isEqualTo("<html>remote</html>")
        expectThat(requestSlot.single().url.toString()).isEqualTo("https://example.com/iam.html")
        verify(exactly = 1) { timeout.timeout(5, TimeUnit.SECONDS) }
        verify(exactly = 1) { call.execute() }
    }

    @Test(expected = InterruptedIOException::class)
    fun `path resolver propagates remote html timeout`() {
        val httpClient = mockk<OkHttpClient>()
        val call = mockk<Call>()
        val timeout = mockk<Timeout>(relaxed = true)

        every { httpClient.newCall(any()) } returns call
        every { call.timeout() } returns timeout
        every { timeout.timeout(5, TimeUnit.SECONDS) } returns timeout
        every { call.execute() } throws InterruptedIOException("timeout")

        val sut = PathInAppMessageHtmlContentResolver(httpClient)

        sut.resolve(InAppMessage.Message.Html.PathHtml("https://example.com/iam.html"))
    }
}
