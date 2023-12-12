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
        manager.setPushToken("abcd1234")

        verify(exactly = 1) {
            core.track(
                withArg {
                    assertThat(it.key, `is`("\$push_token"))
                    assertThat(it.properties["fcm_token"], `is`("abcd1234"))
                },
                any(), any()
            )
        }
        assertThat(preferences.getString("fcm_token"), `is`("abcd1234"))
    }

    @Test
    fun `set another push token`() {
        preferences.putString("fcm_token", "foo")

        manager.setPushToken("bar")

        verify(exactly = 1) {
            core.track(
                withArg {
                    assertThat(it.key, `is`("\$push_token"))
                    assertThat(it.properties["fcm_token"], `is`("bar"))
                },
                any(), any()
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

        manager.onUserUpdated(User.of("foo"), User.of("bar"), System.currentTimeMillis())

        verify(exactly = 1) {
            core.track(
                withArg {
                    assertThat(it.key, `is`("\$push_token"))
                    assertThat(it.properties["fcm_token"], `is`("foo"))
                },
                any(), any()
            )
        }
        assertThat(preferences.getString("fcm_token"), `is`("foo"))
    }

    @Test
    fun `track push click event when notification data received`() {
        val data = NotificationData("abcd1234", 111L, 222L, 789L, 1234567890L, true, "#FF00FF", "foo", "bar", "https://foo.bar/image", "https://foo.foo", NotificationClickAction.APP_OPEN, "foo://bar")
        manager.onNotificationDataReceived(data, 123456789L)

        verify(exactly = 1) {
            core.track(
                withArg {
                    assertThat(it.key, `is`("\$push_click"))
                },
                any(), any()
            )
        }
    }

    @Test
    fun `save notification data if environment is not same`() {
        val correctData = NotificationData("0", 111L, 222L, 789L, 1234567890L, true, "#FF00FF", "foo", "bar", "https://foo.bar/image", "https://foo.foo", NotificationClickAction.APP_OPEN, "foo://bar")
        manager.onNotificationDataReceived(correctData, 1L)

        val diffWorkspaceData = NotificationData("1", 222L, 222L, 789L, 1234567890L, true, "#FF00FF", "foo", "bar", "https://foo.bar/image", "https://foo.foo", NotificationClickAction.APP_OPEN, "foo://bar")
        manager.onNotificationDataReceived(diffWorkspaceData, 2L)

        val diffEnvironmentData = NotificationData("2", 111L, 333L, 789L, 1234567890L, true, "#FF00FF", "foo", "bar", "https://foo.bar/image", "https://foo.foo", NotificationClickAction.APP_OPEN, "foo://bar")
        manager.onNotificationDataReceived(diffEnvironmentData, 3L)

        val bothDiffData = NotificationData("3", 333L, 333L, 789L, 1234567890L, true, "#FF00FF", "foo", "bar", "https://foo.bar/image", "https://foo.foo", NotificationClickAction.APP_OPEN, "foo://bar")
        manager.onNotificationDataReceived(bothDiffData, 4L)

        verify(exactly = 1) { core.track(any(), any(), any()) }
        assertThat(repository.count(), `is`(3))
    }

    @Test
    fun `save notification data if workspace fetcher returns null`() {
        every { workspaceFetcher.fetch() } returns null

        val data = NotificationData("0", 111L, 222L, 789L, 1234567890L, true, "#FF00FF", "foo", "bar", "https://foo.bar/image", "https://foo.foo", NotificationClickAction.APP_OPEN, "foo://bar")
        manager.onNotificationDataReceived(data, 1L)

        verify { core wasNot called }
        assertThat(repository.count(), `is`(1))
    }

    @Test
    fun `flush data until empty`() {
        val notifications = listOf(
            NotificationEntity("0", 111L, 222L, 111L, 1L, "APP_OPEN", 4L, null),
            NotificationEntity("1", 111L, 222L, 222L, 2L, "DEEP_LINK", 5L, "foo://bar"),
            NotificationEntity("2", 111L, 222L, 333L, 3L, "DEEP_LINK", 6L, "bar://foo")
        )
        repository.replaceAll(notifications)

        manager.flush()

        verifySequence {
            core.track(withArg { assertThat(it.key, `is`("\$push_click")) }, any(), any())
            core.track(withArg { assertThat(it.key, `is`("\$push_click")) }, any(), any())
            core.track(withArg { assertThat(it.key, `is`("\$push_click")) }, any(), any())
        }
        assertThat(repository.count(), `is`(0))
    }

    @Test
    fun `flush only same environment data`() {
        val notifications = listOf(
            NotificationEntity("0", 111L, 222L, 111L, 1L, "APP_OPEN", 4L, null),
            NotificationEntity("1", 111L, 111L, 222L, 2L, "DEEP_LINK", 5L, "foo://bar"),
            NotificationEntity("2", 111L, 222L, 333L, 3L, "DEEP_LINK", 6L, "bar://foo"),
            NotificationEntity("3", 222L, 111L, 444L, 4L, "APP_OPEN", 7L, null),
            NotificationEntity("4", 222L, 222L, 555L, 5L, "APP_OPEN", 8L, null),
        )
        repository.replaceAll(notifications)

        manager.flush()

        verifySequence {
            core.track(withArg { assertThat(it.key, `is`("\$push_click")) }, any(), any())
            core.track(withArg { assertThat(it.key, `is`("\$push_click")) }, any(), any())
        }
        assertThat(repository.count(), `is`(3))
    }

    @Test
    fun `do not flush any data if workspace returns null`() {
        val notifications = listOf(
            NotificationEntity("0", 111L, 222L, 111L, 1L, "APP_OPEN", 4L, null),
            NotificationEntity("1", 111L, 111L, 222L, 2L, "DEEP_LINK", 5L, "foo://bar"),
            NotificationEntity("2", 111L, 222L, 333L, 3L, "DEEP_LINK", 6L, "bar://foo"),
            NotificationEntity("3", 222L, 111L, 444L, 4L, "APP_OPEN", 7L, null),
            NotificationEntity("4", 222L, 222L, 555L, 5L, "APP_OPEN", 8L, null),
        )
        repository.replaceAll(notifications)
        every { workspaceFetcher.fetch() } returns null

        verify { core wasNot called }
        assertThat(repository.count(), `is`(5))
    }
}