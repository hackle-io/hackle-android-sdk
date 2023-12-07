package io.hackle.android.internal.database

import android.content.Context
import io.hackle.android.internal.database.shared.SharedDatabase
import io.hackle.android.internal.database.workspace.WorkspaceDatabase

internal object DatabaseHelper {

    private var sharedDatabase: SharedDatabase? = null
    private val workspaceDatabases = hashMapOf<String, WorkspaceDatabase>()

    @Synchronized
    fun getSharedDatabase(context: Context): SharedDatabase {
        if (sharedDatabase == null) {
            sharedDatabase = SharedDatabase(context)
        }
        return sharedDatabase!!
    }

    @Synchronized
    fun getWorkspaceDatabase(context: Context, sdkKey: String): WorkspaceDatabase {
        return workspaceDatabases.getOrPut(sdkKey) {
            WorkspaceDatabase(context, sdkKey)
        }
    }
}