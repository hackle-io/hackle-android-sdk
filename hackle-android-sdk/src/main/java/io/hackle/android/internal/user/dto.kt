package io.hackle.android.internal.user

import android.util.Base64
import android.util.Base64.NO_WRAP
import android.util.Base64.URL_SAFE
import io.hackle.android.internal.utils.json.toJson
import kotlin.text.Charsets.UTF_8

internal class UserCohortsRequestDto(
    val identifiers: Map<String, String>
)

internal fun UserCohortsRequestDto.encodeBase64Url(): String {
    return Base64.encodeToString(toJson().toByteArray(UTF_8), URL_SAFE or NO_WRAP)
}

internal class UserCohortsResponseDto(
    val cohorts: List<UserCohortDto>
)

internal class UserCohortDto(
    val identifier: IdentifierDto,
    val cohorts: List<Long>
)

internal class IdentifierDto(
    val type: String,
    val value: String
)

internal class UserTargetRequestDto(
    val identifiers: Map<String, String>
)

internal fun UserTargetRequestDto.encodeBase64Url(): String {
    return Base64.encodeToString(toJson().toByteArray(UTF_8), URL_SAFE or NO_WRAP)
}

internal class UserTargetResponseDto(
    val events: List<TargetEventDto>
)

internal class TargetEventDto(
    val eventKey: String,
    val stats: List<TargetEventStatDto>,
    val property: TargetEventPropertyDto? = null
)

internal class TargetEventStatDto(
    val date: Long,
    val count: Int
)

internal class TargetEventPropertyDto(
    val key: String,
    val type: String,
    val value: Any
)
