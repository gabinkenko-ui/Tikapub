package com.gabinkenko.tikapub.worker

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import com.gabinkenko.tikapub.AppContainer
import com.gabinkenko.tikapub.MainActivity
import com.gabinkenko.tikapub.R
import com.gabinkenko.tikapub.TikapubApplication
import com.gabinkenko.tikapub.data.db.PublishLogEntity
import com.gabinkenko.tikapub.data.db.PublishStatus
import com.gabinkenko.tikapub.video.MusicLibrary
import com.gabinkenko.tikapub.video.VideoComposerEngine
import kotlinx.coroutines.flow.first

/**
 * Generates one video from the next quote in rotation and publishes it to TikTok. Runs either on
 * the daily schedule ([TAG_PERIODIC], gated by the "auto-publish" toggle) or on demand
 * ([TAG_MANUAL], from the "Publier maintenant" button, which always runs regardless of the toggle).
 */
class PublishWorker(
    appContext: Context,
    params: WorkerParameters,
    private val container: AppContainer,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val settings = container.settingsRepository.settings.first()
        val isManual = tags.contains(TAG_MANUAL)
        if (!settings.autoPublishEnabled && !isManual) return Result.success()

        if (!container.tokenStore.isLinked) {
            logResult(quoteId = null, quoteText = "", videoPath = null, status = PublishStatus.FAILED, message = "Compte TikTok non connecté.")
            return Result.failure()
        }

        val categories = settings.selectedCategories.toList()
        val quote = container.quoteDao.pickNext(categories, categories.isEmpty())
            ?: run {
                logResult(null, "", null, PublishStatus.FAILED, "Aucune citation disponible - ajoutes-en dans l'onglet Citations.")
                return Result.failure()
            }

        setForegroundSafely()

        var videoFile: java.io.File? = null
        return try {
            val musicUri = MusicLibrary.pickRandomTrack(applicationContext, settings.musicFolderUri)
            videoFile = container.videoComposerEngine.compose(
                VideoComposerEngine.Request(
                    quoteText = quote.text,
                    author = quote.author,
                    handle = applicationContext.getString(R.string.tiktok_handle),
                    musicUri = musicUri,
                ),
            )

            val caption = settings.captionTemplate.replace("{quote}", quote.text)
            val publishId = container.tikTokRepository.publishVideo(
                videoFile = videoFile,
                caption = caption,
                privacyLevel = settings.privacyLevel,
                disableComment = !settings.allowCommentsAllowDuet,
                disableDuet = !settings.allowCommentsAllowDuet,
                disableStitch = !settings.allowCommentsAllowDuet,
            ).getOrThrow()

            container.quoteDao.markUsed(quote.id, System.currentTimeMillis())
            logResult(quote.id, quote.text, videoFile.path, PublishStatus.SUCCESS, publishId)
            Result.success()
        } catch (e: Exception) {
            logResult(quote.id, quote.text, videoFile?.path, PublishStatus.FAILED, e.message ?: "Erreur inconnue")
            if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
        } finally {
            videoFile?.delete()
        }
    }

    private suspend fun logResult(
        quoteId: Long?,
        quoteText: String,
        videoPath: String?,
        status: PublishStatus,
        message: String?,
    ) {
        container.publishLogDao.insert(
            PublishLogEntity(
                quoteId = quoteId,
                quoteText = quoteText,
                videoPath = videoPath,
                tiktokPublishId = if (status == PublishStatus.SUCCESS) message else null,
                status = status,
                message = if (status == PublishStatus.FAILED) message else null,
            ),
        )
    }

    private suspend fun setForegroundSafely() {
        runCatching { setForeground(createForegroundInfo()) }
    }

    private fun createForegroundInfo(): ForegroundInfo {
        val openAppIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            Intent(applicationContext, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(applicationContext, TikapubApplication.PUBLISH_CHANNEL_ID)
            .setContentTitle("Tikapub")
            .setContentText("Génération et publication de la vidéo en cours...")
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        const val TAG_MANUAL = "manual_publish"
        const val TAG_PERIODIC = "periodic_publish"
        private const val MAX_RETRIES = 2
        private const val NOTIFICATION_ID = 4201
    }
}
