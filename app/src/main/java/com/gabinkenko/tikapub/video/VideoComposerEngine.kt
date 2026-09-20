package com.gabinkenko.tikapub.video

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.OverlayEffect
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.google.common.collect.ImmutableList
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/**
 * Turns a quote (+ optional background music) into a vertical MP4: a generated gradient
 * background with a slow Ken Burns zoom, the quote text as a static overlay, and the music track
 * clipped to the clip's duration. Built on Media3 Transformer so decode/encode/muxing (including
 * converting whatever the music file's codec is to AAC) is handled by the library.
 */
class VideoComposerEngine(private val context: Context) {

    data class Request(
        val quoteText: String,
        val author: String?,
        val handle: String,
        val musicUri: Uri?,
        val durationSeconds: Int = 15,
        val widthPx: Int = 1080,
        val heightPx: Int = 1920,
    )

    @OptIn(markerClass = [UnstableApi::class])
    suspend fun compose(request: Request): File {
        val outputFile = File(outputDir(), "tikapub_${System.currentTimeMillis()}.mp4")
        val durationUs = request.durationSeconds * 1_000_000L

        val backgroundFile = bitmapToFile(
            BackgroundBitmapFactory.generate(request.widthPx, request.heightPx),
            "bg",
        )
        val overlayBitmap = QuoteOverlayBitmapFactory.render(
            request.widthPx, request.heightPx, request.quoteText, request.author, request.handle,
        )

        try {
            val kenBurns = KenBurnsTransformation(durationUs)
            val overlayEffect = OverlayEffect(
                ImmutableList.of(BitmapOverlay.createStaticBitmapOverlay(overlayBitmap)),
            )

            val videoItem = EditedMediaItem.Builder(MediaItem.fromUri(Uri.fromFile(backgroundFile)))
                .setDurationUs(durationUs)
                .setFrameRate(30)
                .setEffects(Effects(emptyList(), listOf(kenBurns, overlayEffect)))
                .build()
            val videoSequence = EditedMediaItemSequence(ImmutableList.of(videoItem))

            val audioSequence = request.musicUri?.let { buildAudioSequence(it, durationUs) }

            val composition = if (audioSequence != null) {
                Composition.Builder(videoSequence, audioSequence).build()
            } else {
                Composition.Builder(videoSequence).build()
            }

            runExport(composition, outputFile)
            return outputFile
        } finally {
            backgroundFile.delete()
        }
    }

    @OptIn(markerClass = [UnstableApi::class])
    private fun buildAudioSequence(musicUri: Uri, durationUs: Long): EditedMediaItemSequence {
        val clippedMusic = MediaItem.Builder()
            .setUri(musicUri)
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setEndPositionMs(durationUs / 1000)
                    .build(),
            )
            .build()
        val audioItem = EditedMediaItem.Builder(clippedMusic)
            .setRemoveVideo(true)
            .build()
        return EditedMediaItemSequence(ImmutableList.of(audioItem))
    }

    @OptIn(markerClass = [UnstableApi::class])
    private suspend fun runExport(composition: Composition, outputFile: File) {
        withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { cont ->
                lateinit var transformer: Transformer
                transformer = Transformer.Builder(context)
                    .addListener(object : Transformer.Listener {
                        override fun onCompleted(composition: Composition, result: ExportResult) {
                            if (cont.isActive) cont.resume(Unit)
                        }

                        override fun onError(
                            composition: Composition,
                            result: ExportResult,
                            exception: ExportException,
                        ) {
                            if (cont.isActive) cont.resumeWithException(exception)
                        }
                    })
                    .build()
                cont.invokeOnCancellation { transformer.cancel() }
                transformer.start(composition, outputFile.absolutePath)
            }
        }
    }

    private fun bitmapToFile(bitmap: Bitmap, prefix: String): File {
        val file = File(outputDir(), "${prefix}_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return file
    }

    private fun outputDir(): File = File(context.cacheDir, "videos").apply { mkdirs() }
}
