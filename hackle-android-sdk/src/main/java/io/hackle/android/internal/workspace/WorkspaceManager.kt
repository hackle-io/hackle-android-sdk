package io.hackle.android.internal.workspace

import io.hackle.sdk.core.workspace.WorkspaceFetcher

internal interface WorkspaceManager : WorkspaceFetcher {
    fun initialize()
}