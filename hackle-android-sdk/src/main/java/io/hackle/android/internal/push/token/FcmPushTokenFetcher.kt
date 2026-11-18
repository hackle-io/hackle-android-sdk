package io.hackle.android.internal.push.token

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import io.hackle.sdk.core.internal.log.Logger
import java.util.concurrent.Executor

internal class FcmPushTokenFetcher(
    private val firebaseApp: FirebaseApp,
    private val executor: Executor,
) : PushTokenFetcher {

    override fun fetch(callback: (PushToken?) -> Unit) {
        try {
            log.debug { "Attempt to fetch PushToken from FirebaseMessaging" }
            fetchFromFirebaseMessaging(callback)
        } catch (e: Throwable) {
            log.warn { "Failed to fetch PushToken from FirebaseMessaging: $e" }
            callback(fetchFromFirebaseInstanceIdOrNull())
        }
    }

    // FirebaseMessaging.getToken API was introduced since firebase-messaging:21.0.0
    private fun fetchFromFirebaseMessaging(callback: (PushToken?) -> Unit) {
        val firebaseMessaging = firebaseApp.get(FirebaseMessaging::class.java)
        firebaseMessaging.token.addOnCompleteListener(executor) { task ->
            callback(resolve(task))
        }
    }

    private fun resolve(task: Task<String>): PushToken? {
        return try {
            if (task.isSuccessful) {
                log.debug { "Successfully fetched PushToken from FirebaseMessaging" }
                toPushToken(task.result)
            } else {
                log.warn { "Failed to fetch PushToken from FirebaseMessaging: ${task.exception}" }
                fetchFromFirebaseInstanceIdOrNull()
            }
        } catch (e: Throwable) {
            log.warn { "Failed to resolve PushToken from FirebaseMessaging: $e" }
            fetchFromFirebaseInstanceIdOrNull()
        }
    }

    private fun fetchFromFirebaseInstanceIdOrNull(): PushToken? {
        return try {
            log.debug { "Attempt to fetch PushToken from FirebaseInstanceId" }
            val tokenValue = fetchFromFirebaseInstanceId()
            log.debug { "Successfully fetched PushToken from FirebaseInstanceId" }
            toPushToken(tokenValue)
        } catch (e: Throwable) {
            log.warn { "Failed to fetch PushToken from FirebaseInstanceId: $e" }
            null
        }
    }

    private fun toPushToken(tokenValue: String): PushToken {
        if (tokenValue.isBlank()) {
            log.warn { "PushToken value is blank." }
        }
        return PushToken.of(tokenValue)
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

        fun create(context: Context, executor: Executor): FcmPushTokenFetcher {
            require(isFirebaseMessagingAvailable()) { "FirebaseMessaging not available." }
            val options =
                requireNotNull(FirebaseOptions.fromResource(context)) { "Failed to get FirebaseOptions from resource." }
            val firebaseApp = FirebaseApp.initializeApp(context, options, FCM_APP_NAME)
            return FcmPushTokenFetcher(firebaseApp, executor)
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
