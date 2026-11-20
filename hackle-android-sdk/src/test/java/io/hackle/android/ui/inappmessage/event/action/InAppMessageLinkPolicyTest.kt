package io.hackle.android.ui.inappmessage.event.action

import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isFalse
import strikt.assertions.isTrue

internal class InAppMessageLinkPolicyTest {

    @Test
    fun `allows web links and deep links`() {
        val links = listOf(
            "https://www.hackle.io",
            "http://example.com/path?a=b",
            "myapp://home",
            "tel:01012345678",
            "mailto:support@example.com",
            "market://details?id=io.hackle.app",
            "gogo",
        )

        for (link in links) {
            expectThat(InAppMessageLinkPolicy.isAllowed(link)).describedAs(link).isTrue()
        }
    }

    @Test
    fun `blocks schemes that run script or expose local content`() {
        val links = listOf(
            "javascript:alert(1)",
            "JavaScript:alert(1)",
            " javascript:alert(1)",
            "java\tscript:alert(1)",
            "java\nscript:alert(1)",
            "file:///data/data/io.hackle.app/shared_prefs/user.xml",
            "content://io.hackle.app.provider/secret",
            "data:text/html,<script>alert(1)</script>",
            "blob:https://example.com/550e8400-e29b-41d4-a716-446655440000",
            "about:blank",
            "intent://example.com#Intent;scheme=https;end",
        )

        for (link in links) {
            expectThat(InAppMessageLinkPolicy.isAllowed(link)).describedAs(link).isFalse()
        }
    }
}
