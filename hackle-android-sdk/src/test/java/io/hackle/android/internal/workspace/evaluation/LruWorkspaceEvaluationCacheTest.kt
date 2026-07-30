package io.hackle.android.internal.workspace.evaluation

import io.hackle.android.support.Workspaces
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.*
import java.util.Collections
import java.util.concurrent.CyclicBarrier
import kotlin.concurrent.thread

class LruWorkspaceEvaluationCacheTest {

    @Test
    fun `get - 저장되지 않은 키는 null을 리턴한다`() {
        // given
        val sut = LruWorkspaceEvaluationCache(capacity = 2)

        // when
        val actual = sut.get(Workspaces.evaluationKey("\$id" to "user"))

        // then
        expectThat(actual).isNull()
    }

    @Test
    fun `put, get - 저장한 컨텍스트를 키로 조회한다`() {
        // given
        val sut = LruWorkspaceEvaluationCache(capacity = 2)
        val context = Workspaces.evaluationContext(key = Workspaces.evaluationKey("\$id" to "user"))

        // when
        val snapshots = sut.put(context)

        // then
        expectThat(sut.get(Workspaces.evaluationKey("\$id" to "user"))) isSameInstanceAs context
        expectThat(snapshots) isEqualTo listOf(context)
    }

    @Test
    fun `put - 같은 키의 컨텍스트는 교체한다`() {
        // given
        val sut = LruWorkspaceEvaluationCache(capacity = 2)
        val key = Workspaces.evaluationKey("\$id" to "user")
        val old = Workspaces.evaluationContext(
            key = key,
            evaluation = Workspaces.evaluationDto(
                metadata = Workspaces.evaluationMetadataDto(evaluatedAt = 100)
            )
        )
        val new = Workspaces.evaluationContext(
            key = key,
            evaluation = Workspaces.evaluationDto(
                metadata = Workspaces.evaluationMetadataDto(evaluatedAt = 200)
            )
        )
        sut.put(old)

        // when
        val snapshots = sut.put(new)

        // then
        expectThat(sut.get(key)) isSameInstanceAs new
        expectThat(snapshots) isEqualTo listOf(new)
    }

    @Test
    fun `put - 이미 더 최신 평가 결과가 있으면 무시한다`() {
        // given
        val sut = LruWorkspaceEvaluationCache(capacity = 2)
        val key = Workspaces.evaluationKey("\$id" to "user")
        val newer = Workspaces.evaluationContext(
            key = key,
            evaluation = Workspaces.evaluationDto(
                metadata = Workspaces.evaluationMetadataDto(evaluatedAt = 200)
            )
        )
        val stale = Workspaces.evaluationContext(
            key = key,
            evaluation = Workspaces.evaluationDto(
                metadata = Workspaces.evaluationMetadataDto(evaluatedAt = 100)
            )
        )
        sut.put(newer)

        // when
        val snapshots = sut.put(stale)

        // then
        expectThat(sut.get(key)) isSameInstanceAs newer
        expectThat(snapshots) isEqualTo listOf(newer)
    }

    @Test
    fun `put - capacity를 초과하면 가장 오래된 항목을 제거한다`() {
        // given
        val sut = LruWorkspaceEvaluationCache(capacity = 2)
        val context1 = Workspaces.evaluationContext(key = Workspaces.evaluationKey("\$id" to "user1"))
        val context2 = Workspaces.evaluationContext(key = Workspaces.evaluationKey("\$id" to "user2"))
        val context3 = Workspaces.evaluationContext(key = Workspaces.evaluationKey("\$id" to "user3"))

        // when
        sut.put(context1)
        sut.put(context2)
        val snapshots = sut.put(context3)

        // then
        expectThat(sut.get(context1.key)).isNull()
        expectThat(sut.get(context2.key)) isSameInstanceAs context2
        expectThat(sut.get(context3.key)) isSameInstanceAs context3
        expectThat(snapshots) isEqualTo listOf(context2, context3)
    }

    @Test
    fun `put - 같은 키를 다시 저장하면 최신 사용으로 갱신되어 제거 대상에서 밀려난다`() {
        // given
        val sut = LruWorkspaceEvaluationCache(capacity = 2)
        val key1 = Workspaces.evaluationKey("\$id" to "user1")
        val context1 = Workspaces.evaluationContext(
            key = key1,
            evaluation = Workspaces.evaluationDto(
                metadata = Workspaces.evaluationMetadataDto(evaluatedAt = 100)
            )
        )
        val context2 = Workspaces.evaluationContext(
            key = Workspaces.evaluationKey("\$id" to "user2"),
            evaluation = Workspaces.evaluationDto(
                metadata = Workspaces.evaluationMetadataDto(evaluatedAt = 150)
            )
        )
        val context1Updated = Workspaces.evaluationContext(
            key = key1,
            evaluation = Workspaces.evaluationDto(
                metadata = Workspaces.evaluationMetadataDto(evaluatedAt = 200)
            )
        )
        val context3 = Workspaces.evaluationContext(
            key = Workspaces.evaluationKey("\$id" to "user3"),
            evaluation = Workspaces.evaluationDto(
                metadata = Workspaces.evaluationMetadataDto(evaluatedAt = 300)
            )
        )

        // when
        sut.put(context1)
        sut.put(context2)
        sut.put(context1Updated)
        sut.put(context3)

        // then
        expectThat(sut.get(key1)) isSameInstanceAs context1Updated
        expectThat(sut.get(context2.key)).isNull()
        expectThat(sut.get(context3.key)) isSameInstanceAs context3
    }

    @Test
    fun `latest - 비어있으면 null을 리턴한다`() {
        // given
        val sut = LruWorkspaceEvaluationCache(capacity = 2)

        // when
        val actual = sut.latest()

        // then
        expectThat(actual).isNull()
    }

    @Test
    fun `latest - 가장 최근에 저장된 컨텍스트를 리턴한다`() {
        // given
        val sut = LruWorkspaceEvaluationCache(capacity = 2)
        val context1 = Workspaces.evaluationContext(key = Workspaces.evaluationKey("\$id" to "user1"))
        val context2 = Workspaces.evaluationContext(key = Workspaces.evaluationKey("\$id" to "user2"))
        sut.put(context1)
        sut.put(context2)

        // when
        val actual = sut.latest()

        // then
        expectThat(actual) isSameInstanceAs context2
    }

    @Test
    fun `restore - 기존 항목을 비우고 최근 capacity개만 복원한다`() {
        // given
        val sut = LruWorkspaceEvaluationCache(capacity = 2)
        val existing = Workspaces.evaluationContext(key = Workspaces.evaluationKey("\$id" to "existing"))
        sut.put(existing)

        val context1 = Workspaces.evaluationContext(key = Workspaces.evaluationKey("\$id" to "user1"))
        val context2 = Workspaces.evaluationContext(key = Workspaces.evaluationKey("\$id" to "user2"))
        val context3 = Workspaces.evaluationContext(key = Workspaces.evaluationKey("\$id" to "user3"))

        // when
        sut.restore(listOf(context1, context2, context3))

        // then
        expectThat(sut.get(existing.key)).isNull()
        expectThat(sut.get(context1.key)).isNull()
        expectThat(sut.get(context2.key)) isSameInstanceAs context2
        expectThat(sut.get(context3.key)) isSameInstanceAs context3
        expectThat(sut.latest()) isSameInstanceAs context3
    }

    @Test
    fun `동시에 여러 스레드가 put_get_latest를 호출해도 상태가 깨지지 않는다`() {
        // given
        val capacity = 50
        val sut = LruWorkspaceEvaluationCache(capacity = capacity)
        val threadCount = 32
        val perThread = 200
        val barrier = CyclicBarrier(threadCount)
        val errors = Collections.synchronizedList(mutableListOf<Throwable>())
        val keys = Collections.synchronizedList(mutableListOf<WorkspaceEvaluationContext.Key>())

        // when: 서로 다른 키를 capacity보다 훨씬 많이(32 * 200 = 6400) 동시에 넣으며 읽는다
        val threads = (0 until threadCount).map { t ->
            thread {
                try {
                    barrier.await()
                    for (i in 0 until perThread) {
                        val key = Workspaces.evaluationKey("\$id" to "user-$t-$i")
                        keys.add(key)
                        sut.put(Workspaces.evaluationContext(key = key))
                        sut.get(key)
                        sut.latest()
                    }
                } catch (e: Throwable) {
                    errors.add(e)
                }
            }
        }
        threads.forEach { it.join() }

        // then
        // 1) 동기화가 깨지면 HashMap/ArrayList에서 ConcurrentModificationException 등이 발생한다
        expectThat(errors).isEmpty()
        // 2) 서로 다른 키를 capacity보다 많이 넣었으므로 정확히 capacity개만 남는다 (eviction이 원자적으로 동작)
        val survivors = keys.toSet().count { sut.get(it) != null }
        expectThat(survivors) isEqualTo capacity
        // 3) latest는 유효한 항목을 가리킨다
        expectThat(sut.latest()).isNotNull()
    }

    @Test
    fun `동시에 put과 restore가 섞여도 예외 없이 완료된다`() {
        // given
        val sut = LruWorkspaceEvaluationCache(capacity = 20)
        val threadCount = 16
        val perThread = 200
        val barrier = CyclicBarrier(threadCount)
        val errors = Collections.synchronizedList(mutableListOf<Throwable>())
        val snapshot = (0 until 20).map {
            Workspaces.evaluationContext(key = Workspaces.evaluationKey("\$id" to "restore-$it"))
        }

        // when: 절반은 put, 절반은 restore를 동시에 반복한다
        val threads = (0 until threadCount).map { t ->
            thread {
                try {
                    barrier.await()
                    for (i in 0 until perThread) {
                        if (t % 2 == 0) {
                            val key = Workspaces.evaluationKey("\$id" to "put-$t-$i")
                            sut.put(Workspaces.evaluationContext(key = key))
                        } else {
                            sut.restore(snapshot)
                            sut.latest()
                        }
                    }
                } catch (e: Throwable) {
                    errors.add(e)
                }
            }
        }
        threads.forEach { it.join() }

        // then
        expectThat(errors).isEmpty()
    }
}
