package com.gabinkenko.tikapub.video

import android.graphics.Matrix
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.MatrixTransformation

/** Slow continuous zoom-in on the static background so the clip doesn't feel like a frozen image. */
@UnstableApi
class KenBurnsTransformation(
    private val durationUs: Long,
    private val startScale: Float = 1.0f,
    private val endScale: Float = 1.12f,
) : MatrixTransformation {

    override fun getMatrix(presentationTimeUs: Long): Matrix {
        val progress = if (durationUs <= 0) 0f else (presentationTimeUs.toFloat() / durationUs).coerceIn(0f, 1f)
        val scale = startScale + (endScale - startScale) * progress
        return Matrix().apply { setScale(scale, scale) }
    }
}
