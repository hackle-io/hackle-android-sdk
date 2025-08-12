package io.hackle.android.internal.bridge

import io.hackle.android.internal.bridge.model.EventDto
import io.hackle.android.internal.bridge.model.HackleSubscriptionOperationsDto
import io.hackle.android.internal.bridge.model.PropertyOperationsDto
import io.hackle.android.internal.bridge.model.UserDto
import io.hackle.android.internal.bridge.model.from
import io.hackle.sdk.common.Event
import io.hackle.sdk.common.User

internal typealias HackleBridgeParameters = Map<String, Any?>

/**
 * 사용자 정보를 담은 [Map] 객체를 반환합니다.
 * @return [Map] 형태의 사용자 객체 또는 `null`
 */
@Suppress("UNCHECKED_CAST")
internal fun HackleBridgeParameters.userAsMap(): Map<String, Any>? = this["user"] as? Map<String, Any>

/**
 * id를 사용하는 [User] 객체를 반환합니다.
 * 
 * id를 사용합니다
 * @return [User] 객체, 또는 `null`
 */
internal fun HackleBridgeParameters.user(): User? {
    return when (val user = this["user"]) {
        is String -> {
            User.of(user)
        }

        is Map<*, *> -> {
            val data = userAsMap()
            if (data != null) {
                User.from(UserDto.from(data))
            } else {
                null
            }
        }

        else -> {
            null
        }
    }
}

/**
 * userId를 사용하는 [User] 객체를 반환합니다.
 * @return [User] 객체, 또는 `null`
 */
internal fun HackleBridgeParameters.userWithUserId(): User? {
    return when (val user = this["user"]) {
        is String -> {
            User.builder()
                .userId(user)
                .build()
        }

        is Map<*, *> -> {
            val data = userAsMap()
            if (data != null) {
                User.from(UserDto.from(data))
            } else {
                null
            }
        }

        else -> {
            null
        }
    }
}

/**
 * 사용자 ID를 반환합니다.
 * @return 사용자 ID 또는 `null`
 */
internal fun HackleBridgeParameters.userId(): String? = this["userId"] as? String

/**
 * 기기 ID를 반환합니다.
 * @return 기기 ID 또는 `null`
 */
internal fun HackleBridgeParameters.deviceId(): String? = this["deviceId"] as? String

/**
 * 프로퍼티의 키를 반환합니다.
 * @return 프로퍼티 키 또는 `null`
 */
internal fun HackleBridgeParameters.key(): String? = this["key"] as? String

/**
 * 프로퍼티의 값을 반환합니다.
 * @return 프로퍼티 값 (`Any?`)
 */
internal fun HackleBridgeParameters.value(): Any? = this["value"]

/**
 * UserProperty 업데이트를 위한 operations 객체를 [PropertyOperationsDto] 형태로 반환합니다.
 * @return PropertyOperationsDto 객체 또는 `null`
 */
@Suppress("UNCHECKED_CAST")
internal fun HackleBridgeParameters.propertyOperationDto(): PropertyOperationsDto? =
    this["operations"] as? PropertyOperationsDto

/**
 * HackleSubscriptionOperations 업데이트를 위한 operations 객체를 [HackleSubscriptionOperationsDto] 형태로 반환합니다.
 * @return HackleSubscriptionOperationsDto 객체 또는 `null`
 */
@Suppress("UNCHECKED_CAST")
internal fun HackleBridgeParameters.hackleSubscriptionOperationDto(): HackleSubscriptionOperationsDto? =
    this["operations"] as? HackleSubscriptionOperationsDto

/**
 * 사용자의 전화번호를 반환합니다.
 * @return 전화번호 또는 `null`
 */
internal fun HackleBridgeParameters.phoneNumber(): String? = this["phoneNumber"] as? String

/**
 * 실험 키(Experiment Key)를 [Long] 타입으로 반환합니다.
 * @return 실험 키 또는 `null`
 */
internal fun HackleBridgeParameters.experimentKey(): Long? = (this["experimentKey"] as? Number)?.toLong()

/**
 * A/B 테스트의 기본 그룹(variation) 키를 반환합니다.
 * 파라미터가 없는 경우 기본값으로 빈 문자열("")을 반환합니다.
 * @return 기본 그룹 키. `null`이 아님.
 */
internal fun HackleBridgeParameters.defaultVariation(): String = this["defaultVariation"] as? String ?: "A"

/**
 * 기능 플래그 키(Feature Key)를 [Long] 타입으로 반환합니다.
 * @return 기능 플래그 키 또는 `null`
 */
internal fun HackleBridgeParameters.featureKey(): Long? = (this["featureKey"] as? Number)?.toLong()

/**
 * 트래킹할 이벤트를 반환합니다.
 * @return [Event] 객체 또는 `null`
 */
internal fun HackleBridgeParameters.event(): Event? {
   return when (val event = this["event"]) {
        is String -> {
            Event.of(event)
        }

        is Map<*, *> -> {
            val dto = EventDto.from(event)
            Event.from(dto)
        }

        else -> {
            null
        }
    }
}

/**
 * 원격 구성(Remote Config)에서 가져올 값의 타입("string", "number", "boolean")을 반환합니다.
 * @return 값의 타입 또는 `null`
 */
internal fun HackleBridgeParameters.valueType(): String? = this["valueType"] as? String

/**
 * 원격 구성(Remote Config) 조회 시 사용할 string 기본값을 반환합니다.
 * @return 기본값 (`String?`)
 */

internal fun HackleBridgeParameters.defaultStringValue(): String? = this["defaultValue"] as? String
/**
 * 원격 구성(Remote Config) 조회 시 사용할 number 기본값을 반환합니다.
 * @return 기본값 (`Number?`)
 */

internal fun HackleBridgeParameters.defaultNumberValue(): Number? = this["defaultValue"] as? Number

/**
 * 원격 구성(Remote Config) 조회 시 사용할 boolean 기본값을 반환합니다.
 * @return 기본값 (`Boolean?`)
 */
internal fun HackleBridgeParameters.defaultBooleanValue(): Boolean? = this["defaultValue"] as? Boolean

/**
 * 현재 화면의 이름을 반환합니다.
 * @return 화면 이름 또는 `null`
 */
internal fun HackleBridgeParameters.screenName(): String? = this["screenName"] as? String

/**
 * 현재 화면의 클래스 이름을 반환합니다.
 * @return 클래스 이름 또는 `null`
 */
internal fun HackleBridgeParameters.className(): String? = this["className"] as? String
