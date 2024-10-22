package io.hackle.android.sdk.tester

import android.app.Activity
import io.hackle.sdk.common.User
import java.util.*

class UserService(
    private val activity: Activity
) {

    fun user(): User {
        val builder = defaultIdOrNull()?.let { User.builder(it) } ?: User.builder()
        return builder
            .userId(userIdOrNull())
            .deviceId(deviceIdOrNull())
            .property("age", listOf(31, 32, 32.1))
            .property(property(R.id.property_key_01, R.id.property_value_01))
            .property(property(R.id.property_key_02, R.id.property_value_02))
            .build()
    }

    private fun User.Builder.property(property: Pair<String, Any?>?) = apply {
        if (property != null) {
            property(property.first, property.second)
        }
    }

    fun defaultIdOrNull(): String? {
        return if (activity.isChecked(R.id.default_id_random_checkbox)) {
            UUID.randomUUID().toString()
        } else {
            activity.textOrNull(R.id.default_id)
        }
    }

    fun userIdOrNull(): String? {
        return if (activity.isChecked(R.id.user_id_random_checkbox)) {
            UUID.randomUUID().toString()
        } else {
            activity.textOrNull(R.id.user_id)
        }
    }

    fun deviceIdOrNull(): String? {
        return if (activity.isChecked(R.id.device_id_random_checkbox)) {
            UUID.randomUUID().toString()
        } else {
            activity.textOrNull(R.id.device_id)
        }
    }

    fun property(keyId: Int, valueId: Int): Pair<String, Any?>? {
        val propertyKey = activity.textOrNull(keyId) ?: return null
        val propertyValue = activity.textOrNull(valueId)

        val propertyValueInt = propertyValue?.toIntOrNull()
        if (propertyValueInt != null) {
            return (propertyKey to propertyValueInt)
        }

        val propertyValueBoolean = if (propertyValue == "true") {
            true
        } else if (propertyValue == "false") {
            false
        } else {
            null
        }

        if (propertyValueBoolean != null) {
            return (propertyKey to propertyValueBoolean)
        }

        return (propertyKey to propertyValue)
    }
}