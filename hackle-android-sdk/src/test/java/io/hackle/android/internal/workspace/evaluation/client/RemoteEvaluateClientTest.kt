package io.hackle.android.internal.workspace.evaluation.client

import io.hackle.android.internal.utils.json.toJson
import io.hackle.android.internal.workspace.evaluation.evaluator.full.FullWorkspaceEvaluateRequest
import io.hackle.android.internal.workspace.evaluation.evaluator.full.toDto
import io.hackle.android.internal.workspace.evaluation.model.EntityEvaluateRequestDto
import io.hackle.android.internal.workspace.evaluation.model.RemoteEvaluateContext
import io.hackle.android.internal.workspace.evaluation.model.WorkspaceEvaluateRequestDto
import io.hackle.android.support.Workspaces
import io.hackle.android.support.assertThrows
import io.hackle.sdk.core.user.HackleUser
import io.hackle.sdk.core.user.IdentifierType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Before
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.*
import java.util.concurrent.ExecutionException

class RemoteEvaluateClientTest {

    private lateinit var httpClient: OkHttpClient
    private lateinit var sut: RemoteEvaluateClient

    @Before
    fun before() {
        httpClient = mockk()
        sut = RemoteEvaluateClient(
            sdkUri = "http://localhost",
            executor = { it.run() },
            httpClient = httpClient
        )
    }

    private fun workspaceEvaluateRequestDto(): WorkspaceEvaluateRequestDto {
        val request = FullWorkspaceEvaluateRequest.of(
            context = RemoteEvaluateContext.of(HackleUser.builder().identifier(IdentifierType.ID, "user").build()),
            base = null
        )
        return request.toDto()
    }

    @Test
    fun `evaluateIfModified - 요청을 workspace-evaluate 엔드포인트로 POST한다`() {
        // given
        every { httpClient.newCall(any()) } returns mockCall(204)

        // when
        sut.evaluateIfModified(workspaceEvaluateRequestDto()).get()

        // then
        verify(exactly = 1) {
            httpClient.newCall(withArg {
                expectThat(it.method) isEqualTo "POST"
                expectThat(it.url.toString()) isEqualTo "http://localhost/api/v1/workspace-evaluate"
                val body = Buffer().also { buffer -> it.body!!.writeTo(buffer) }.readUtf8()
                expectThat(body).contains("\"policy\":\"FORCE_FULL\"")
            })
        }
    }

    @Test
    fun `evaluateIfModified - 변경되지 않았으면(204) null을 리턴한다`() {
        // given
        every { httpClient.newCall(any()) } returns mockCall(204)

        // when
        val actual = sut.evaluateIfModified(workspaceEvaluateRequestDto()).get()

        // then
        expectThat(actual).isNull()
    }

    @Test
    fun `evaluateIfModified - 성공 응답을 파싱한다`() {
        // given
        val responseDto = mapOf(
            "status" to "FULL",
            "full" to Workspaces.evaluationDto(
                workspace = Workspaces.workspaceDto(id = 1, environmentId = 2),
                metadata = Workspaces.evaluationMetadataDto(evaluatedAt = 100),
                results = listOf(Workspaces.resultDto(type = "AB_TEST", id = 1, hash = 11))
            )
        )
        every { httpClient.newCall(any()) } returns mockCall(200, responseDto.toJson())

        // when
        val actual = sut.evaluateIfModified(workspaceEvaluateRequestDto()).get()

        // then
        expectThat(actual).isNotNull().and {
            get { status } isEqualTo "FULL"
            get { full }.isNotNull().and {
                get { workspace.id } isEqualTo 1L
                get { metadata.evaluatedAt } isEqualTo 100L
                get { results.map { Triple(it.type, it.id, it.hash) } } isEqualTo listOf(Triple("AB_TEST", 1L, 11))
            }
            get { delta }.isNull()
        }
    }

    @Test
    fun `evaluateIfModified - 실패 응답이면 예외로 완료된다`() {
        // given
        every { httpClient.newCall(any()) } returns mockCall(500)

        // when
        val exception = assertThrows<ExecutionException> {
            sut.evaluateIfModified(workspaceEvaluateRequestDto()).get()
        }

        // then
        expectThat(exception.cause).isA<IllegalStateException>()
            .get { message } isEqualTo "Http status code: 500"
    }

    @Test
    fun `evaluateEntities - 요청을 entity-evaluate 엔드포인트로 POST한다`() {
        // given
        val responseDto = mapOf("evaluation" to Workspaces.entityEvaluationDto())
        every { httpClient.newCall(any()) } returns mockCall(200, responseDto.toJson())
        val requestDto = EntityEvaluateRequestDto(
            context = workspaceEvaluateRequestDto().context,
            entities = listOf(Workspaces.entityDto(type = "IN_APP_MESSAGE", id = 320))
        )

        // when
        sut.evaluateEntities(requestDto).get()

        // then
        verify(exactly = 1) {
            httpClient.newCall(withArg {
                expectThat(it.method) isEqualTo "POST"
                expectThat(it.url.toString()) isEqualTo "http://localhost/api/v1/entity-evaluate"
            })
        }
    }

    @Test
    fun `evaluateEntities - 성공 응답을 파싱한다`() {
        // given
        val responseDto = mapOf(
            "evaluation" to Workspaces.entityEvaluationDto(
                workspace = Workspaces.workspaceDto(id = 1, environmentId = 2),
                metadata = Workspaces.entityMetadataDto(evaluatedAt = 100, modifiedAt = "42")
            )
        )
        every { httpClient.newCall(any()) } returns mockCall(200, responseDto.toJson())
        val requestDto = EntityEvaluateRequestDto(
            context = workspaceEvaluateRequestDto().context,
            entities = emptyList()
        )

        // when
        val actual = sut.evaluateEntities(requestDto).get()

        // then
        expectThat(actual.evaluation) {
            get { workspace.id } isEqualTo 1L
            get { metadata.evaluatedAt } isEqualTo 100L
            get { metadata.config.modifiedAt } isEqualTo "42"
        }
    }

    @Test
    fun `evaluateEntities - 실패 응답이면 예외로 완료된다`() {
        // given
        every { httpClient.newCall(any()) } returns mockCall(500)
        val requestDto = EntityEvaluateRequestDto(
            context = workspaceEvaluateRequestDto().context,
            entities = emptyList()
        )

        // when
        val exception = assertThrows<ExecutionException> {
            sut.evaluateEntities(requestDto).get()
        }

        // then
        expectThat(exception.cause).isA<IllegalStateException>()
            .get { message } isEqualTo "Http status code: 500"
    }

    private fun mockCall(statusCode: Int, body: String? = null): Call {
        val response = Response.Builder()
            .request(mockk())
            .protocol(mockk())
            .code(statusCode)
            .networkResponse(
                Response.Builder()
                    .request(mockk())
                    .protocol(mockk())
                    .code(statusCode)
                    .message(statusCode.toString())
                    .build()
            )
            .body((body ?: "").toResponseBody(null))
            .message(statusCode.toString())
            .build()
        return mockk {
            every { execute() } returns response
        }
    }
}
