package io.hackle.android.ui.inappmessage.view.html

import io.hackle.sdk.core.model.InAppMessage
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

internal interface InAppMessageHtmlContentResolver<HTML : InAppMessage.Message.Html> {
    fun supports(resourceType: InAppMessage.Message.Html.ResourceType): Boolean
    fun resolve(html: HTML): String
}

internal class TextInAppMessageHtmlContentResolver :
    InAppMessageHtmlContentResolver<InAppMessage.Message.Html.TextHtml> {
    override fun supports(resourceType: InAppMessage.Message.Html.ResourceType): Boolean {
        return resourceType == InAppMessage.Message.Html.ResourceType.TEXT
    }

    override fun resolve(html: InAppMessage.Message.Html.TextHtml): String {
        return html.text
    }
}

internal class PathInAppMessageHtmlContentResolver(
    private val httpClient: OkHttpClient,
) : InAppMessageHtmlContentResolver<InAppMessage.Message.Html.PathHtml> {
    override fun supports(resourceType: InAppMessage.Message.Html.ResourceType): Boolean {
        return resourceType == InAppMessage.Message.Html.ResourceType.PATH
    }

    override fun resolve(html: InAppMessage.Message.Html.PathHtml): String {
        val url = html.path
        val request = Request.Builder().url(url).build()
        val response = httpClient.newCall(request).execute()
        return response.use { handleResponse(it) }
    }

    private fun handleResponse(response: Response): String {
        check(response.isSuccessful) { "Http status code: ${response.code()}" }
        val responseBody = checkNotNull(response.body()) { "Response body is null" }
        return responseBody.string()
    }
}

internal class InAppMessageHtmlContentResolverFactory(
    private val resolvers: List<InAppMessageHtmlContentResolver<out InAppMessage.Message.Html>>,
) {
    fun get(resourceType: InAppMessage.Message.Html.ResourceType): InAppMessageHtmlContentResolver<InAppMessage.Message.Html> {
        @Suppress("UNCHECKED_CAST")
        val resolver =
            resolvers.find { it.supports(resourceType) } as? InAppMessageHtmlContentResolver<InAppMessage.Message.Html>
        return requireNotNull(resolver) { "Not found InAppMessageHtmlContentResolver [$resourceType]" }
    }
}
