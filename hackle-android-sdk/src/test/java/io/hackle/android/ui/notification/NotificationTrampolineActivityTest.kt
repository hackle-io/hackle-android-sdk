package io.hackle.android.ui.notification

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationTrampolineActivityTest {

    @Test
    fun `deep link intent flags are NEW_TASK, CLEAR_TOP, SINGLE_TOP`() {
        val expected =
            Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP

        assertEquals(expected, NotificationTrampolineActivity.DEEP_LINK_INTENT_FLAGS)
    }

    @Test
    fun `deep link intent flags must not contain NO_HISTORY`() {
        val noHistoryBit =
            NotificationTrampolineActivity.DEEP_LINK_INTENT_FLAGS and Intent.FLAG_ACTIVITY_NO_HISTORY

        assertEquals(0, noHistoryBit)
    }
}
