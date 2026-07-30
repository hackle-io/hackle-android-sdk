package io.hackle.android.internal.workspace.evaluation.evaluator

import io.hackle.android.support.Workspaces
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.nio.file.Files
import java.nio.file.Paths

class WorkspaceEvaluationHashTest {
    @Test
    fun `hash - external-sdk의 EvaluateResultHasher와 동일한 해시를 만든다`() {
        val path = Paths.get("src/test/resources/evaluate_results_hash.csv")
        for (line in Files.readAllLines(path)) {
            val values = line.split(",").map { it.toInt() }
            val expected = values[0]
            val results = values.drop(1)
                .mapIndexed { index, hash ->
                    Workspaces.resultDto(id = index.toLong(), hash = hash)
                }

            expectThat(WorkspaceEvaluations.hash(results)) isEqualTo expected
        }
    }
}
