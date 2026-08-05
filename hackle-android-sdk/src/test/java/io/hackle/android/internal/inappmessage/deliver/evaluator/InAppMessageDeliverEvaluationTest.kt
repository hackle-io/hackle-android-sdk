package io.hackle.android.internal.inappmessage.deliver.evaluator

import io.hackle.android.support.Experiments
import io.hackle.android.support.InAppMessages
import io.hackle.sdk.common.decision.DecisionReason
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class InAppMessageDeliverEvaluationTest {

    @Test
    fun `toProperties - layout에 experiment 평가가 없으면 빈 맵을 반환한다`() {
        // given
        val evaluation = InAppMessageDeliverEvaluation(
            eligibility = InAppMessages.eligibilityResponse(),
            layout = InAppMessages.layoutResponse(experiment = null),
        )

        // when
        val actual = evaluation.toProperties()

        // then
        expectThat(actual) isEqualTo emptyMap()
    }

    @Test
    fun `toProperties - layout에 experiment 평가가 있으면 실험 정보를 반환한다`() {
        // given
        val evaluation = InAppMessageDeliverEvaluation(
            eligibility = InAppMessages.eligibilityResponse(),
            layout = InAppMessages.layoutResponse(
                experiment = Experiments.evaluation(
                    entity = Experiments.config(id = 42, key = 320),
                    result = Experiments.result(
                        reason = DecisionReason.TRAFFIC_ALLOCATED,
                        variation = Experiments.variation(id = 1001, key = "B")
                    )
                )
            ),
        )

        // when
        val actual = evaluation.toProperties()

        // then
        expectThat(actual) isEqualTo mapOf(
            "experiment_id" to 42L,
            "experiment_key" to 320L,
            "variation_id" to 1001L,
            "variation_key" to "B",
            "experiment_decision_reason" to "TRAFFIC_ALLOCATED",
        )
    }
}
