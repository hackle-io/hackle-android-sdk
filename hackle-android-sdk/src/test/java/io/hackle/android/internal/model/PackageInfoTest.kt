package io.hackle.android.internal.model

import io.hackle.android.internal.platform.packageinfo.PackageVersionInfo
import io.hackle.android.internal.platform.packageinfo.PackageInfoImpl
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class PackageInfoTest {

    @Test
    fun `PackageInfoImpl properties should return correct map`() {
        // given
        val packageVersionInfo = PackageVersionInfo("1.2.3", 123L)
        val packageInfo = PackageInfoImpl(
            packageName = "com.example.app",
            packageVersion = packageVersionInfo
        )

        // when
        val properties = packageInfo.properties

        // then
        expectThat(properties["packageName"]).isEqualTo("com.example.app")
        expectThat(properties["versionName"]).isEqualTo("1.2.3")
        expectThat(properties["versionCode"]).isEqualTo(123L)
    }

    @Test
    fun `PackageInfoImpl packageVersionInfo should return correct version info`() {
        // given
        val packageVersionInfo = PackageVersionInfo("1.2.3", 123L)
        val packageInfo = PackageInfoImpl(
            packageName = "com.example.app",
            packageVersion = packageVersionInfo
        )

        // when & then
        expectThat(packageInfo.packageVersion.versionName).isEqualTo("1.2.3")
        expectThat(packageInfo.packageVersion.versionCode).isEqualTo(123L)
    }
}
