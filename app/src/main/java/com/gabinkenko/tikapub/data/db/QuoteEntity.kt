package com.gabinkenko.tikapub.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A short piece of text (quote, fact, tip...) that the video engine turns into a clip.
 * [lastUsedAtMs] lets the picker rotate through content instead of repeating the same quote.
 */
@Entity(tableName = "quotes")
data class QuoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val author: String? = null,
    val category: String = "general",
    val enabled: Boolean = true,
    val lastUsedAtMs: Long = 0L,
    val createdAtMs: Long = System.currentTimeMillis(),
)
