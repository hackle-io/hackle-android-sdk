package io.hackle.android.internal.application

import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo

class ApplicationInstallStateTest {

    @Test
    fun `enum values should be correct`() {
        val values = ApplicationInstallState.values()

        expectThat(values.size).isEqualTo(3)
        expectThat(values[0]).isEqualTo(ApplicationInstallState.NONE)
        expectThat(values[1]).isEqualTo(ApplicationInstallState.INSTALL)
        expectThat(values[2]).isEqualTo(ApplicationInstallState.UPDATE)
    }

    @Test
    fun `valueOf should return correct enum`() {
        expectThat(ApplicationInstallState.valueOf("NONE")).isEqualTo(ApplicationInstallState.NONE)
        expectThat(ApplicationInstallState.valueOf("INSTALL")).isEqualTo(ApplicationInstallState.INSTALL)
        expectThat(ApplicationInstallState.valueOf("UPDATE")).isEqualTo(ApplicationInstallState.UPDATE)
    }

    @Test
    fun `toString should return correct string`() {
        expectThat(ApplicationInstallState.NONE.toString()).isEqualTo("NONE")
        expectThat(ApplicationInstallState.INSTALL.toString()).isEqualTo("INSTALL")
        expectThat(ApplicationInstallState.UPDATE.toString()).isEqualTo("UPDATE")
    }
}