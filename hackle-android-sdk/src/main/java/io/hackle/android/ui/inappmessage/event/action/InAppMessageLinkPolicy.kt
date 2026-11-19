package io.hackle.android.ui.inappmessage.event.action

import java.util.Locale

/**
 * Decides whether the link of an in-app message may be opened.
 *
 * The link comes from the server, and for an HTML in-app message from the script running in the message.
 * Schemes that run script or expose local content are never a valid landing link, so they are rejected
 * before the link reaches the [io.hackle.android.ui.core.HackleUrlDispatcher]
 * and a registered [io.hackle.android.HackleDelegate].
 */
internal object InAppMessageLinkPolicy {

    private val BLOCKED_SCHEMES = setOf("javascript", "file", "content", "data", "blob", "about", "intent")

    fun isAllowed(link: String): Boolean {
        val scheme = scheme(link) ?: return true
        return scheme !in BLOCKED_SCHEMES
    }

    /**
     * Browsers ignore whitespace and control characters in a scheme (e.g. `java\tscript:`),
     * so they are ignored here as well.
     */
    private fun scheme(link: String): String? {
        val index = link.indexOf(':')
        if (index < 0) {
            return null
        }
        return link.substring(0, index).filter { it > ' ' }.lowercase(Locale.ROOT)
    }
}
