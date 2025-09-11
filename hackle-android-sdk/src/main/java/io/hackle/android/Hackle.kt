package io.hackle.android

import android.content.Context
import android.content.Intent
import io.hackle.sdk.common.Event
import io.hackle.sdk.common.User
import io.hackle.sdk.common.subscription.HackleSubscriptionOperations
import io.hackle.sdk.common.HackleRemoteConfig

/**
 * Hackle SDK main entry point providing convenient access to SDK features.
 */
object Hackle

/**
 * Gets the [HackleApp] instance.
 */
val Hackle.app: HackleApp get() = HackleApp.getInstance()

/**
 * Registers activity lifecycle callbacks to track app state changes.
 *
 * @param context the application [Context]
 */
fun Hackle.registerActivityLifecycleCallbacks(context: Context) {
    HackleApp.registerActivityLifecycleCallbacks(context)
}

/**
 * Checks if the given intent is a Hackle push message.
 *
 * @param intent the [Intent] to check
 * @return true if the intent is a Hackle push message, false otherwise
 */
fun Hackle.isHacklePushMessage(intent: Intent) = HackleApp.isHacklePushMessage(intent)

/**
 * Initializes the Hackle SDK.
 *
 * @param context the application [Context]
 * @param sdkKey the SDK key of your Hackle environment
 * @param config the [HackleConfig] that contains the desired configuration
 * @param onReady callback that is called when HackleApp is ready to use
 * @return the initialized [HackleApp] instance
 */
fun Hackle.initialize(
    context: Context,
    sdkKey: String,
    config: HackleConfig = HackleConfig.DEFAULT,
    onReady: () -> Unit = {},
): HackleApp {
    return HackleApp.initializeApp(context, sdkKey, config, onReady)
}


/**
 * Initializes the Hackle SDK with user.
 *
 * @param context the application [Context]
 * @param sdkKey the SDK key of your Hackle environment
 * @param user the initial [User], can be null
 * @param config the [HackleConfig] that contains the desired configuration
 * @param onReady callback that is called when HackleApp is ready to use
 * @return the initialized [HackleApp] instance
 */
fun Hackle.initialize(
    context: Context,
    sdkKey: String,
    user: User? = null,
    config: HackleConfig = HackleConfig.DEFAULT,
    onReady: () -> Unit = {},
): HackleApp {
    return HackleApp.initializeApp(context, sdkKey, user, config, onReady)
}

/**
 * Creates a [User] instance with the specified ID.
 *
 * @param id the user ID
 * @return a [User] instance
 */
fun Hackle.user(id: String) = User.of(id)
/**
 * Creates a [User] instance using a builder pattern.
 *
 * @param id the user ID, can be null
 * @param init the builder configuration block
 * @return a configured [User] instance
 */
fun Hackle.user(id: String? = null, init: User.Builder.() -> Unit) =
    User.builder().id(id).apply(init).build()

/**
 * Creates an [Event] instance with the specified key.
 *
 * @param key the event key
 * @return an [Event] instance
 */
fun Hackle.event(key: String) = Event.of(key)
/**
 * Creates an [Event] instance using a builder pattern.
 *
 * @param key the event key
 * @param init the builder configuration block
 * @return a configured [Event] instance
 */
fun Hackle.event(key: String, init: Event.Builder.() -> Unit) =
    Event.builder(key).apply(init).build()


/**
 * Gets the remote config instance.
 *
 * @return the [HackleRemoteConfig] instance
 */
fun Hackle.remoteConfig() = app.remoteConfig()

/**
 * Sets the current user.
 *
 * @param user the [User] to set
 */
fun Hackle.setUser(user: User) = app.setUser(user)
/**
 * Sets the user ID for the current user.
 *
 * @param userId the user ID to set, can be null for anonymous users
 */
fun Hackle.setUserId(userId: String?) = app.setUserId(userId)
/**
 * Sets a custom device ID.
 *
 * @param deviceId the custom device ID to set
 */
fun Hackle.setDeviceId(deviceId: String) = app.setDeviceId(deviceId)
/**
 * Sets a user property.
 *
 * @param key the property key
 * @param value the property value
 */
fun Hackle.setUserProperty(key: String, value: Any?) = app.setUserProperty(key, value)
/**
 * Resets the current user.
 */
fun Hackle.resetUser() = app.resetUser()

/**
 * Sets the phone number for the current user.
 *
 * @param phoneNumber the phone number to set
 */
fun Hackle.setPhoneNumber(phoneNumber: String) = app.setPhoneNumber(phoneNumber)
/**
 * Removes the phone number from the current user.
 */
fun Hackle.unsetPhoneNumber() = app.unsetPhoneNumber()

/**
 * Updates push notification subscription status.
 *
 * @param operations the [HackleSubscriptionOperations] to apply
 */
fun Hackle.updatePushSubscriptions(operations: HackleSubscriptionOperations) = app.updatePushSubscriptions(operations)
/**
 * Updates SMS subscription status.
 *
 * @param operations the [HackleSubscriptionOperations] to apply
 */
fun Hackle.updateSmsSubscriptions(operations: HackleSubscriptionOperations) = app.updateSmsSubscriptions(operations)
/**
 * Updates KakaoTalk subscription status.
 *
 * @param operations the [HackleSubscriptionOperations] to apply
 */
fun Hackle.updateKakaoSubscriptions(operations: HackleSubscriptionOperations) = app.updateKakaoSubscriptions(operations)

/**
 * Fetches the latest configuration from the server.
 *
 * @param callback optional callback to be executed when the operation is complete
 */
fun Hackle.fetch(callback: Runnable? = null) = app.fetch(callback)

/**
 * Gets the remote config instance for a specific user.
 *
 * @param user the [User] for remote config
 * @return the [HackleRemoteConfig] instance
 */
@Deprecated(
    "Use remoteConfig() with setUser(user) instead",
    ReplaceWith("app.remoteConfig()")
)
fun Hackle.remoteConfig(user: User) = app.remoteConfig(user)

/**
 * Sets the push token.
 *
 * @param token the push token to set
 */
@Deprecated("Do not use the method because Hackle SDK will register push token by self. (Will remove v2.38.0)")
fun Hackle.setPushToken(token: String) = app.setPushToken(token)
