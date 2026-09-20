package com.gabinkenko.tikapub.video

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import kotlin.random.Random

/** Generates a vertical gradient background so every clip doesn't need a stock asset. */
object BackgroundBitmapFactory {

    private val palettes = listOf(
        intArrayOf(0xFF7C4DFF.toInt(), 0xFF1A1147.toInt()),
        intArrayOf(0xFFFE2C55.toInt(), 0xFF1A0510.toInt()),
        intArrayOf(0xFF25F4EE.toInt(), 0xFF072B2B.toInt()),
        intArrayOf(0xFFFF9A3D.toInt(), 0xFF2B140A.toInt()),
        intArrayOf(0xFF3D5AFE.toInt(), 0xFF080B29.toInt()),
        intArrayOf(0xFF00C853.toInt(), 0xFF04210F.toInt()),
    )

    fun generate(width: Int, height: Int, seed: Long = Random.nextLong()): Bitmap {
        val random = Random(seed)
        val palette = palettes[random.nextInt(palettes.size)]

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val gradient = LinearGradient(
            0f, 0f, width * 0.3f, height.toFloat(),
            palette[0], palette[1],
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), Paint().apply { shader = gradient })

        // A few soft translucent circles for depth, without needing any bundled image asset.
        val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x22FFFFFF
        }
        repeat(4) {
            val radius = width * (0.25f + random.nextFloat() * 0.35f)
            val cx = random.nextFloat() * width
            val cy = random.nextFloat() * height
            canvas.drawCircle(cx, cy, radius, circlePaint)
        }

        return bitmap
    }
}
