package io.hackle.android.internal.notification

import io.hackle.android.internal.database.repository.KeyValueRepository
import io.hackle.android.internal.database.repository.MapKeyValueRepository
import io.hackle.android.internal.database.shared.NotificationEntity
import io.hackle.android.internal.user.UserManager
import io.hackle.android.internal.workspace.WorkspaceImpl
import io.hackle.android.ui.notification.NotificationClickAction
import io.hackle.android.ui.notification.NotificationData
import io.hackle.sdk.common.User
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
    private lateinit var preferences: KeyValueRepository
    private lateinit var userManager: UserManager
    private lateinit var executor: Executor
    private lateinit var workspaceFetcher: WorkspaceFetcher

    private val repository = MockNotificationRepository()

    private lateinit var manager: NotificationManager

    @Before
    fun setup() {
        core = mockk(relaxed = true)
        preferences = MapKeyValueRepository()
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
            preferences = preferences,
            repository = repository,
        )
    }

    @Test
    fun `fresh new push token`() {
        val timestamp = System.currentTimeMillis()
        manager.setPushToken("abcd1234", timestamp)

        verify(exactly = 1) {
            core.track(
                withArg {
                    assertThat(it.key, `is`("\$push_token"))
                    assertThat(it.properties["provider_type"], `is`("FCM"))
                    assertThat(it.properties["token"], `is`("abcd1234"))
                },
                any(), timestamp
            )
        }
        assertThat(preferences.getString("fcm_token"), `is`("abcd1234"))
    }

    @Test
    fun `set another push token`() {
        preferences.putString("fcm_token", "foo")

        val timestamp = System.currentTimeMillis()
        manager.setPushToken("bar", timestamp)

        verify(exactly = 1) {
            core.track(
                withArg {
                    assertThat(it.key, `is`("\$push_token"))
                    assertThat(it.properties["provider_type"], `is`("FCM"))
                    assertThat(it.properties["token"], `is`("bar"))
                },
                any(), timestamp
            )
        }
        assertThat(preferences.getString("fcm_token"), `is`("bar"))
    }

    @Test
    fun `set same push token`() {
        preferences.putString("fcm_token", "foo")

        manager.setPushToken("foo")

        verify { core wasNot called }
        assertThat(preferences.getString("fcm_token"), `is`("foo"))
    }

    @Test
    fun `resend push token when user updated called`() {
        preferences.putString("fcm_token", "foo")

        val timestamp = System.currentTimeMillis()
        manager.onUserUpdated(User.of("foo"), User.of("bar"), timestamp)

        verify(exactly = 1) {
            core.track(
                withArg {
                    assertThat(it.key, `is`("\$push_token"))
                    assertThat(it.properties["token"], `is`("foo"))
                }, any(), timestamp
            )
        }
        assertThat(preferences.getString("fcm_token"), `is`("foo"))
    }

    @Test
    fun `track push click event when notification data received`() {
        val data = NotificationData("abcd1234", 111L, 222L, 1111L, 2222L, 3333L, 4444L, true, "#FF00FF", "foo", "bar", "https://foo.bar/image", "https://foo.foo", NotificationClickAction.APP_OPEN, "foo://bar", true)
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
        val correctData = NotificationData("0", 111L, 222L, 1111L, 2222L, 3333L, 4444L, true, "#FF00FF", "foo", "bar", "https://foo.bar/image", "https://foo.foo", NotificationClickAction.APP_OPEN, "foo://bar", true)
        manager.onNotificationDataReceived(correctData, timestamp)

        val diffWorkspaceData = NotificationData("1", 333L, 222L, 1111L, 2222L, 3333L, 4444L, true, "#FF00FF", "foo", "bar", "https://foo.bar/image", "https://foo.foo", NotificationClickAction.APP_OPEN, "foo://bar", true)
        manager.onNotificationDataReceived(diffWorkspaceData, 2L)

        val diffEnvironmentData = NotificationData("2", 111L, 333L, 1111L, 2222L, 3333L, 4444L, true, "#FF00FF", "foo", "bar", "https://foo.bar/image", "https://foo.foo", NotificationClickAction.APP_OPEN, "foo://bar", true)
        manager.onNotificationDataReceived(diffEnvironmentData, 3L)

        val bothDiffData = NotificationData("4", 333L, 333L, 1111L, 2222L, 3333L, 4444L, true, "#FF00FF", "foo", "bar", "https://foo.bar/image", "https://foo.foo", NotificationClickAction.APP_OPEN, "foo://bar", true)
        manager.onNotificationDataReceived(bothDiffData, 4L)

        verify(exactly = 1) {
            core.track(
                withArg {
                    assertThat(it.key, `is`("\$push_click"))
                    assertThat(it.properties["push_message_id"], `is`(1111L))
                    assertThat(it.properties["push_message_key"], `is`(2222L))
                    assertThat(it.properties["push_message_execution_id"], `is`(3333L))
                    assertThat(it.properties["push_message_delivery_id"], `is`(4444L))
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

        val data = NotificationData("abcd1234", 111L, 222L, 1111L, 2222L, 3333L, 4444L, true, "#FF00FF", "foo", "bar", "https://foo.bar/image", "https://foo.foo", NotificationClickAction.APP_OPEN, "foo://bar", true)
        manager.onNotificationDataReceived(data, 1L)

        verify { core wasNot called }
        assertThat(repository.count(111L, 222L), `is`(1))
    }

    @Test
    fun `flush data until empty`() {
        val notifications = listOf(
            NotificationEntity("0", 111L, 222L, 1111L, 2222L, 3333L, 4444L, 1L, true),
            NotificationEntity("1", 111L, 222L, 2222L, 3333L, 4444L, 5555L, 2L, false),
            NotificationEntity("2", 111L, 222L, 3333L, 4444L, 5555L, 6666L, 3L, true),
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
                assertThat(it.properties["debug"], `is`(true))
            }, any(), 1L)
            core.track(withArg {
                assertThat(it.key, `is`("\$push_click"))
                assertThat(it.properties["push_message_id"], `is`(2222L))
                assertThat(it.properties["push_message_key"], `is`(3333L))
                assertThat(it.properties["push_message_execution_id"], `is`(4444L))
                assertThat(it.properties["push_message_delivery_id"], `is`(5555L))
                assertThat(it.properties["debug"], `is`(false))
            }, any(), 2L)
            core.track(withArg {
                assertThat(it.key, `is`("\$push_click"))
                assertThat(it.properties["push_message_id"], `is`(3333L))
                assertThat(it.properties["push_message_key"], `is`(4444L))
                assertThat(it.properties["push_message_execution_id"], `is`(5555L))
                assertThat(it.properties["push_message_delivery_id"], `is`(6666L))
                assertThat(it.properties["debug"], `is`(true))
            }, any(), 3L)
        }
        assertThat(repository.count(111L, 222L), `is`(0))
    }

    @Test
    fun `flush only same environment data`() {
        val notifications = listOf(
            NotificationEntity("0", 111L, 222L, 1L, 1L, 1L, 1L, 1L, true),
            NotificationEntity("1", 111L, 111L, 2L, 2L, 2L, 2L, 2L, false),
            NotificationEntity("2", 111L, 222L, 3L, 3L, 3L, 3L, 3L, true),
            NotificationEntity("3", 222L, 111L, 4L, 4L, 4L, 4L, 4L, false),
            NotificationEntity("4", 222L, 222L, 5L, 5L, 5L, 5L, 5L, true),
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
            NotificationEntity("0", 111L, 222L, 1L, 1L, 1L, 1L, 1L, true),
            NotificationEntity("1", 111L, 111L, 2L, 2L, 2L, 2L, 2L, false),
            NotificationEntity("2", 111L, 222L, 3L, 3L, 3L, 3L, 3L, true),
            NotificationEntity("3", 222L, 222L, 4L, 4L, 4L, 4L, 4L, false),
            NotificationEntity("4", 222L, 222L, 5L, 5L, 5L, 5L, 5L, true),
        )
        repository.putAll(notifications)
        every { workspaceFetcher.fetch() } returns null

        verify { core wasNot called }
        assertThat(repository.count(111L, 111L), `is`(1))
        assertThat(repository.count(111L, 222L), `is`(2))
        assertThat(repository.count(222L, 222L), `is`(2))
    }
}