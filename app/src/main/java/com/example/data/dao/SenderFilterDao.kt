package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.SenderFilterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SenderFilterDao {
    @Query("SELECT * FROM sender_filters ORDER BY createdAt DESC")
    fun getAllFiltersFlow(): Flow<List<SenderFilterEntity>>

    @Query("SELECT * FROM sender_filters WHERE isEnabled = 1")
    fun getActiveFiltersFlow(): Flow<List<SenderFilterEntity>>

    @Query("SELECT * FROM sender_filters WHERE isEnabled = 1")
    suspend fun getActiveFiltersList(): List<SenderFilterEntity>

    @Query("SELECT * FROM sender_filters WHERE LOWER(phoneNumberOrSenderId) = LOWER(:sender) LIMIT 1")
    suspend fun findBySender(sender: String): SenderFilterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFilter(filter: SenderFilterEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFilters(filters: List<SenderFilterEntity>)

    @Update
    suspend fun updateFilter(filter: SenderFilterEntity)

    @Delete
    suspend fun deleteFilter(filter: SenderFilterEntity)

    @Query("DELETE FROM sender_filters WHERE id = :id")
    suspend fun deleteFilterById(id: Long)
}
