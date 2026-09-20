package com.gabinkenko.tikapub.tiktok

import com.gabinkenko.tikapub.data.settings.SecureTokenStore
import com.gabinkenko.tikapub.data.settings.TikTokPrivacyLevel
import com.gabinkenko.tikapub.tiktok.model.CreatorInfoData
import com.gabinkenko.tikapub.tiktok.model.InitVideoRequest
import com.gabinkenko.tikapub.tiktok.model.PostInfo
import com.gabinkenko.tikapub.tiktok.model.PublishStatusRequest
import com.gabinkenko.tikapub.tiktok.model.PublishStatusValue
import com.gabinkenko.tikapub.tiktok.model.SourceInfo
import com.gabinkenko.tikapub.tiktok.model.TokenResponse
import java.io.File
import java.io.RandomAccessFile
import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class TikTokRepository(
    private val api: TikTokApiService,
    private val rawHttpClient: OkHttpClient,
    private val tokenStore: SecureTokenStore,
) {
    /** Exchanges the OAuth authorization code (from [TikTokAuthManager.parseCallback]) for tokens. */
    suspend fun completeLogin(code: String, codeVerifier: String, redirectUri: String): Result<Unit> {
        val clientKey = tokenStore.clientKey ?: return Result.failure(IllegalStateException("Client Key manquante"))
        val clientSecret = tokenStore.clientSecret ?: return Result.failure(IllegalStateException("Client Secret manquante"))

        return runCatching {
            val response = api.exchangeCodeForToken(
                clientKey = clientKey,
                clientSecret = clientSecret,
                code = code,
                redirectUri = redirectUri,
                codeVerifier = codeVerifier,
            )
            val body = response.body()
            check(response.isSuccessful && body?.accessToken != null) {
                "Échange du code OAuth échoué: ${body?.errorDescription ?: response.errorBody()?.string() ?: response.code()}"
            }
            persistTokens(body)
        }
    }

    /** Refreshes the access token if it's missing or close to expiry. Call before any API request. */
    suspend fun ensureValidAccessToken(): Result<String> {
        val existing = tokenStore.accessToken
        val expiresAt = tokenStore.accessTokenExpiresAtMs
        val refresh = tokenStore.refreshToken

        if (existing != null && System.currentTimeMillis() < expiresAt - TOKEN_REFRESH_MARGIN_MS) {
            return Result.success(existing)
        }
        if (refresh == null) {
            return Result.failure(IllegalStateException("Compte TikTok non connecté."))
        }
        val clientKey = tokenStore.clientKey ?: return Result.failure(IllegalStateException("Client Key manquante"))
        val clientSecret = tokenStore.clientSecret ?: return Result.failure(IllegalStateException("Client Secret manquante"))

        return runCatching {
            val response = api.refreshToken(clientKey = clientKey, clientSecret = clientSecret, refreshToken = refresh)
            val body = response.body()
            check(response.isSuccessful && body?.accessToken != null) {
                "Rafraîchissement du token échoué: ${body?.errorDescription ?: response.code()}"
            }
            persistTokens(body)
            body.accessToken!!
        }
    }

    suspend fun queryCreatorInfo(): Result<CreatorInfoData> {
        val token = ensureValidAccessToken().getOrElse { return Result.failure(it) }
        return runCatching {
            val response = api.queryCreatorInfo("Bearer $token")
            val body = response.body()
            check(response.isSuccessful && body?.data != null) {
                "Impossible de récupérer les infos du créateur: ${body?.error?.message ?: response.code()}"
            }
            body.data!!
        }
    }

    /**
     * Uploads [videoFile] and starts a TikTok Direct Post. Returns the `publish_id` once TikTok
     * confirms the post reached a terminal state (published, sent to inbox, or failed).
     */
    suspend fun publishVideo(
        videoFile: File,
        caption: String,
        privacyLevel: TikTokPrivacyLevel,
        disableComment: Boolean,
        disableDuet: Boolean,
        disableStitch: Boolean,
    ): Result<String> {
        val token = ensureValidAccessToken().getOrElse { return Result.failure(it) }
        val bearer = "Bearer $token"

        return runCatching {
            val videoSize = videoFile.length()
            check(videoSize > 0) { "Fichier vidéo vide ou introuvable: ${videoFile.path}" }

            val chunkSize = if (videoSize <= MAX_SINGLE_CHUNK_BYTES) videoSize else DEFAULT_CHUNK_BYTES
            val totalChunks = if (chunkSize <= 0) 1 else ((videoSize + chunkSize - 1) / chunkSize).toInt()

            val initResponse = api.initVideoUpload(
                bearer,
                InitVideoRequest(
                    postInfo = PostInfo(
                        title = caption,
                        privacyLevel = privacyLevel.apiValue,
                        disableDuet = disableDuet,
                        disableComment = disableComment,
                        disableStitch = disableStitch,
                    ),
                    sourceInfo = SourceInfo(
                        videoSize = videoSize,
                        chunkSize = chunkSize,
                        totalChunkCount = totalChunks,
                    ),
                ),
            )
            val initBody = initResponse.body()
            check(initResponse.isSuccessful && initBody?.data?.uploadUrl != null && initBody.data.publishId != null) {
                "Initialisation de l'upload TikTok échouée: ${initBody?.error?.message ?: initResponse.code()}"
            }
            val uploadUrl = initBody.data.uploadUrl!!
            val publishId = initBody.data.publishId!!

            uploadChunks(uploadUrl, videoFile, videoSize, chunkSize, totalChunks)
            pollUntilTerminal(bearer, publishId)
            publishId
        }
    }

    private fun uploadChunks(uploadUrl: String, file: File, totalSize: Long, chunkSize: Long, totalChunks: Int) {
        RandomAccessFile(file, "r").use { raf ->
            for (chunkIndex in 0 until totalChunks) {
                val start = chunkIndex * chunkSize
                val end = minOf(start + chunkSize, totalSize) - 1
                val length = (end - start + 1).toInt()
                val buffer = ByteArray(length)
                raf.seek(start)
                raf.readFully(buffer)

                val request = Request.Builder()
                    .url(uploadUrl)
                    .put(buffer.toRequestBody("video/mp4".toMediaType()))
                    .addHeader("Content-Range", "bytes $start-$end/$totalSize")
                    .addHeader("Content-Type", "video/mp4")
                    .build()

                rawHttpClient.newCall(request).execute().use { resp ->
                    check(resp.isSuccessful) {
                        "Échec de l'envoi du chunk vidéo $chunkIndex/${totalChunks - 1}: HTTP ${resp.code}"
                    }
                }
            }
        }
    }

    private suspend fun pollUntilTerminal(bearer: String, publishId: String) {
        repeat(STATUS_POLL_MAX_ATTEMPTS) { attempt ->
            val response = api.fetchPublishStatus(bearer, PublishStatusRequest(publishId))
            val status = response.body()?.data?.status
            if (PublishStatusValue.isTerminal(status)) {
                check(status != PublishStatusValue.FAILED) {
                    "TikTok a rejeté la publication: ${response.body()?.data?.failReason ?: "raison inconnue"}"
                }
                return
            }
            if (attempt < STATUS_POLL_MAX_ATTEMPTS - 1) delay(STATUS_POLL_INTERVAL_MS)
        }
        // Not terminal after polling: TikTok is still processing, treat as accepted (fire-and-forget).
    }

    private fun persistTokens(body: TokenResponse) {
        val now = System.currentTimeMillis()
        tokenStore.accessToken = body.accessToken
        tokenStore.refreshToken = body.refreshToken ?: tokenStore.refreshToken
        tokenStore.openId = body.openId ?: tokenStore.openId
        tokenStore.accessTokenExpiresAtMs = now + (body.expiresIn ?: 0L) * 1000
        body.refreshExpiresIn?.let { tokenStore.refreshTokenExpiresAtMs = now + it * 1000 }
    }

    companion object {
        private const val TOKEN_REFRESH_MARGIN_MS = 5 * 60 * 1000L
        private const val MAX_SINGLE_CHUNK_BYTES = 64L * 1024 * 1024
        private const val DEFAULT_CHUNK_BYTES = 10L * 1024 * 1024
        private const val STATUS_POLL_INTERVAL_MS = 3000L
        private const val STATUS_POLL_MAX_ATTEMPTS = 20
    }
}
