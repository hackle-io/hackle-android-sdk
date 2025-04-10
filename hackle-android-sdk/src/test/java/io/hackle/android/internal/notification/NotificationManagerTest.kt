package io.hackle.android.internal.notification

import io.hackle.android.internal.database.shared.NotificationHistoryEntity
import io.hackle.android.internal.user.UserManager
import io.hackle.android.internal.workspace.WorkspaceImpl
import io.hackle.android.ui.notification.NotificationClickAction
import io.hackle.android.ui.notification.NotificationData
import io.hackle.sdk.core.HackleCore
import io.hackle.sdk.core.workspace.WorkspaceFetcher
import io.mockk.called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifySequence
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test
import java.util.concurrent.Executor

class NotificationManagerTest {

    private lateinit var core: HackleCore
    private lateinit var userManager: UserManager
    private lateinit var executor: Executor
    private lateinit var workspaceFetcher: WorkspaceFetcher

    private val repository = MockNotificationHistoryRepository()

    private lateinit var manager: NotificationManager

    @Before
    fun setup() {
        core = mockk(relaxed = true)
        userManager = mockk()
        every { userManager.currentUser } returns mockk()
        every { userManager.toHackleUser(any()) } returns mockk()
        executor = mockk()
        every { executor.execute(any()) } answers { firstArg<Runnable>().run() }
        workspaceFetcher = mockk()
        every { workspaceFetcher.fetch() } returns WorkspaceImpl(111L, 222L, emptyList(), emptyList(), emptyMap(), emptyMap(), emptyMap(), emptyMap(), emptyMap(), emptyMap(), emptyList())

        manager = NotificationManager(
            core = core,
            executor = executor,
            workspaceFetcher = workspaceFetcher,
            userManager = userManager,
            repository = repository,
        )
    }

    @Test
    fun `track push click event when notification data received`() {
        val data = NotificationData("abcd1234", 111L, 222L, 1111L, 2222L, 3333L, 4444L, true, "#FF00FF", "foo", "bar", "https://foo.bar/image", "https://foo.foo", NotificationClickAction.APP_OPEN, "foo://bar", 1L, 2L, 3L, "JOURNEY", true)
        val timestamp = System.currentTimeMillis()
        manager.onNotificationDataReceived(data, timestamp)

        verify(exactly = 1) {
            core.track(
                withArg {
                    assertThat(it.key, `is`("\$push_click"))
                    assertThat(it.properties["push_message_id"], `is`(1111L))
                    assertThat(it.properties["push_message_key"], `is`(2222L))
                    assertThat(it.properties["push_message_execution_id"], `is`(3333L))
                    assertThat(it.properties["push_message_delivery_id"], `is`(4444L))
                    assertThat(it.properties["debug"], `is`(true))
                },
                any(), timestamp
            )
        }
    }

    @Test
    fun `save notification data if environment is not same`() {
        val timestamp = System.currentTimeMillis()
        val correctData = NotificationData("0", 111L, 222L, 1111L, 2222L, 3333L, 4444L, true, "#FF00FF", "foo", "bar", "https://foo.bar/image", "https://foo.foo", NotificationClickAction.APP_OPEN, "foo://bar", 1L, 2L, 3L, "PUSH_MESSAGE", true)
        manager.onNotificationDataReceived(correctData, timestamp)

        val diffWorkspaceData = NotificationData("1", 333L, 222L, 1111L, 2222L, 3333L, 4444L, true, "#FF00FF", "foo", "bar", "https://foo.bar/image", "https://foo.foo", NotificationClickAction.APP_OPEN, "foo://bar", 2L, 3L, 4L, "PUSH_MESSAGE", true)
        manager.onNotificationDataReceived(diffWorkspaceData, 2L)

        val diffEnvironmentData = NotificationData("2", 111L, 333L, 1111L, 2222L, 3333L, 4444L, true, "#FF00FF", "foo", "bar", "https://foo.bar/image", "https://foo.foo", NotificationClickAction.APP_OPEN, "foo://bar", 3L, 4L, 5L, "PUSH_MESSAGE", true)
        manager.onNotificationDataReceived(diffEnvironmentData, 3L)

        val bothDiffData = NotificationData("4", 333L, 333L, 1111L, 2222L, 3333L, 4444L, true, "#FF00FF", "foo", "bar", "https://foo.bar/image", "https://foo.foo", NotificationClickAction.APP_OPEN, "foo://bar", 4L, 5L, 6L, "PUSH_MESSAGE", true)
        manager.onNotificationDataReceived(bothDiffData, 4L)

        verify(exactly = 1) {
            core.track(
                withArg {
                    assertThat(it.key, `is`("\$push_click"))
                    assertThat(it.properties["push_message_id"], `is`(1111L))
                    assertThat(it.properties["push_message_key"], `is`(2222L))
                    assertThat(it.properties["push_message_execution_id"], `is`(3333L))
                    assertThat(it.properties["push_message_delivery_id"], `is`(4444L))
                    assertThat(it.properties["journey_id"], `is`(1L))
                    assertThat(it.properties["journey_key"], `is`(2L))
                    assertThat(it.properties["journey_node_id"], `is`(3L))
                    assertThat(it.properties["campaign_type"], `is`("PUSH_MESSAGE"))
                    assertThat(it.properties["debug"], `is`(true))
                }, any(), timestamp
            )
        }
        assertThat(repository.count(111L, 222L), `is`(0))
        assertThat(repository.count(333L, 222L), `is`(1))
        assertThat(repository.count(111L, 333L), `is`(1))
        assertThat(repository.count(333L, 333L), `is`(1))
    }

    @Test
    fun `save notification data if workspace fetcher returns null`() {
        every { workspaceFetcher.fetch() } returns null

        val data = NotificationData("abcd1234", 111L, 222L, 1111L, 2222L, 3333L, 4444L, true, "#FF00FF", "foo", "bar", "https://foo.bar/image", "https://foo.foo", NotificationClickAction.APP_OPEN, "foo://bar", null, null, null, null, true)
        manager.onNotificationDataReceived(data, 1L)

        verify { core wasNot called }
        assertThat(repository.count(111L, 222L), `is`(1))
    }

    @Test
    fun `flush data until empty`() {
        val notifications = listOf(
            NotificationHistoryEntity(0, 111L, 222L, 1111L, 2222L, 3333L, 4444L, 1L, 2L, 3L, "JOURNEY", 1L, true),
            NotificationHistoryEntity(1, 111L, 222L, 2222L, 3333L, 4444L, 5555L, 2L, 3L, 4L, "PUSH_MESSAGE", 2L, false),
            NotificationHistoryEntity(2, 111L, 222L, 3333L, 4444L, 5555L, 6666L, 3L, 4L, 5L, "JOURNEY", 3L, true),
        )
        repository.putAll(notifications)

        manager.flush()

        verifySequence {
            core.track(withArg {
                assertThat(it.key, `is`("\$push_click"))
                assertThat(it.properties["push_message_id"], `is`(1111L))
                assertThat(it.properties["push_message_key"], `is`(2222L))
                assertThat(it.properties["push_message_execution_id"], `is`(3333L))
                assertThat(it.properties["push_message_delivery_id"], `is`(4444L))
                assertThat(it.properties["journey_id"], `is`(1L))
                assertThat(it.properties["journey_key"], `is`(2L))
                assertThat(it.properties["journey_node_id"], `is`(3L))
                assertThat(it.properties["campaign_type"], `is`("JOURNEY"))
                assertThat(it.properties["debug"], `is`(true))
            }, any(), 1L)
            core.track(withArg {
                assertThat(it.key, `is`("\$push_click"))
                assertThat(it.properties["push_message_id"], `is`(2222L))
                assertThat(it.properties["push_message_key"], `is`(3333L))
                assertThat(it.properties["push_message_execution_id"], `is`(4444L))
                assertThat(it.properties["push_message_delivery_id"], `is`(5555L))
                assertThat(it.properties["journey_id"], `is`(2L))
                assertThat(it.properties["journey_key"], `is`(3L))
                assertThat(it.properties["journey_node_id"], `is`(4L))
                assertThat(it.properties["campaign_type"], `is`("PUSH_MESSAGE"))
                assertThat(it.properties["debug"], `is`(false))
            }, any(), 2L)
            core.track(withArg {
                assertThat(it.key, `is`("\$push_click"))
                assertThat(it.properties["push_message_id"], `is`(3333L))
                assertThat(it.properties["push_message_key"], `is`(4444L))
                assertThat(it.properties["push_message_execution_id"], `is`(5555L))
                assertThat(it.properties["push_message_delivery_id"], `is`(6666L))
                assertThat(it.properties["journey_id"], `is`(3L))
                assertThat(it.properties["journey_key"], `is`(4L))
                assertThat(it.properties["journey_node_id"], `is`(5L))
                assertThat(it.properties["campaign_type"], `is`("JOURNEY"))
                assertThat(it.properties["debug"], `is`(true))
            }, any(), 3L)
        }
        assertThat(repository.count(111L, 222L), `is`(0))
    }

    @Test
    fun `flush only same environment data`() {
        val notifications = listOf(
            NotificationHistoryEntity(0, 111L, 222L, 1L, 1L, 1L, 1L, null, null, null, null, 1L, true),
            NotificationHistoryEntity(1, 111L, 111L, 2L, 2L, 2L, 2L, null, null, null, null, 2L, false),
            NotificationHistoryEntity(2, 111L, 222L, 3L, 3L, 3L, 3L, null, null, null, null, 3L, true),
            NotificationHistoryEntity(3, 222L, 111L, 4L, 4L, 4L, 4L, null, null, null, null, 4L, false),
            NotificationHistoryEntity(4, 222L, 222L, 5L, 5L, 5L, 5L, null, null, null, null, 5L, true),
        )
        repository.putAll(notifications)

        manager.flush()

        verifySequence {
            core.track(withArg { assertThat(it.key, `is`("\$push_click")) }, any(), 1L)
            core.track(withArg { assertThat(it.key, `is`("\$push_click")) }, any(), 3L)
        }
        assertThat(repository.count(111L, 222L), `is`(0))
        assertThat(repository.count(111L, 111L), `is`(1))
        assertThat(repository.count(222L, 111L), `is`(1))
        assertThat(repository.count(222L, 222L), `is`(1))
    }

    @Test
    fun `do not flush any data if workspace returns null`() {
        val notifications = listOf(
            NotificationHistoryEntity(0, 111L, 222L, 1L, 1L, 1L, 1L, 1L, 2L, 3L, "JOURNEY", 1L, true),
            NotificationHistoryEntity(1, 111L, 111L, 2L, 2L, 2L, 2L, 1L, 2L, 3L, "JOURNEY", 2L, false),
            NotificationHistoryEntity(2, 111L, 222L, 3L, 3L, 3L, 3L, 1L, 2L, 3L, "JOURNEY", 3L, true),
            NotificationHistoryEntity(3, 222L, 222L, 4L, 4L, 4L, 4L, 1L, 2L, 3L, "JOURNEY", 4L, false),
            NotificationHistoryEntity(4, 222L, 222L, 5L, 5L, 5L, 5L, 1L, 2L, 3L, "JOURNEY", 5L, true),
        )
        repository.putAll(notifications)
        every { workspaceFetcher.fetch() } returns null

        verify { core wasNot called }
        assertThat(repository.count(111L, 111L), `is`(1))
        assertThat(repository.count(111L, 222L), `is`(2))
        assertThat(repository.count(222L, 222L), `is`(2))
    }
}