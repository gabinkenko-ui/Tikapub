package com.gabinkenko.tikapub.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PublishLogDao {

    @Query("SELECT * FROM publish_log ORDER BY timestampMs DESC LIMIT :limit")
    fun observeRecent(limit: Int = 50): Flow<List<PublishLogEntity>>

    @Insert
    suspend fun insert(entry: PublishLogEntity): Long

    @Update
    suspend fun update(entry: PublishLogEntity)

    @Query("DELETE FROM publish_log")
    suspend fun clear()
}
