package com.gabinkenko.tikapub.video

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint

/**
 * Renders the quote text (+ optional author + the @handle watermark) onto a transparent bitmap
 * sized like the video. Kept as plain Canvas/StaticLayout drawing (no Compose) so it can run from
 * a background Worker without a UI context.
 */
object QuoteOverlayBitmapFactory {

    fun render(
        width: Int,
        height: Int,
        quoteText: String,
        author: String?,
        handle: String,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val quotePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = width * 0.078f
            typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
            setShadowLayer(width * 0.02f, 0f, width * 0.006f, 0x66000000)
        }

        val maxTextWidth = (width * 0.82f).toInt()
        val quoteLayout = buildStaticLayout("“${quoteText.trim()}”", quotePaint, maxTextWidth)

        val authorPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFE0E0E0.toInt()
            textSize = width * 0.045f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        }
        val authorLayout = author?.takeIf { it.isNotBlank() }
            ?.let { buildStaticLayout("— $it", authorPaint, maxTextWidth) }

        val spacing = height * 0.03f
        val cardPaddingH = width * 0.09f
        val cardPaddingV = height * 0.045f
        val contentHeight = quoteLayout.height + (authorLayout?.let { spacing + it.height } ?: 0f)
        val cardTop = (height - contentHeight) / 2f - cardPaddingV
        val cardBottom = (height + contentHeight) / 2f + cardPaddingV
        val cardLeft = (width - maxTextWidth) / 2f - cardPaddingH
        val cardRight = (width + maxTextWidth) / 2f + cardPaddingH

        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x59000000 }
        canvas.drawRoundRect(
            RectF(cardLeft, cardTop, cardRight, cardBottom),
            width * 0.06f,
            width * 0.06f,
            cardPaint,
        )

        canvas.save()
        canvas.translate((width - maxTextWidth) / 2f, (height - contentHeight) / 2f)
        quoteLayout.draw(canvas)
        canvas.restore()

        if (authorLayout != null) {
            canvas.save()
            canvas.translate(
                (width - maxTextWidth) / 2f,
                (height - contentHeight) / 2f + quoteLayout.height + spacing,
            )
            authorLayout.draw(canvas)
            canvas.restore()
        }

        drawHandleWatermark(canvas, width, height, handle)

        return bitmap
    }

    private fun drawHandleWatermark(canvas: Canvas, width: Int, height: Int, handle: String) {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xCCFFFFFF.toInt()
            textSize = width * 0.042f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
            setShadowLayer(width * 0.015f, 0f, 0f, 0x88000000)
        }
        canvas.drawText(handle, width / 2f, height * 0.94f, paint)
    }

    private fun buildStaticLayout(text: String, paint: TextPaint, width: Int): StaticLayout {
        return StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(width * 0.01f, 1.08f)
            .setIncludePad(false)
            .build()
    }
}
