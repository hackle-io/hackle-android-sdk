package io.hackle.android.internal.platform

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class ManifestMetadataTest {
    private val key = "io.hackle.android.defer_push_link_until_initialized"

    @Test
    fun `boolean reads manifest metadata`() {
        val metadata = mockk<Bundle>()
        every { metadata.containsKey(key) } returns true
        every { metadata.getBoolean(key, false) } returns true
        val context = contextWith(metadata)

        assertEquals(true, ManifestMetadata.boolean(context, key))
    }

    @Test
    fun `boolean uses default when key is absent`() {
        val metadata = mockk<Bundle>()
        every { metadata.containsKey(key) } returns false

        assertEquals(false, ManifestMetadata.boolean(contextWith(metadata), key))
    }

    @Test
    fun `resourceId reads existing icon and ignores zero`() {
        val metadata = mockk<Bundle>()
        every { metadata.containsKey("icon") } returns true
        every { metadata.getInt("icon") } returns 42
        every { metadata.containsKey("empty") } returns true
        every { metadata.getInt("empty") } returns 0
        val context = contextWith(metadata)

        assertEquals(42, ManifestMetadata.resourceId(context, "icon"))
        assertEquals(null, ManifestMetadata.resourceId(context, "empty"))
    }

    private fun contextWith(metadata: Bundle): Context {
        val context = mockk<Context>()
        val packageManager = mockk<PackageManager>()
        val appInfo = ApplicationInfo().apply { metaData = metadata }
        every { context.packageName } returns "io.hackle.test"
        every { context.packageManager } returns packageManager
        every { packageManager.getApplicationInfo("io.hackle.test", PackageManager.GET_META_DATA) } returns appInfo
        return context
    }
}
