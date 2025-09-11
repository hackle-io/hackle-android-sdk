package io.hackle.android

import android.content.Context
import android.content.Intent
import io.hackle.sdk.common.Event
import io.hackle.sdk.common.User
import io.hackle.sdk.common.HackleRemoteConfig
import io.hackle.sdk.common.subscription.HackleSubscriptionOperations
import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.junit.After
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isA

class HackleTest {

    @MockK
    private lateinit var hackleApp: HackleApp
    
    @MockK
    private lateinit var context: Context
    
    @MockK
    private lateinit var intent: Intent
    
    @MockK
    private lateinit var remoteConfig: HackleRemoteConfig
    
    @MockK
    private lateinit var subscriptionOperations: HackleSubscriptionOperations

    private val testSdkKey = "test_sdk_key"
    private val testUserId = "test_user_id"
    private val testDeviceId = "test_device_id"
    private val testEventKey = "test_event"
    private val testPropertyKey = "test_property"
    private val testPropertyValue = "test_value"
    private val testPhoneNumber = "010-1234-5678"
    private val testPushToken = "test_push_token"

    @Before
    fun before() {
        MockKAnnotations.init(this, relaxUnitFun = true)

        // HackleApp static methods 모킹
        mockkObject(HackleApp)
        every { HackleApp.getInstance() } returns hackleApp
        every { HackleApp.initializeApp(any(), any(), any<HackleConfig>(), any()) } returns hackleApp
        every { HackleApp.initializeApp(any(), any(), any(), any<HackleConfig>(), any()) } returns hackleApp
        every { HackleApp.registerActivityLifecycleCallbacks(any()) } just Runs
        every { HackleApp.isHacklePushMessage(any()) } returns true
        
        // HackleApp 인스턴스 메소드들 모킹
        every { hackleApp.remoteConfig() } returns remoteConfig
        every { hackleApp.remoteConfig(any<User>()) } returns remoteConfig
        every { hackleApp.setUser(any(), any()) } just Runs
        every { hackleApp.setUserId(any(), any()) } just Runs
        every { hackleApp.setDeviceId(any()) } just Runs
        every { hackleApp.setUserProperty(any(), any()) } just Runs
        every { hackleApp.resetUser() } just Runs
        every { hackleApp.setPhoneNumber(any()) } just Runs
        every { hackleApp.unsetPhoneNumber() } just Runs
        every { hackleApp.updatePushSubscriptions(any()) } just Runs
        every { hackleApp.updateSmsSubscriptions(any()) } just Runs
        every { hackleApp.updateKakaoSubscriptions(any()) } just Runs
        every { hackleApp.fetch(any()) } just Runs
        every { hackleApp.setPushToken(any()) } just Runs
    }

    @After
    fun tearDown() {
        unmockkObject(HackleApp)
        clearAllMocks()
    }

    @Test
    fun `app property should return HackleApp getInstance`() {
        // when
        val result = Hackle.app
        
        // then
        expectThat(result).isEqualTo(hackleApp)
        verify { HackleApp.getInstance() }
    }

    @Test
    fun `registerActivityLifecycleCallbacks should delegate to HackleApp`() {
        // when
        Hackle.registerActivityLifecycleCallbacks(context)
        
        // then
        verify { HackleApp.registerActivityLifecycleCallbacks(context) }
    }

    @Test
    fun `isHacklePushMessage should delegate to HackleApp`() {
        // when
        val result = Hackle.isHacklePushMessage(intent)
        
        // then
        expectThat(result).isEqualTo(true)
        verify { HackleApp.isHacklePushMessage(intent) }
    }

    @Test
    fun `initialize with context sdkKey and config should delegate to HackleApp initializeApp`() {
        // given
        val config = HackleConfig.DEFAULT
        val onReady: () -> Unit = {}
        
        // when
        val result = Hackle.initialize(context, testSdkKey, config, onReady)
        
        // then
        expectThat(result).isEqualTo(hackleApp)
        verify { HackleApp.initializeApp(context, testSdkKey, config, any()) }
    }

    @Test
    fun `initialize with context sdkKey user and config should delegate to HackleApp initializeApp`() {
        // given
        val user = User.of(testUserId)
        val config = HackleConfig.DEFAULT
        val onReady: () -> Unit = {}
        
        // when
        val result = Hackle.initialize(context, testSdkKey, user, config, onReady)
        
        // then
        expectThat(result).isEqualTo(hackleApp)
        verify { HackleApp.initializeApp(context, testSdkKey, user, config, any()) }
    }

    @Test
    fun `user with id should create User instance`() {
        // when
        val result = Hackle.user(testUserId)
        
        // then
        expectThat(result).isA<User>()
        expectThat(result.id).isEqualTo(testUserId)
    }

    @Test
    fun `user with id and builder should create configured User instance`() {
        // when
        val result = Hackle.user(testUserId) {
            property("key", "value")
        }
        
        // then
        expectThat(result).isA<User>()
        expectThat(result.id).isEqualTo(testUserId)
    }

    @Test
    fun `event with key should create Event instance`() {
        // when
        val result = Hackle.event(testEventKey)
        
        // then
        expectThat(result).isA<Event>()
        expectThat(result.key).isEqualTo(testEventKey)
    }

    @Test
    fun `event with key and builder should create configured Event instance`() {
        // when
        val result = Hackle.event(testEventKey) {
            property("key", "value")
        }
        
        // then
        expectThat(result).isA<Event>()
        expectThat(result.key).isEqualTo(testEventKey)
    }

    @Test
    fun `remoteConfig should delegate to app remoteConfig`() {
        // when
        val result = Hackle.remoteConfig()
        
        // then
        expectThat(result).isEqualTo(remoteConfig)
        verify { hackleApp.remoteConfig() }
    }

    @Test
    fun `setUser should delegate to app setUser`() {
        // given
        val user = User.of(testUserId)
        
        // when
        Hackle.setUser(user)
        
        // then
        verify { hackleApp.setUser(user, null) }
    }

    @Test
    fun `setUserId should delegate to app setUserId`() {
        // when
        Hackle.setUserId(testUserId)
        
        // then
        verify { hackleApp.setUserId(testUserId, null) }
    }

    @Test
    fun `setDeviceId should delegate to app setDeviceId`() {
        // when
        Hackle.setDeviceId(testDeviceId)
        
        // then
        verify { hackleApp.setDeviceId(testDeviceId) }
    }

    @Test
    fun `setUserProperty should delegate to app setUserProperty`() {
        // when
        Hackle.setUserProperty(testPropertyKey, testPropertyValue)
        
        // then
        verify { hackleApp.setUserProperty(testPropertyKey, testPropertyValue) }
    }

    @Test
    fun `resetUser should delegate to app resetUser`() {
        // when
        Hackle.resetUser()
        
        // then
        verify { hackleApp.resetUser() }
    }

    @Test
    fun `setPhoneNumber should delegate to app setPhoneNumber`() {
        // when
        Hackle.setPhoneNumber(testPhoneNumber)
        
        // then
        verify { hackleApp.setPhoneNumber(testPhoneNumber) }
    }

    @Test
    fun `unsetPhoneNumber should delegate to app unsetPhoneNumber`() {
        // when
        Hackle.unsetPhoneNumber()
        
        // then
        verify { hackleApp.unsetPhoneNumber() }
    }

    @Test
    fun `updatePushSubscriptions should delegate to app updatePushSubscriptions`() {
        // when
        Hackle.updatePushSubscriptions(subscriptionOperations)
        
        // then
        verify { hackleApp.updatePushSubscriptions(subscriptionOperations) }
    }

    @Test
    fun `updateSmsSubscriptions should delegate to app updateSmsSubscriptions`() {
        // when
        Hackle.updateSmsSubscriptions(subscriptionOperations)
        
        // then
        verify { hackleApp.updateSmsSubscriptions(subscriptionOperations) }
    }

    @Test
    fun `updateKakaoSubscriptions should delegate to app updateKakaoSubscriptions`() {
        // when
        Hackle.updateKakaoSubscriptions(subscriptionOperations)
        
        // then
        verify { hackleApp.updateKakaoSubscriptions(subscriptionOperations) }
    }

    @Test
    fun `fetch should delegate to app fetch`() {
        // given
        val callback = Runnable { }
        
        // when
        Hackle.fetch(callback)
        
        // then
        verify { hackleApp.fetch(callback) }
    }

    @Test
    fun `fetch without callback should delegate to app fetch with null`() {
        // when
        Hackle.fetch()
        
        // then
        verify { hackleApp.fetch(null) }
    }

    @Test
    fun `deprecated remoteConfig with user should delegate to app remoteConfig`() {
        // given
        val user = User.of(testUserId)
        
        // when
        @Suppress("DEPRECATION")
        val result = Hackle.remoteConfig(user)
        
        // then
        expectThat(result).isEqualTo(remoteConfig)
        verify { hackleApp.remoteConfig(user) }
    }

    @Test
    fun `deprecated setPushToken should delegate to app setPushToken`() {
        // when
        @Suppress("DEPRECATION")
        Hackle.setPushToken(testPushToken)
        
        // then
        verify { hackleApp.setPushToken(testPushToken) }
    }

    // 기존 테스트들도 유지
    @Test
    fun `user builder function should create user with id`() {
        val userId = "test_user_id"
        
        val result = Hackle.user(userId)
        
        expectThat(result.id).isEqualTo(userId)
    }

    @Test
    fun `user builder function with init block should create configured user`() {
        val userId = "test_user_id"
        
        val result = Hackle.user(userId) {
            property("age", 25)
            property("name", "test_name")
        }
        
        expectThat(result.id).isEqualTo(userId)
        expectThat(result.properties["age"]).isEqualTo(25)
        expectThat(result.properties["name"]).isEqualTo("test_name")
    }

    @Test
    fun `user builder function without id should create user with null id`() {
        val result = Hackle.user(null) {
            property("age", 30)
        }
        
        expectThat(result.id).isEqualTo(null)
        expectThat(result.properties["age"]).isEqualTo(30)
    }

    @Test
    fun `event builder function should create event with key`() {
        val eventKey = "test_event"
        
        val result = Hackle.event(eventKey)
        
        expectThat(result.key).isEqualTo(eventKey)
    }

    @Test
    fun `event builder function with init block should create configured event`() {
        val eventKey = "test_event"
        
        val result = Hackle.event(eventKey) {
            property("count", 5)
            property("category", "test")
        }
        
        expectThat(result.key).isEqualTo(eventKey)
        expectThat(result.properties["count"]).isEqualTo(5)
        expectThat(result.properties["category"]).isEqualTo("test")
    }

    @Test
    fun `User builder should create user with properties`() {
        val result = User.builder()
            .id("user123")
            .property("name", "John")
            .property("age", 25)
            .build()

        expectThat(result.id).isEqualTo("user123")
        expectThat(result.properties["name"]).isEqualTo("John")
        expectThat(result.properties["age"]).isEqualTo(25)
    }

    @Test
    fun `Event builder should create event with properties`() {
        val result = Event.builder("click")
            .property("button", "submit")
            .property("count", 1)
            .build()

        expectThat(result.key).isEqualTo("click")
        expectThat(result.properties["button"]).isEqualTo("submit")
        expectThat(result.properties["count"]).isEqualTo(1)
    }
}