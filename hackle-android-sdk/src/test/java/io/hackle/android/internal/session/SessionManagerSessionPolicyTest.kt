package io.hackle.android.internal.session

import io.hackle.android.internal.database.repository.MapKeyValueRepository
import io.hackle.android.internal.platform.packageinfo.PackageVersionInfo
import io.hackle.android.internal.user.UserManager
import io.hackle.android.mock.MockDevice
import io.hackle.android.mock.MockPackageInfo
import io.hackle.sdk.common.HackleSessionPersistPolicy
import io.hackle.sdk.common.HackleSessionPersistPolicy.*
import io.hackle.sdk.common.HackleSessionPolicy
import io.hackle.sdk.common.User
import io.mockk.mockk
import org.junit.Assert.fail
import org.junit.Test

/**
 * Truth table: 16 policy combinations × 7 scenarios = 112 cases.
 *
 * Scenarios:
 *   S1: deviceId same, null → A (triggers: NULL_TO_USER_ID)
 *   S2: deviceId same, A → B (triggers: USER_ID_CHANGE)
 *   S3: deviceId same, A → null (triggers: USER_ID_TO_NULL)
 *   S4: deviceId changed, userId same (triggers: DEVICE_ID_CHANGE)
 *   S5: deviceId changed, null → A (triggers: NULL_TO_USER_ID, DEVICE_ID_CHANGE)
 *   S6: deviceId changed, A → B (triggers: USER_ID_CHANGE, DEVICE_ID_CHANGE)
 *   S7: deviceId changed, A → null (triggers: USER_ID_TO_NULL, DEVICE_ID_CHANGE)
 *
 * EXPIRED = session expires, MAINTAINED = session maintained.
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
        return SessionManager(
            userManager = UserManager(
                MockDevice("test_id", emptyMap()),
                MockPackageInfo(PackageVersionInfo("1.0.0", 1L)),
                MapKeyValueRepository(),
                mockk(),
                mockk()
            ),
            keyValueRepository = MapKeyValueRepository(),
            sessionTimeoutMillis = 10000,
            sessionPolicy = sessionPolicy,
        )
    }

    private fun policy(vararg conditions: HackleSessionPersistPolicy): HackleSessionPolicy {
        return HackleSessionPolicy.builder().persistWhen(*conditions).build()
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
        if (failures.isNotEmpty()) {
            fail("Policy $policyLabel mismatches:\n${failures.joinToString("\n")}")
        }
    }

    @Test
    fun `#1 persist {} - default, all changes expire session`() {
        verifyPolicy(policy(),
            "{}", listOf(EXPIRED, EXPIRED, EXPIRED, EXPIRED, EXPIRED, EXPIRED, EXPIRED))
    }

    @Test
    fun `#2 persist {N} - persist NULL_TO_USER_ID`() {
        verifyPolicy(policy(NULL_TO_USER_ID),
            "{N}", listOf(MAINTAINED, EXPIRED, EXPIRED, EXPIRED, EXPIRED, EXPIRED, EXPIRED))
    }

    @Test
    fun `#3 persist {U} - persist USER_ID_CHANGE`() {
        verifyPolicy(policy(USER_ID_CHANGE),
            "{U}", listOf(EXPIRED, MAINTAINED, EXPIRED, EXPIRED, EXPIRED, EXPIRED, EXPIRED))
    }

    @Test
    fun `#4 persist {T} - persist USER_ID_TO_NULL`() {
        verifyPolicy(policy(USER_ID_TO_NULL),
            "{T}", listOf(EXPIRED, EXPIRED, MAINTAINED, EXPIRED, EXPIRED, EXPIRED, EXPIRED))
    }

    @Test
    fun `#5 persist {D} - persist DEVICE_ID_CHANGE`() {
        verifyPolicy(policy(DEVICE_ID_CHANGE),
            "{D}", listOf(EXPIRED, EXPIRED, EXPIRED, MAINTAINED, EXPIRED, EXPIRED, EXPIRED))
    }

    @Test
    fun `#6 persist {N,U}`() {
        verifyPolicy(policy(NULL_TO_USER_ID, USER_ID_CHANGE),
            "{N,U}", listOf(MAINTAINED, MAINTAINED, EXPIRED, EXPIRED, EXPIRED, EXPIRED, EXPIRED))
    }

    @Test
    fun `#7 persist {N,T}`() {
        verifyPolicy(policy(NULL_TO_USER_ID, USER_ID_TO_NULL),
            "{N,T}", listOf(MAINTAINED, EXPIRED, MAINTAINED, EXPIRED, EXPIRED, EXPIRED, EXPIRED))
    }

    @Test
    fun `#8 persist {N,D}`() {
        verifyPolicy(policy(NULL_TO_USER_ID, DEVICE_ID_CHANGE),
            "{N,D}", listOf(MAINTAINED, EXPIRED, EXPIRED, MAINTAINED, MAINTAINED, EXPIRED, EXPIRED))
    }

    @Test
    fun `#9 persist {U,T}`() {
        verifyPolicy(policy(USER_ID_CHANGE, USER_ID_TO_NULL),
            "{U,T}", listOf(EXPIRED, MAINTAINED, MAINTAINED, EXPIRED, EXPIRED, EXPIRED, EXPIRED))
    }

    @Test
    fun `#10 persist {U,D}`() {
        verifyPolicy(policy(USER_ID_CHANGE, DEVICE_ID_CHANGE),
            "{U,D}", listOf(EXPIRED, MAINTAINED, EXPIRED, MAINTAINED, EXPIRED, MAINTAINED, EXPIRED))
    }

    @Test
    fun `#11 persist {T,D}`() {
        verifyPolicy(policy(USER_ID_TO_NULL, DEVICE_ID_CHANGE),
            "{T,D}", listOf(EXPIRED, EXPIRED, MAINTAINED, MAINTAINED, EXPIRED, EXPIRED, MAINTAINED))
    }

    @Test
    fun `#12 persist {N,U,T}`() {
        verifyPolicy(policy(NULL_TO_USER_ID, USER_ID_CHANGE, USER_ID_TO_NULL),
            "{N,U,T}", listOf(MAINTAINED, MAINTAINED, MAINTAINED, EXPIRED, EXPIRED, EXPIRED, EXPIRED))
    }

    @Test
    fun `#13 persist {N,U,D}`() {
        verifyPolicy(policy(NULL_TO_USER_ID, USER_ID_CHANGE, DEVICE_ID_CHANGE),
            "{N,U,D}", listOf(MAINTAINED, MAINTAINED, EXPIRED, MAINTAINED, MAINTAINED, MAINTAINED, EXPIRED))
    }

    @Test
    fun `#14 persist {N,T,D}`() {
        verifyPolicy(policy(NULL_TO_USER_ID, USER_ID_TO_NULL, DEVICE_ID_CHANGE),
            "{N,T,D}", listOf(MAINTAINED, EXPIRED, MAINTAINED, MAINTAINED, MAINTAINED, EXPIRED, MAINTAINED))
    }

    @Test
    fun `#15 persist {U,T,D}`() {
        verifyPolicy(policy(USER_ID_CHANGE, USER_ID_TO_NULL, DEVICE_ID_CHANGE),
            "{U,T,D}", listOf(EXPIRED, MAINTAINED, MAINTAINED, MAINTAINED, EXPIRED, MAINTAINED, MAINTAINED))
    }

    @Test
    fun `#16 persist {N,U,T,D} - all persist, no changes expire`() {
        verifyPolicy(policy(NULL_TO_USER_ID, USER_ID_CHANGE, USER_ID_TO_NULL, DEVICE_ID_CHANGE),
            "{N,U,T,D}", listOf(MAINTAINED, MAINTAINED, MAINTAINED, MAINTAINED, MAINTAINED, MAINTAINED, MAINTAINED))
    }
}
