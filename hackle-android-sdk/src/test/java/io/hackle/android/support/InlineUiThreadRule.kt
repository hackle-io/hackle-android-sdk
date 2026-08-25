package io.hackle.android.support

import io.hackle.android.internal.task.TaskExecutors
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.rules.ExternalResource

/**
 * TaskExecutors.runOnUiThread를 호출 스레드에서 즉시 실행하도록 모킹한다.
 */
internal class InlineUiThreadRule : ExternalResource() {

    override fun before() {
        mockkObject(TaskExecutors)
        every { TaskExecutors.runOnUiThread(any()) } answers { firstArg<() -> Unit>()() }
    }

    override fun after() {
        unmockkObject(TaskExecutors)
    }
}
