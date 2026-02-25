package io.hackle.android.internal.session

import io.hackle.android.internal.database.repository.MapKeyValueRepository
import io.hackle.android.internal.platform.packageinfo.PackageVersionInfo
import io.hackle.android.internal.user.UserManager
import io.hackle.android.mock.MockDevice
import io.hackle.android.mock.MockPackageInfo
import io.hackle.sdk.common.HackleSessionExpiry
import io.hackle.sdk.common.HackleSessionExpiry.*
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
 * O = session expires, X = session maintained.
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

    private fun policy(vararg expiries: HackleSessionExpiry): HackleSessionPolicy {
        return HackleSessionPolicy.builder().expiredPolicy(*expiries).build()
    }

    private val O = true
    private val X = false

    private fun verifyPolicy(policy: HackleSessionPolicy, policyLabel: String, expected: List<Boolean>) {
        val failures = mutableListOf<String>()
        for ((i, scenario) in scenarios.withIndex()) {
            val sut = manager(policy)
            val session1 = sut.startNewSession(scenario.oldUser, 100)
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
    fun `#1 policy {N,U,T,D} - default, all changes expire session`() {
        verifyPolicy(policy(NULL_TO_USER_ID, USER_ID_CHANGE, USER_ID_TO_NULL, DEVICE_ID_CHANGE),
            "{N,U,T,D}", listOf(O, O, O, O, O, O, O))
    }

    @Test
    fun `#2 policy {U,T,D} - exclude NULL_TO_USER_ID`() {
        verifyPolicy(policy(USER_ID_CHANGE, USER_ID_TO_NULL, DEVICE_ID_CHANGE),
            "{U,T,D}", listOf(X, O, O, O, O, O, O))
    }

    @Test
    fun `#3 policy {N,T,D} - exclude USER_ID_CHANGE`() {
        verifyPolicy(policy(NULL_TO_USER_ID, USER_ID_TO_NULL, DEVICE_ID_CHANGE),
            "{N,T,D}", listOf(O, X, O, O, O, O, O))
    }

    @Test
    fun `#4 policy {N,U,D} - exclude USER_ID_TO_NULL`() {
        verifyPolicy(policy(NULL_TO_USER_ID, USER_ID_CHANGE, DEVICE_ID_CHANGE),
            "{N,U,D}", listOf(O, O, X, O, O, O, O))
    }

    @Test
    fun `#5 policy {N,U,T} - exclude DEVICE_ID_CHANGE`() {
        verifyPolicy(policy(NULL_TO_USER_ID, USER_ID_CHANGE, USER_ID_TO_NULL),
            "{N,U,T}", listOf(O, O, O, X, O, O, O))
    }

    @Test
    fun `#6 policy {T,D} - only USER_ID_TO_NULL and DEVICE_ID_CHANGE`() {
        verifyPolicy(policy(USER_ID_TO_NULL, DEVICE_ID_CHANGE),
            "{T,D}", listOf(X, X, O, O, O, O, O))
    }

    @Test
    fun `#7 policy {U,D} - only USER_ID_CHANGE and DEVICE_ID_CHANGE`() {
        verifyPolicy(policy(USER_ID_CHANGE, DEVICE_ID_CHANGE),
            "{U,D}", listOf(X, O, X, O, O, O, O))
    }

    @Test
    fun `#8 policy {U,T} - only USER_ID_CHANGE and USER_ID_TO_NULL`() {
        verifyPolicy(policy(USER_ID_CHANGE, USER_ID_TO_NULL),
            "{U,T}", listOf(X, O, O, X, X, O, O))
    }

    @Test
    fun `#9 policy {N,D} - only NULL_TO_USER_ID and DEVICE_ID_CHANGE`() {
        verifyPolicy(policy(NULL_TO_USER_ID, DEVICE_ID_CHANGE),
            "{N,D}", listOf(O, X, X, O, O, O, O))
    }

    @Test
    fun `#10 policy {N,T} - only NULL_TO_USER_ID and USER_ID_TO_NULL`() {
        verifyPolicy(policy(NULL_TO_USER_ID, USER_ID_TO_NULL),
            "{N,T}", listOf(O, X, O, X, O, X, O))
    }

    @Test
    fun `#11 policy {N,U} - only NULL_TO_USER_ID and USER_ID_CHANGE`() {
        verifyPolicy(policy(NULL_TO_USER_ID, USER_ID_CHANGE),
            "{N,U}", listOf(O, O, X, X, O, O, X))
    }

    @Test
    fun `#12 policy {D} - only DEVICE_ID_CHANGE`() {
        verifyPolicy(policy(DEVICE_ID_CHANGE),
            "{D}", listOf(X, X, X, O, O, O, O))
    }

    @Test
    fun `#13 policy {T} - only USER_ID_TO_NULL`() {
        verifyPolicy(policy(USER_ID_TO_NULL),
            "{T}", listOf(X, X, O, X, X, X, O))
    }

    @Test
    fun `#14 policy {U} - only USER_ID_CHANGE`() {
        verifyPolicy(policy(USER_ID_CHANGE),
            "{U}", listOf(X, O, X, X, X, O, X))
    }

    @Test
    fun `#15 policy {N} - only NULL_TO_USER_ID`() {
        verifyPolicy(policy(NULL_TO_USER_ID),
            "{N}", listOf(O, X, X, X, O, X, X))
    }

    @Test
    fun `#16 policy {} - empty, no changes expire session`() {
        verifyPolicy(policy(),
            "{}", listOf(X, X, X, X, X, X, X))
    }
}
