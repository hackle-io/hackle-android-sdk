package io.hackle.android.internal.session

import io.hackle.android.internal.database.repository.MapKeyValueRepository
import io.hackle.android.internal.platform.packageinfo.PackageVersionInfo
import io.hackle.android.internal.user.UserManager
import io.hackle.android.mock.MockDevice
import io.hackle.android.mock.MockPackageInfo
import io.hackle.sdk.common.HackleSessionPolicy
import io.hackle.sdk.common.HackleSessionPersistCondition
import io.hackle.sdk.common.User
import io.mockk.mockk
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEmpty

/**
 * Truth table: 16 policy combinations × 7 scenarios = 112 cases.
 *
 * Scenarios:
 *   S1: deviceId same, null → A
 *   S2: deviceId same, A → B
 *   S3: deviceId same, A → null
 *   S4: deviceId changed, userId same
 *   S5: deviceId changed, null → A
 *   S6: deviceId changed, A → B
 *   S7: deviceId changed, A → null
 *
 * EXPIRED = session expires, MAINTAINED = session maintained.
 *
 * Each persist condition flag is OR-evaluated:
 * if ANY enabled flag covers ANY detected change, the session is maintained.
 */
class SessionManagerSessionPolicyTest {

    private data class Scenario(val label: String, val oldUser: User, val newUser: User)

    private val scenarios = listOf(
        Scenario("S1(null→A)", User.builder().deviceId("d1").build(), User.builder().userId("A").deviceId("d1").build()),
        Scenario("S2(A→B)", User.builder().userId("A").deviceId("d1").build(), User.builder().userId("B").deviceId("d1").build()),
        Scenario("S3(A→null)", User.builder().userId("A").deviceId("d1").build(), User.builder().deviceId("d1").build()),
        Scenario("S4(devChange)", User.builder().deviceId("d1").build(), User.builder().deviceId("d2").build()),
        Scenario("S5(null→A+dev)", User.builder().deviceId("d1").build(), User.builder().userId("A").deviceId("d2").build()),
        Scenario("S6(A→B+dev)", User.builder().userId("A").deviceId("d1").build(), User.builder().userId("B").deviceId("d2").build()),
        Scenario("S7(A→null+dev)", User.builder().userId("A").deviceId("d1").build(), User.builder().deviceId("d2").build()),
    )

    private fun manager(sessionPolicy: HackleSessionPolicy): SessionManager {
        val policy = sessionPolicy.toBuilder()
            .timeoutMillis(10000)
            .build()
        return SessionManager(
            userManager = UserManager(
                MockDevice("test_id", emptyMap()),
                MockPackageInfo(PackageVersionInfo("1.0.0", 1L)),
                MapKeyValueRepository(),
                mockk(),
                mockk()
            ),
            keyValueRepository = MapKeyValueRepository(),
            sessionPolicy = policy,
        )
    }

    private fun policy(condition: HackleSessionPersistCondition): HackleSessionPolicy {
        return HackleSessionPolicy.builder().persistCondition(condition).build()
    }

    /**
     * Creates a persist condition that covers the specified change types.
     * Session is persisted when ANY detected change is covered by an enabled flag (OR logic).
     */
    private fun persistWhen(
        nullToUserId: Boolean = false,
        userIdChange: Boolean = false,
        userIdToNull: Boolean = false,
        deviceIdChange: Boolean = false,
    ): HackleSessionPersistCondition = HackleSessionPersistCondition { old, new ->
        val isNullToUserId = old.userId == null && new.userId != null
        val isUserIdChange = old.userId != null && new.userId != null && old.userId != new.userId
        val isUserIdToNull = old.userId != null && new.userId == null
        val isDeviceIdChange = old.deviceId != new.deviceId

        (isNullToUserId && nullToUserId) ||
            (isUserIdChange && userIdChange) ||
            (isUserIdToNull && userIdToNull) ||
            (isDeviceIdChange && deviceIdChange)
    }

    private val EXPIRED = true
    private val MAINTAINED = false

    private fun verifyPolicy(policy: HackleSessionPolicy, policyLabel: String, expected: List<Boolean>) {
        val failures = mutableListOf<String>()
        for ((i, scenario) in scenarios.withIndex()) {
            val sut = manager(policy)
            val session1 = sut.startNewSession(scenario.oldUser, scenario.oldUser, 100)
            val session2 = sut.startNewSessionIfNeeded(scenario.oldUser, scenario.newUser, 200)
            val sessionExpired = session1.id != session2.id
            if (sessionExpired != expected[i]) {
                val exp = if (expected[i]) "O (expire)" else "X (maintain)"
                val act = if (sessionExpired) "O (expired)" else "X (maintained)"
                failures.add("  ${scenario.label}: expected=$exp, actual=$act")
            }
        }
        expectThat(failures)
            .describedAs("Policy $policyLabel mismatches:\n${failures.joinToString("\n")}")
            .isEmpty()
    }

    //                                           S1       S2       S3       S4       S5       S6       S7
    @Test
    fun `#1 persist {} - default, all changes expire session`() {
        verifyPolicy(HackleSessionPolicy.DEFAULT,
            "{}", listOf(EXPIRED, EXPIRED, EXPIRED, EXPIRED, EXPIRED, EXPIRED, EXPIRED))
    }

    @Test
    fun `#2 persist {N}`() {
        verifyPolicy(policy(persistWhen(nullToUserId = true)),
            "{N}", listOf(MAINTAINED, EXPIRED, EXPIRED, EXPIRED, MAINTAINED, EXPIRED, EXPIRED))
    }

    @Test
    fun `#3 persist {U}`() {
        verifyPolicy(policy(persistWhen(userIdChange = true)),
            "{U}", listOf(EXPIRED, MAINTAINED, EXPIRED, EXPIRED, EXPIRED, MAINTAINED, EXPIRED))
    }

    @Test
    fun `#4 persist {T}`() {
        verifyPolicy(policy(persistWhen(userIdToNull = true)),
            "{T}", listOf(EXPIRED, EXPIRED, MAINTAINED, EXPIRED, EXPIRED, EXPIRED, MAINTAINED))
    }

    @Test
    fun `#5 persist {D}`() {
        verifyPolicy(policy(persistWhen(deviceIdChange = true)),
            "{D}", listOf(EXPIRED, EXPIRED, EXPIRED, MAINTAINED, MAINTAINED, MAINTAINED, MAINTAINED))
    }

    @Test
    fun `#6 persist {N,U}`() {
        verifyPolicy(policy(persistWhen(nullToUserId = true, userIdChange = true)),
            "{N,U}", listOf(MAINTAINED, MAINTAINED, EXPIRED, EXPIRED, MAINTAINED, MAINTAINED, EXPIRED))
    }

    @Test
    fun `#7 persist {N,T}`() {
        verifyPolicy(policy(persistWhen(nullToUserId = true, userIdToNull = true)),
            "{N,T}", listOf(MAINTAINED, EXPIRED, MAINTAINED, EXPIRED, MAINTAINED, EXPIRED, MAINTAINED))
    }

    @Test
    fun `#8 persist {N,D}`() {
        verifyPolicy(policy(persistWhen(nullToUserId = true, deviceIdChange = true)),
            "{N,D}", listOf(MAINTAINED, EXPIRED, EXPIRED, MAINTAINED, MAINTAINED, MAINTAINED, MAINTAINED))
    }

    @Test
    fun `#9 persist {U,T}`() {
        verifyPolicy(policy(persistWhen(userIdChange = true, userIdToNull = true)),
            "{U,T}", listOf(EXPIRED, MAINTAINED, MAINTAINED, EXPIRED, EXPIRED, MAINTAINED, MAINTAINED))
    }

    @Test
    fun `#10 persist {U,D}`() {
        verifyPolicy(policy(persistWhen(userIdChange = true, deviceIdChange = true)),
            "{U,D}", listOf(EXPIRED, MAINTAINED, EXPIRED, MAINTAINED, MAINTAINED, MAINTAINED, MAINTAINED))
    }

    @Test
    fun `#11 persist {T,D}`() {
        verifyPolicy(policy(persistWhen(userIdToNull = true, deviceIdChange = true)),
            "{T,D}", listOf(EXPIRED, EXPIRED, MAINTAINED, MAINTAINED, MAINTAINED, MAINTAINED, MAINTAINED))
    }

    @Test
    fun `#12 persist {N,U,T}`() {
        verifyPolicy(policy(persistWhen(nullToUserId = true, userIdChange = true, userIdToNull = true)),
            "{N,U,T}", listOf(MAINTAINED, MAINTAINED, MAINTAINED, EXPIRED, MAINTAINED, MAINTAINED, MAINTAINED))
    }

    @Test
    fun `#13 persist {N,U,D}`() {
        verifyPolicy(policy(persistWhen(nullToUserId = true, userIdChange = true, deviceIdChange = true)),
            "{N,U,D}", listOf(MAINTAINED, MAINTAINED, EXPIRED, MAINTAINED, MAINTAINED, MAINTAINED, MAINTAINED))
    }

    @Test
    fun `#14 persist {N,T,D}`() {
        verifyPolicy(policy(persistWhen(nullToUserId = true, userIdToNull = true, deviceIdChange = true)),
            "{N,T,D}", listOf(MAINTAINED, EXPIRED, MAINTAINED, MAINTAINED, MAINTAINED, MAINTAINED, MAINTAINED))
    }

    @Test
    fun `#15 persist {U,T,D}`() {
        verifyPolicy(policy(persistWhen(userIdChange = true, userIdToNull = true, deviceIdChange = true)),
            "{U,T,D}", listOf(EXPIRED, MAINTAINED, MAINTAINED, MAINTAINED, MAINTAINED, MAINTAINED, MAINTAINED))
    }

    @Test
    fun `#16 persist {N,U,T,D} - all persist, no changes expire`() {
        verifyPolicy(policy(persistWhen(nullToUserId = true, userIdChange = true, userIdToNull = true, deviceIdChange = true)),
            "{N,U,T,D}", listOf(MAINTAINED, MAINTAINED, MAINTAINED, MAINTAINED, MAINTAINED, MAINTAINED, MAINTAINED))
    }

    @Test
    fun `#17 NULL_TO_USER_ID companion constant`() {
        verifyPolicy(policy(HackleSessionPersistCondition.NULL_TO_USER_ID),
            "NULL_TO_USER_ID", listOf(MAINTAINED, EXPIRED, EXPIRED, EXPIRED, MAINTAINED, EXPIRED, EXPIRED))
    }
}
