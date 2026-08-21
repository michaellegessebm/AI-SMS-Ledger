package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "remote_database_config")
data class RemoteDatabaseConfigEntity(
    @PrimaryKey
    val id: Int = 1,
    val databaseType: String = "FIREBASE", // FIREBASE, NEON_POSTGRES, SUPABASE, CUSTOM_REST
    val databaseUrl: String = "",
    val apiKeyOrToken: String = "",
    val tableNameOrPath: String = "transactions",
    val isAutoSyncEnabled: Boolean = true,
    val lastSyncTime: Long = 0L,
    val lastSyncStatus: String = "NOT_CONFIGURED", // NOT_CONFIGURED, CONNECTED, SYNCED, ERROR
    val lastErrorMessage: String? = null
)
