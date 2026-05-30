package io.hackle.android.internal.utils.concurrent

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * 재진입 가능한 락을 제공하는 재사용 유틸.
 *
 * 같은 스레드가 락을 잡은 채 락으로 보호된 다른 메서드를 다시 호출하는(재진입) 컴포넌트에서 사용한다.
 * 단순 상호배제만 필요한 경우에는 `synchronized` 를 사용한다.
 */
internal class ReentrantLocker {
    private val lock = ReentrantLock()
    fun <T> withLock(action: () -> T): T = lock.withLock(action)
}
