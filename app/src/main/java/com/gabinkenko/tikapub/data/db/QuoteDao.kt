package com.gabinkenko.tikapub.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface QuoteDao {

    @Query("SELECT * FROM quotes ORDER BY createdAtMs DESC")
    fun observeAll(): Flow<List<QuoteEntity>>

    @Query("SELECT COUNT(*) FROM quotes")
    suspend fun count(): Int

    /** Picks the least-recently-used enabled quote, optionally restricted to [categories]. */
    @Query(
        """
        SELECT * FROM quotes
        WHERE enabled = 1
        AND (:categoriesEmpty = 1 OR category IN (:categories))
        ORDER BY lastUsedAtMs ASC, RANDOM()
        LIMIT 1
        """
    )
    suspend fun pickNext(categories: List<String>, categoriesEmpty: Boolean): QuoteEntity?

    @Query("UPDATE quotes SET lastUsedAtMs = :timestampMs WHERE id = :id")
    suspend fun markUsed(id: Long, timestampMs: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(quotes: List<QuoteEntity>)

    @Insert
    suspend fun insert(quote: QuoteEntity): Long

    @Update
    suspend fun update(quote: QuoteEntity)

    @Query("DELETE FROM quotes WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT DISTINCT category FROM quotes ORDER BY category ASC")
    fun observeCategories(): Flow<List<String>>
}
