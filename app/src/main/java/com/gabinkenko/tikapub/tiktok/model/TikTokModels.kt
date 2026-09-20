package com.gabinkenko.tikapub.tiktok.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TokenResponse(
    @Json(name = "access_token") val accessToken: String? = null,
    @Json(name = "expires_in") val expiresIn: Long? = null,
    @Json(name = "open_id") val openId: String? = null,
    @Json(name = "refresh_token") val refreshToken: String? = null,
    @Json(name = "refresh_expires_in") val refreshExpiresIn: Long? = null,
    @Json(name = "scope") val scope: String? = null,
    @Json(name = "token_type") val tokenType: String? = null,
    @Json(name = "error") val error: String? = null,
    @Json(name = "error_description") val errorDescription: String? = null,
)

@JsonClass(generateAdapter = true)
data class PostInfo(
    val title: String,
    @Json(name = "privacy_level") val privacyLevel: String,
    @Json(name = "disable_duet") val disableDuet: Boolean = false,
    @Json(name = "disable_comment") val disableComment: Boolean = false,
    @Json(name = "disable_stitch") val disableStitch: Boolean = false,
    @Json(name = "video_cover_timestamp_ms") val videoCoverTimestampMs: Long = 1000,
)

@JsonClass(generateAdapter = true)
data class SourceInfo(
    val source: String = "FILE_UPLOAD",
    @Json(name = "video_size") val videoSize: Long,
    @Json(name = "chunk_size") val chunkSize: Long,
    @Json(name = "total_chunk_count") val totalChunkCount: Int,
)

@JsonClass(generateAdapter = true)
data class InitVideoRequest(
    @Json(name = "post_info") val postInfo: PostInfo,
    @Json(name = "source_info") val sourceInfo: SourceInfo,
)

@JsonClass(generateAdapter = true)
data class InitVideoResponseData(
    @Json(name = "publish_id") val publishId: String? = null,
    @Json(name = "upload_url") val uploadUrl: String? = null,
)

@JsonClass(generateAdapter = true)
data class ApiError(
    val code: String? = null,
    val message: String? = null,
    @Json(name = "log_id") val logId: String? = null,
)

@JsonClass(generateAdapter = true)
data class InitVideoResponse(
    val data: InitVideoResponseData? = null,
    val error: ApiError? = null,
)

@JsonClass(generateAdapter = true)
data class PublishStatusRequest(
    @Json(name = "publish_id") val publishId: String,
)

@JsonClass(generateAdapter = true)
data class PublishStatusData(
    val status: String? = null,
    @Json(name = "fail_reason") val failReason: String? = null,
    @Json(name = "publicaly_available_post_id") val publiclyAvailablePostId: List<String>? = null,
)

@JsonClass(generateAdapter = true)
data class PublishStatusResponse(
    val data: PublishStatusData? = null,
    val error: ApiError? = null,
)

@JsonClass(generateAdapter = true)
data class CreatorInfoData(
    @Json(name = "creator_avatar_url") val creatorAvatarUrl: String? = null,
    @Json(name = "creator_username") val creatorUsername: String? = null,
    @Json(name = "creator_nickname") val creatorNickname: String? = null,
    @Json(name = "privacy_level_options") val privacyLevelOptions: List<String>? = null,
    @Json(name = "comment_disabled") val commentDisabled: Boolean? = null,
    @Json(name = "duet_disabled") val duetDisabled: Boolean? = null,
    @Json(name = "stitch_disabled") val stitchDisabled: Boolean? = null,
    @Json(name = "max_video_post_duration_sec") val maxVideoPostDurationSec: Int? = null,
)

@JsonClass(generateAdapter = true)
data class CreatorInfoResponse(
    val data: CreatorInfoData? = null,
    val error: ApiError? = null,
)

/** Terminal-ish states returned by /v2/post/publish/status/fetch/. */
object PublishStatusValue {
    const val PROCESSING_UPLOAD = "PROCESSING_UPLOAD"
    const val PROCESSING_DOWNLOAD = "PROCESSING_DOWNLOAD"
    const val SEND_TO_USER_INBOX = "SEND_TO_USER_INBOX"
    const val PUBLISH_COMPLETE = "PUBLISH_COMPLETE"
    const val FAILED = "FAILED"

    fun isTerminal(status: String?): Boolean =
        status == PUBLISH_COMPLETE || status == FAILED || status == SEND_TO_USER_INBOX
}
