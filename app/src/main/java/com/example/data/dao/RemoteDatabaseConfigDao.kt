package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.RemoteDatabaseConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RemoteDatabaseConfigDao {

    @Query("SELECT * FROM remote_database_config WHERE id = 1 LIMIT 1")
    fun getConfig(): Flow<RemoteDatabaseConfigEntity?>

    @Query("SELECT * FROM remote_database_config WHERE id = 1 LIMIT 1")
    suspend fun getConfigDirect(): RemoteDatabaseConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateConfig(config: RemoteDatabaseConfigEntity)

    @Query("UPDATE remote_database_config SET lastSyncStatus = :status, lastSyncTime = :time, lastErrorMessage = :error WHERE id = 1")
    suspend fun updateSyncStatus(status: String, time: Long, error: String?)
}
