package io.hackle.android.internal.push.token

import android.content.Context
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import io.hackle.sdk.core.internal.log.Logger

internal class FcmPushTokenFetcher(private val firebaseApp: FirebaseApp) : PushTokenFetcher {
    override fun fetch(): PushToken? {
        try {
            log.debug { "Attempt to fetch PushToken from FirebaseMessaging" }
            val tokenValue = fetchFromFirebaseMessaging()
            log.debug { "Successfully fetched PushToken from FirebaseMessaging" }
            return PushToken.of(tokenValue)
        } catch (e: Throwable) {
            log.debug { "Failed to fetch PushToken from FirebaseMessaging: $e" }
        }

        try {
            log.debug { "Attempt to fetch PushToken from FirebaseInstanceId" }
            val tokenValue = fetchFromFirebaseInstanceId()
            log.debug { "Successfully fetched PushToken from FirebaseInstanceId" }
            return PushToken.of(tokenValue)
        } catch (e: Throwable) {
            log.debug { "Failed to fetch PushToken from FirebaseInstanceId: $e" }
        }

        return null
    }

    // FirebaseMessaging.getToken API was introduced since firebase-messaging:21.0.0
    private fun fetchFromFirebaseMessaging(): String {
        val firebaseMessaging = firebaseApp.get(FirebaseMessaging::class.java)
        return Tasks.await(firebaseMessaging.token)
    }

    // This method for less than firebase-messaging:21.0.0
    private fun fetchFromFirebaseInstanceId(): String {
        val firebaseInstanceIdClass = Class.forName("com.google.firebase.iid.FirebaseInstanceId")
        val getInstanceMethod = firebaseInstanceIdClass.getMethod("getInstance", FirebaseApp::class.java)

        val instanceId = getInstanceMethod.invoke(null, firebaseApp)
        requireNotNull(instanceId) { "Failed to get FirebaseInstanceId." }

        val getTokenMethod = instanceId.javaClass.getMethod("getToken")
        val token = getTokenMethod.invoke(instanceId)
        requireNotNull(token) { "Failed to get push token from FirebaseInstanceId." }

        return requireNotNull(token as? String) { "Push token value must be string [${token::class.java}, $token]" }
    }

    companion object {
        private val log = Logger<FcmPushTokenFetcher>()

        private const val FCM_APP_NAME = "HACKLE_SDK_APP"

        fun create(context: Context): FcmPushTokenFetcher {
            require(isFirebaseMessagingAvailable()) { "FirebaseMessaging not available." }
            val options =
                requireNotNull(FirebaseOptions.fromResource(context)) { "Failed to get FirebaseOptions from resource." }
            val firebaseApp = FirebaseApp.initializeApp(context, options, FCM_APP_NAME)
            return FcmPushTokenFetcher(firebaseApp)
        }

        private fun isFirebaseMessagingAvailable(): Boolean {
            return try {
                Class.forName("com.google.firebase.messaging.FirebaseMessaging")
                true
            } catch (_: Throwable) {
                false
            }
        }
    }
}
