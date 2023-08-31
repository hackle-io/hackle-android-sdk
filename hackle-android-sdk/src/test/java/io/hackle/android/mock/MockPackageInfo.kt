package io.hackle.android.mock

import io.hackle.android.internal.platform.PackageInfo

class MockPackageInfo : PackageInfo {

    override val packageName: String = "io.hackle.app"

    override val versionName: String = "1.0.0"

    override val versionCode: Long = 10101L
}