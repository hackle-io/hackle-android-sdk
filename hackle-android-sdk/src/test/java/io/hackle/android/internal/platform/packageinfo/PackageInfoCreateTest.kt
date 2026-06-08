package io.hackle.android.internal.platform.packageinfo

import android.content.Context
import android.content.pm.PackageManager
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class PackageInfoCreateTest {

    @Test
    fun `create populates versionCode even when platform versionName is null`() {
        // given - some APKs / split installs report a null versionName.
        // Assigning it to the non-null String used to throw, dropping into
        // the catch block before versionCode (assigned afterwards) was set.
        val androidPackageInfo = mockk<android.content.pm.PackageInfo>(relaxed = true)
        androidPackageInfo.versionName = null
        @Suppress("DEPRECATION")
        androidPackageInfo.versionCode = 42

        val packageManager = mockk<PackageManager>()
        every { packageManager.getPackageInfo(any<String>(), any<Int>()) } returns androidPackageInfo

        val context = mockk<Context>()
        every { context.packageManager } returns packageManager
        every { context.packageName } returns "io.hackle.app"

        // when
        val packageInfo = PackageInfo.create(context)

        // then - versionName falls back to "" and versionCode is no longer skipped
        expectThat(packageInfo.properties["versionName"]).isEqualTo("")
        expectThat(packageInfo.properties["versionCode"]).isEqualTo(42L)
    }
}
