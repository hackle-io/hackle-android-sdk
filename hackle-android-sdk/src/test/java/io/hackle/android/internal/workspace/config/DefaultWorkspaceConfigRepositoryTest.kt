package io.hackle.android.internal.workspace.config

import io.hackle.android.internal.utils.json.parseJson
import io.hackle.android.mock.MockFileStorage
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import java.io.File

class DefaultWorkspaceConfigRepositoryTest {

    private lateinit var fileStorage: MockFileStorage
    private lateinit var sut: DefaultWorkspaceConfigRepository

    @Before
    fun before() {
        fileStorage = MockFileStorage()
        sut = DefaultWorkspaceConfigRepository(fileStorage)
    }

    @Test
    fun `get - 저장된 파일이 없으면 null을 리턴한다`() {
        expectThat(sut.get()).isNull()
    }

    @Test
    fun `get - 저장된 workspace config를 파싱해 리턴한다`() {
        // given
        fileStorage.createMockFile(
            resFilePath = "workspace_config.json",
            dstFilePath = "workspace.json"
        )

        // when
        val actual = sut.get()

        // then
        expectThat(actual).isNotNull().and {
            get { modifiedAt } isEqualTo "Tue, 16 Jan 2024 07:39:44 GMT"
            get { workspace.metadata.id } isEqualTo 7356L
            get { workspace.metadata.environmentId } isEqualTo 112712L
        }
    }

    @Test
    fun `get - 파싱할 수 없는 파일이면 삭제하고 null을 리턴한다`() {
        // given
        fileStorage.writer("workspace.json").use { it.write("invalid-json") }

        // when
        val actual = sut.get()

        // then
        expectThat(actual).isNull()
        expectThat(fileStorage.exists("workspace.json")) isEqualTo false
    }

    @Test
    fun `set - 저장한 workspace config를 다시 로드할 수 있다`() {
        // given
        val context = createWorkspaceConfigContext("workspace_config.json")

        // when
        sut.set(context)

        // then
        expectThat(sut.get()).isNotNull().and {
            get { modifiedAt } isEqualTo "Tue, 16 Jan 2024 07:39:44 GMT"
            get { workspace.metadata.id } isEqualTo 7356L
            get { workspace.metadata.environmentId } isEqualTo 112712L
        }
    }

    @Test
    fun `set - 기존 저장본을 덮어쓴다`() {
        // given
        fileStorage.createMockFile(
            resFilePath = "workspace_config.json",
            dstFilePath = "workspace.json"
        )
        expectThat(sut.get()).isNotNull()
            .get { modifiedAt } isEqualTo "Tue, 16 Jan 2024 07:39:44 GMT"

        // when
        sut.set(createWorkspaceConfigContext("workspace_config_modified.json"))

        // then
        expectThat(sut.get()).isNotNull()
            .get { modifiedAt } isEqualTo "Mon, 22 Jan 2024 08:37:33 GMT"
    }

    private fun createWorkspaceConfigContext(filename: String): WorkspaceConfigContext {
        val url = javaClass.classLoader!!.getResource(filename)
        val dto = File(url.path).readText().parseJson<WorkspaceConfigRecordDto>()
        return WorkspaceConfigContext.from(dto)
    }
}
