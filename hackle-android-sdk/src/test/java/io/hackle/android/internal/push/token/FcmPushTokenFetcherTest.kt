package io.hackle.android.internal.push.token

import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEmpty
import java.util.concurrent.Executor

class FcmPushTokenFetcherTest {

    private lateinit var firebaseApp: FirebaseApp
    private lateinit var firebaseMessaging: FirebaseMessaging
    private lateinit var task: Task<String>
    private lateinit var listener: CapturingSlot<OnCompleteListener<String>>
    private lateinit var fetched: MutableList<PushToken?>

    private lateinit var sut: FcmPushTokenFetcher

    @Before
    fun before() {
        firebaseApp = mockk()
        firebaseMessaging = mockk()
        task = mockk()
        listener = slot()
        fetched = mutableListOf()

        every { firebaseApp.get(FirebaseMessaging::class.java) } returns firebaseMessaging
        every { firebaseMessaging.token } returns task
        every { task.addOnCompleteListener(any<Executor>(), capture(listener)) } returns task

        sut = FcmPushTokenFetcher(firebaseApp, Executor { it.run() })
    }

    private fun complete() {
        listener.captured.onComplete(task)
    }

    @Test
    fun `fetch - does not invoke callback until task is completed`() {
        // given
        every { task.isSuccessful } returns true
        every { task.result } returns "token"

        // when
        sut.fetch { fetched.add(it) }

        // then
        expectThat(fetched).isEmpty()

        // when
        complete()

        // then
        expectThat(fetched).containsExactly(PushToken.of("token"))
    }

    @Test
    fun `fetch - when task failed and FirebaseInstanceId is not available then callback with null`() {
        // given
        every { task.isSuccessful } returns false
        every { task.exception } returns IllegalStateException("fail")

        // when
        sut.fetch { fetched.add(it) }
        complete()

        // then
        expectThat(fetched).containsExactly(null)
    }

    @Test
    fun `fetch - when task result is null then callback with null without throwing`() {
        // given
        every { task.isSuccessful } returns true
        every { task.result } returns null

        // when
        sut.fetch { fetched.add(it) }
        complete()

        // then
        expectThat(fetched).containsExactly(null)
    }

    @Test
    fun `fetch - when task result throws then callback with null without throwing`() {
        // given
        every { task.isSuccessful } returns true
        every { task.result } throws IllegalStateException("fail")

        // when
        sut.fetch { fetched.add(it) }
        complete()

        // then
        expectThat(fetched).containsExactly(null)
    }

    @Test
    fun `fetch - when FirebaseMessaging throws synchronously then callback with null`() {
        // given
        every { firebaseApp.get(FirebaseMessaging::class.java) } throws NoSuchMethodError("getToken")

        // when
        sut.fetch { fetched.add(it) }

        // then
        expectThat(fetched).containsExactly(null)
    }
}
