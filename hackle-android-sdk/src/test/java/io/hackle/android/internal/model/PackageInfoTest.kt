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
        val currentVersion = PackageVersionInfo("1.2.3", 123L)
        val previousVersion = PackageVersionInfo("1.0.0", 100L)
        val packageInfo = PackageInfoImpl(
            packageName = "com.example.app",
            currentPackageVersionInfo = currentVersion,
            previousPackageVersionInfo = previousVersion
        )

        // when
        val properties = packageInfo.properties

        // then
        expectThat(properties["packageName"]).isEqualTo("com.example.app")
        expectThat(properties["versionName"]).isEqualTo("1.2.3")
        expectThat(properties["versionCode"]).isEqualTo(123L)
    }

    @Test
    fun `PackageInfoImpl properties should not include previousVersion`() {
        // given
        val currentVersion = PackageVersionInfo("1.2.3", 123L)
        val previousVersion = PackageVersionInfo("1.0.0", 100L)
        val packageInfo = PackageInfoImpl(
            packageName = "com.example.app",
            currentPackageVersionInfo = currentVersion,
            previousPackageVersionInfo = previousVersion
        )

        // when
        val properties = packageInfo.properties

        // then
        expectThat(properties.containsKey("previousVersionName")).isEqualTo(false)
        expectThat(properties.containsKey("previousVersionCode")).isEqualTo(false)
    }
}
