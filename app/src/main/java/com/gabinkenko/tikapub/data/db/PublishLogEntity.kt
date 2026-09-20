package com.gabinkenko.tikapub.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class PublishStatus { PENDING, UPLOADING, SUCCESS, FAILED }

@Entity(tableName = "publish_log")
data class PublishLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMs: Long = System.currentTimeMillis(),
    val quoteId: Long?,
    val quoteText: String,
    val videoPath: String?,
    val tiktokPublishId: String? = null,
    val status: PublishStatus,
    val message: String? = null,
)
