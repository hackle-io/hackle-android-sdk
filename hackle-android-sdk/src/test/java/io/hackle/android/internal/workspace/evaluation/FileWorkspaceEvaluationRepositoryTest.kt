package io.hackle.android.internal.workspace.evaluation

import io.hackle.android.internal.storage.FileStorage
import io.hackle.android.mock.MockFileStorage
import io.hackle.android.support.Workspaces
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import java.io.IOException

class FileWorkspaceEvaluationRepositoryTest {

    private lateinit var fileStorage: MockFileStorage
    private lateinit var sut: FileWorkspaceEvaluationRepository

    @Before
    fun before() {
        fileStorage = MockFileStorage()
        sut = FileWorkspaceEvaluationRepository(fileStorage)
    }

    @Test
    fun `get - 저장된 파일이 없으면 빈 리스트를 리턴한다`() {
        val actual = sut.get()
        expectThat(actual).isEmpty()
    }

    @Test
    fun `get - 저장된 평가 결과를 파싱해 리턴한다`() {
        // given
        val context = Workspaces.evaluationContext(
            key = Workspaces.evaluationKey("\$id" to "user"),
            evaluation = Workspaces.evaluationDto(
                workspace = Workspaces.workspaceDto(id = 1, environmentId = 2),
                metadata = Workspaces.evaluationMetadataDto(evaluatedAt = 100, modifiedAt = "42"),
                results = listOf(Workspaces.resultDto(type = "AB_TEST", id = 1, hash = 11))
            ),
            fullEvaluatedAt = 100
        )
        sut.set(listOf(context))

        // when
        val actual = sut.get()

        // then
        expectThat(actual).hasSize(1)
        expectThat(actual[0]) {
            get { key } isEqualTo Workspaces.evaluationKey("\$id" to "user")
            get { fullEvaluatedAt } isEqualTo 100L
            get { workspace.metadata.id } isEqualTo 1L
            get { workspace.metadata.environmentId } isEqualTo 2L
            get { workspace.metadata.evaluatedAt } isEqualTo 100L
            get { workspace.metadata.modifiedAt } isEqualTo "42"
            get { dto.results.map { Triple(it.type, it.id, it.hash) } } isEqualTo listOf(Triple("AB_TEST", 1L, 11))
        }
    }

    @Test
    fun `get - 손상된 파일이면 삭제하고 빈 리스트를 리턴한다`() {
        // given
        fileStorage.writer("workspace_evaluation.json").use { it.write("invalid-json") }

        // when
        val actual = sut.get()

        // then
        expectThat(actual).isEmpty()
        expectThat(fileStorage.exists("workspace_evaluation.json")) isEqualTo false
    }

    @Test
    fun `get - 손상된 파일 삭제에도 실패하면 빈 리스트를 리턴한다`() {
        // given
        val fileStorage = mockk<FileStorage> {
            every { exists(any()) } returns true
            every { reader(any()) } returns "invalid-json".reader()
            every { delete(any()) } throws IOException("delete fail")
        }
        val sut = FileWorkspaceEvaluationRepository(fileStorage)

        // when
        val actual = sut.get()

        // then
        expectThat(actual).isEmpty()
    }

    @Test
    fun `set - 저장한 평가 결과들을 다시 로드할 수 있다`() {
        // given
        val contexts = listOf(
            Workspaces.evaluationContext(
                key = Workspaces.evaluationKey("\$id" to "user1"),
                evaluation = Workspaces.evaluationDto(
                    workspace = Workspaces.workspaceDto(id = 1, environmentId = 2),
                    metadata = Workspaces.evaluationMetadataDto(evaluatedAt = 100, modifiedAt = "42"),
                    results = listOf(Workspaces.resultDto(type = "AB_TEST", id = 1, hash = 11))
                ),
                fullEvaluatedAt = 100
            ),
            Workspaces.evaluationContext(
                key = Workspaces.evaluationKey("\$id" to "user2"),
                evaluation = Workspaces.evaluationDto(metadata = Workspaces.evaluationMetadataDto(evaluatedAt = 200)),
                fullEvaluatedAt = 200
            ),
        )

        // when
        sut.set(contexts)
        val actual = sut.get()

        // then
        expectThat(actual).hasSize(2)
        expectThat(actual[0]) {
            get { key } isEqualTo Workspaces.evaluationKey("\$id" to "user1")
            get { fullEvaluatedAt } isEqualTo 100L
            get { workspace.metadata.id } isEqualTo 1L
            get { workspace.metadata.environmentId } isEqualTo 2L
            get { workspace.metadata.evaluatedAt } isEqualTo 100L
            get { workspace.metadata.modifiedAt } isEqualTo "42"
            get { dto.results.map { Triple(it.type, it.id, it.hash) } } isEqualTo listOf(Triple("AB_TEST", 1L, 11))
        }
        expectThat(actual[1]) {
            get { key } isEqualTo Workspaces.evaluationKey("\$id" to "user2")
            get { fullEvaluatedAt } isEqualTo 200L
        }
    }

    @Test
    fun `set - 기존 저장본을 덮어쓴다`() {
        // given
        sut.set(listOf(Workspaces.evaluationContext(key = Workspaces.evaluationKey("\$id" to "user1"))))

        // when
        sut.set(listOf(Workspaces.evaluationContext(key = Workspaces.evaluationKey("\$id" to "user2"))))
        val actual = sut.get()

        // then
        expectThat(actual).hasSize(1)
        expectThat(actual[0].key) isEqualTo Workspaces.evaluationKey("\$id" to "user2")
    }

    @Test
    fun `set - 저장 중 예외가 발생해도 전파하지 않는다`() {
        // given
        val fileStorage = mockk<FileStorage> {
            every { writer(any()) } throws IOException("write fail")
        }
        val sut = FileWorkspaceEvaluationRepository(fileStorage)

        // when & then: 예외가 전파되지 않는다
        sut.set(listOf(Workspaces.evaluationContext()))
    }
}
