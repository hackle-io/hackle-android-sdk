package io.hackle.android.internal.user

import io.hackle.android.internal.database.repository.KeyValueRepository
import io.hackle.android.internal.utils.json.parseJson
import io.hackle.android.internal.utils.json.toJson
import io.hackle.sdk.common.User
import io.hackle.sdk.core.internal.log.Logger

internal class UserRepository(
    private val repository: KeyValueRepository
) {
    fun get(): User? {
        return try {
            val json = repository.getString(USER_KEY) ?: return null
            val dto = json.parseJson<UserModelDto>()
            val user = dto.toUser()
            log.debug { "User loaded [$user]" }
            return user
        } catch (e: Exception) {
            log.error { "Failed to load User: $e" }
            null
        }
    }

    fun set(user: User) {
        try {
            val dto = UserModelDto.from(user)
            val json = dto.toJson()
            repository.putString(USER_KEY, json)
            log.debug { "User saved [$user]" }
        } catch (e: Exception) {
            log.error { "Failed to save User: $e" }
        }
    }

    companion object {
        private val log = Logger<UserRepository>()
        private const val USER_KEY = "user"
    }
}


private data class UserModelDto(
    val id: String?,
    val userId: String?,
    val deviceId: String?,
    val identifiers: Map<String, String>,
    val properties: Map<String, Any>,
) {

    fun toUser(): User {
        return User.builder()
            .id(id)
            .userId(userId)
            .deviceId(deviceId)
            .identifiers(identifiers)
            .properties(properties)
            .build()
    }

    companion object {
        fun from(user: User): UserModelDto {
            return UserModelDto(
                id = user.id,
                userId = user.userId,
                deviceId = user.deviceId,
                identifiers = user.identifiers,
                properties = user.properties
            )
        }
    }
}
