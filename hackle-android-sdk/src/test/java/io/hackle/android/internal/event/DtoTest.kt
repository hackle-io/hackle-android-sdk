package io.hackle.android.internal.event

import io.hackle.android.internal.database.workspace.EventEntity
import io.hackle.sdk.common.Event
import io.hackle.sdk.common.decision.DecisionReason
import io.hackle.sdk.core.event.UserEvent
import io.hackle.sdk.core.model.*
import io.hackle.sdk.core.user.HackleUser
import io.hackle.sdk.core.user.IdentifierType
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

internal class DtoTest {

    @Test
    fun `toBody - 빈 리스트는 빈 JSON 배열을 반환한다`() {
        val emptyList = emptyList<EventEntity>()
        val result = emptyList.toBody()

        expectThat(result).isEqualTo("{\"exposureEvents\":[],\"trackEvents\":[],\"remoteConfigEvents\":[]}")
    }

    @Test
    fun `toBody - EXPOSURE 타입만 있는 경우`() {
        val exposureEvent = EventEntity(
            id = 1L,
            status = EventEntity.Status.PENDING,
            type = EventEntity.Type.EXPOSURE,
            body = "{\"exposureData\":\"test\"}"
        )
        val events = listOf(exposureEvent)
        val result = events.toBody()

        expectThat(result).isEqualTo("{\"exposureEvents\":[{\"exposureData\":\"test\"}],\"trackEvents\":[],\"remoteConfigEvents\":[]}")
    }

    @Test
    fun `toBody - TRACK 타입만 있는 경우`() {
        val trackEvent = EventEntity(
            id = 1L,
            status = EventEntity.Status.PENDING,
            type = EventEntity.Type.TRACK,
            body = "{\"trackData\":\"test\"}"
        )
        val events = listOf(trackEvent)
        val result = events.toBody()

        expectThat(result).isEqualTo("{\"exposureEvents\":[],\"trackEvents\":[{\"trackData\":\"test\"}],\"remoteConfigEvents\":[]}")
    }

    @Test
    fun `toBody - REMOTE_CONFIG 타입만 있는 경우`() {
        val remoteConfigEvent = EventEntity(
            id = 1L,
            status = EventEntity.Status.PENDING,
            type = EventEntity.Type.REMOTE_CONFIG,
            body = "{\"remoteConfigData\":\"test\"}"
        )
        val events = listOf(remoteConfigEvent)
        val result = events.toBody()

        expectThat(result).isEqualTo("{\"exposureEvents\":[],\"trackEvents\":[],\"remoteConfigEvents\":[{\"remoteConfigData\":\"test\"}]}")
    }

    @Test
    fun `toBody - 모든 타입이 혼합된 경우`() {
        val exposureEvent = EventEntity(
            id = 1L,
            status = EventEntity.Status.PENDING,
            type = EventEntity.Type.EXPOSURE,
            body = "{\"exposureData\":\"test1\"}"
        )
        val trackEvent = EventEntity(
            id = 2L,
            status = EventEntity.Status.PENDING,
            type = EventEntity.Type.TRACK,
            body = "{\"trackData\":\"test2\"}"
        )
        val remoteConfigEvent = EventEntity(
            id = 3L,
            status = EventEntity.Status.PENDING,
            type = EventEntity.Type.REMOTE_CONFIG,
            body = "{\"remoteConfigData\":\"test3\"}"
        )
        val events = listOf(exposureEvent, trackEvent, remoteConfigEvent)
        val result = events.toBody()

        expectThat(result).isEqualTo("{\"exposureEvents\":[{\"exposureData\":\"test1\"}],\"trackEvents\":[{\"trackData\":\"test2\"}],\"remoteConfigEvents\":[{\"remoteConfigData\":\"test3\"}]}")
    }

    @Test
    fun `toBody - 동일 타입 여러 이벤트 처리`() {
        val event1 = EventEntity(
            id = 1L,
            status = EventEntity.Status.PENDING,
            type = EventEntity.Type.EXPOSURE,
            body = "{\"data\":\"test1\"}"
        )
        val event2 = EventEntity(
            id = 2L,
            status = EventEntity.Status.PENDING,
            type = EventEntity.Type.EXPOSURE,
            body = "{\"data\":\"test2\"}"
        )
        val events = listOf(event1, event2)
        val result = events.toBody()

        expectThat(result).isEqualTo("{\"exposureEvents\":[{\"data\":\"test1\"},{\"data\":\"test2\"}],\"trackEvents\":[],\"remoteConfigEvents\":[]}")
    }

    @Test
    fun `Exposure toDto - 모든 필드가 올바르게 매핑된다`() {
        val user = HackleUser.builder()
            .identifier(IdentifierType.ID, "testUserId")
            .identifier(IdentifierType.DEVICE, "deviceId")
            .property("userProp", "userValue")
            .hackleProperty("hackleProp", "hackleValue")
            .build()

        val experiment = Experiment(
            id = 123L,
            key = 456L,
            name = "testExperiment",
            type = Experiment.Type.AB_TEST,
            identifierType = "\$id",
            status = Experiment.Status.RUNNING,
            version = 2,
            executionVersion = 1,
            variations = emptyList(),
            userOverrides = emptyMap(),
            segmentOverrides = emptyList(),
            targetAudiences = emptyList(),
            targetRules = emptyList(),
            defaultRule = Action.Variation(0L),
            winnerVariationId = null,
            containerId = null
        )

        val exposureEvent = UserEvent.Exposure(
            insertId = "insert123",
            timestamp = 1000L,
            user = user,
            experiment = experiment,
            variationId = 789L,
            variationKey = "variationA",
            decisionReason = DecisionReason.TRAFFIC_ALLOCATED,
            properties = mapOf("eventProp" to "eventValue")
        )

        val dto = exposureEvent.toDto()

        expectThat(dto.insertId).isEqualTo("insert123")
        expectThat(dto.timestamp).isEqualTo(1000L)
        expectThat(dto.userId).isEqualTo("testUserId")
        expectThat(dto.identifiers["\$id"]).isEqualTo("testUserId")
        expectThat(dto.identifiers["\$deviceId"]).isEqualTo("deviceId")
        expectThat(dto.userProperties["userProp"]).isEqualTo("userValue")
        expectThat(dto.hackleProperties["hackleProp"]).isEqualTo("hackleValue")
        expectThat(dto.experimentId).isEqualTo(123L)
        expectThat(dto.experimentKey).isEqualTo(456L)
        expectThat(dto.experimentType).isEqualTo("AB_TEST")
        expectThat(dto.experimentVersion).isEqualTo(2)
        expectThat(dto.variationId).isEqualTo(789L)
        expectThat(dto.variationKey).isEqualTo("variationA")
        expectThat(dto.decisionReason).isEqualTo("TRAFFIC_ALLOCATED")
        expectThat(dto.properties["eventProp"]).isEqualTo("eventValue")
    }

    @Test
    fun `Exposure toDto - userId가 null인 경우`() {
        val user = HackleUser.builder()
            .identifier(IdentifierType.DEVICE, "deviceId")
            .build()

        val experiment = Experiment(
            id = 123L,
            key = 456L,
            name = "testExperiment",
            type = Experiment.Type.AB_TEST,
            identifierType = "\$id",
            status = Experiment.Status.RUNNING,
            version = 1,
            executionVersion = 1,
            variations = emptyList(),
            userOverrides = emptyMap(),
            segmentOverrides = emptyList(),
            targetAudiences = emptyList(),
            targetRules = emptyList(),
            defaultRule = Action.Variation(0L),
            winnerVariationId = null,
            containerId = null
        )

        val exposureEvent = UserEvent.Exposure(
            insertId = "insert123",
            timestamp = 1000L,
            user = user,
            experiment = experiment,
            variationId = null,
            variationKey = "control",
            decisionReason = DecisionReason.NOT_IN_MUTUAL_EXCLUSION_EXPERIMENT,
            properties = emptyMap()
        )

        val dto = exposureEvent.toDto()

        expectThat(dto.userId).isEqualTo(null)
        expectThat(dto.variationId).isEqualTo(null)
    }

    @Test
    fun `Track toDto - 모든 필드가 올바르게 매핑된다`() {
        val user = HackleUser.builder()
            .identifier(IdentifierType.ID, "testUserId")
            .property("userProp", "userValue")
            .hackleProperty("hackleProp", "hackleValue")
            .build()

        val eventType = EventType.Custom(1L, "purchase")
        val event = Event.builder("purchase")
            .value(99.99)
            .property("productId", "product123")
            .build()

        val trackEvent = UserEvent.Track(
            insertId = "track123",
            timestamp = 2000L,
            user = user,
            eventType = eventType,
            event = event
        )

        val dto = trackEvent.toDto()

        expectThat(dto.insertId).isEqualTo("track123")
        expectThat(dto.timestamp).isEqualTo(2000L)
        expectThat(dto.userId).isEqualTo("testUserId")
        expectThat(dto.identifiers["\$id"]).isEqualTo("testUserId")
        expectThat(dto.userProperties["userProp"]).isEqualTo("userValue")
        expectThat(dto.hackleProperties["hackleProp"]).isEqualTo("hackleValue")
        expectThat(dto.eventTypeId).isEqualTo(1L)
        expectThat(dto.eventTypeKey).isEqualTo("purchase")
        expectThat(dto.value).isEqualTo(99.99)
        expectThat(dto.properties["productId"]).isEqualTo("product123")
    }

    @Test
    fun `Track toDto - value가 null인 경우`() {
        val user = HackleUser.builder()
            .identifier(IdentifierType.ID, "testUserId")
            .build()

        val eventType = EventType.Custom(1L, "click")
        val event = Event.builder("click").build()

        val trackEvent = UserEvent.Track(
            insertId = "track123",
            timestamp = 2000L,
            user = user,
            eventType = eventType,
            event = event
        )

        val dto = trackEvent.toDto()

        expectThat(dto.value).isEqualTo(null)
    }

    @Test
    fun `RemoteConfig toDto - 모든 필드가 올바르게 매핑된다`() {
        val user = HackleUser.builder()
            .identifier(IdentifierType.ID, "testUserId")
            .property("userProp", "userValue")
            .hackleProperty("hackleProp", "hackleValue")
            .build()

        val parameter = RemoteConfigParameter(
            id = 456L,
            key = "testParam",
            type = ValueType.STRING,
            identifierType = "\$id",
            targetRules = emptyList(),
            defaultValue = RemoteConfigParameter.Value(1L, "default")
        )

        val remoteConfigEvent = UserEvent.RemoteConfig(
            insertId = "config123",
            timestamp = 3000L,
            user = user,
            parameter = parameter,
            valueId = 789L,
            decisionReason = DecisionReason.DEFAULT_RULE,
            properties = mapOf("configProp" to "configValue")
        )

        val dto = remoteConfigEvent.toDto()

        expectThat(dto.insertId).isEqualTo("config123")
        expectThat(dto.timestamp).isEqualTo(3000L)
        expectThat(dto.userId).isEqualTo("testUserId")
        expectThat(dto.identifiers["\$id"]).isEqualTo("testUserId")
        expectThat(dto.userProperties["userProp"]).isEqualTo("userValue")
        expectThat(dto.hackleProperties["hackleProp"]).isEqualTo("hackleValue")
        expectThat(dto.parameterId).isEqualTo(456L)
        expectThat(dto.parameterKey).isEqualTo("testParam")
        expectThat(dto.parameterType).isEqualTo("STRING")
        expectThat(dto.valueId).isEqualTo(789L)
        expectThat(dto.decisionReason).isEqualTo("DEFAULT_RULE")
        expectThat(dto.properties["configProp"]).isEqualTo("configValue")
    }

    @Test
    fun `RemoteConfig toDto - valueId가 null인 경우`() {
        val user = HackleUser.builder()
            .identifier(IdentifierType.ID, "testUserId")
            .build()

        val parameter = RemoteConfigParameter(
            id = 456L,
            key = "testParam",
            type = ValueType.BOOLEAN,
            identifierType = "\$id",
            targetRules = emptyList(),
            defaultValue = RemoteConfigParameter.Value(1L, false)
        )

        val remoteConfigEvent = UserEvent.RemoteConfig(
            insertId = "config123",
            timestamp = 3000L,
            user = user,
            parameter = parameter,
            valueId = null,
            decisionReason = DecisionReason.EXCEPTION,
            properties = emptyMap()
        )

        val dto = remoteConfigEvent.toDto()

        expectThat(dto.valueId).isEqualTo(null)
    }
}