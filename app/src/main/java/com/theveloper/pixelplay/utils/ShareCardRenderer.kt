package com.theveloper.pixelplay.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.ContextCompat
import coil.imageLoader
import coil.request.ImageRequest
import com.theveloper.pixelplay.R
import com.theveloper.pixelplay.data.model.Song
import kotlin.math.max

/**
 * Aspect ratios offered for the generated share card.
 */
enum class ShareCardAspect(val widthRatio: Float, val heightRatio: Float) {
    SQUARE(1f, 1f),
    STORY(9f, 16f);

    val ratio: Float get() = widthRatio / heightRatio
}

/**
 * Renders the "music card" shared for a song: the album artwork as backdrop, a gradient
 * scrim, the song title and artist, plus a small PixelPlayer logo. The result is a bitmap
 * that both the in-app preview and the shared/saved file are produced from, so what the
 * user sees is exactly what gets shared.
 */
object ShareCardRenderer {

    private const val OUTPUT_WIDTH = 1080
    private const val BRAND_NAME = "PixelPlayer"

    // Google Sans Flex weight axis values used for the card typography.
    private const val TITLE_WEIGHT = 700
    private const val ARTIST_WEIGHT = 500
    private const val WORDMARK_WEIGHT = 500

    // Rounded axis value used across the app's Google Sans Rounded family.
    private const val ROUNDED_AXIS = 100

    suspend fun render(context: Context, song: Song, aspect: ShareCardAspect): Bitmap {
        val width = OUTPUT_WIDTH
        val height = (OUTPUT_WIDTH * aspect.heightRatio / aspect.widthRatio).toInt()
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }

        // 1. Backdrop: the album artwork, center-cropped to fill the whole card.
        val artwork = loadArtwork(context, song.albumArtUriString)
        if (artwork != null) {
            drawCenterCrop(canvas, artwork, width, height, bitmapPaint)
        } else {
            val fallback = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f, 0f, width.toFloat(), height.toFloat(),
                    intArrayOf(0xFF35313F.toInt(), 0xFF16141C.toInt()),
                    null, Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), fallback)
        }

        // 2. Gradients: a light one on top (logo legibility) and a strong one at the bottom (text).
        val scrim = Paint(Paint.ANTI_ALIAS_FLAG)
        scrim.shader = LinearGradient(
            0f, 0f, 0f, height * 0.32f,
            Color.argb(140, 0, 0, 0), Color.TRANSPARENT, Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, width.toFloat(), height * 0.32f, scrim)
        scrim.shader = LinearGradient(
            0f, height * 0.34f, 0f, height.toFloat(),
            Color.TRANSPARENT, Color.argb(240, 0, 0, 0), Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, height * 0.34f, width.toFloat(), height.toFloat(), scrim)

        // 3. Title + artist, anchored to the bottom-left.
        val padding = width * 0.075f
        val textWidth = (width - padding * 2f).toInt()

        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = width * 0.077f
            typeface = brandTypeface(context, TITLE_WEIGHT)
        }
        val artistPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(225, 255, 255, 255)
            textSize = width * 0.045f
            typeface = brandTypeface(context, ARTIST_WEIGHT)
        }

        val titleLayout = buildTextLayout(song.title, titlePaint, textWidth, maxLines = 2)
        val artistLayout = buildTextLayout(song.displayArtist, artistPaint, textWidth, maxLines = 1)

        val gap = width * 0.022f
        val artistTop = height - padding - artistLayout.height
        val titleTop = artistTop - gap - titleLayout.height

        canvas.save()
        canvas.translate(padding, titleTop)
        titleLayout.draw(canvas)
        canvas.restore()

        canvas.save()
        canvas.translate(padding, artistTop)
        artistLayout.draw(canvas)
        canvas.restore()

        // 4. Branding, top-left.
        drawBranding(context, canvas, padding)

        return output
    }

    private fun drawBranding(context: Context, canvas: Canvas, padding: Float) {
        val logoSize = (padding * 1.35f).toInt().coerceAtLeast(1)
        val logo = loadLogo(context, logoSize)
        val wordmarkPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(235, 255, 255, 255)
            textSize = padding * 0.62f
            typeface = brandTypeface(context, WORDMARK_WEIGHT)
            letterSpacing = 0.02f
        }
        val metrics = wordmarkPaint.fontMetrics

        if (logo != null) {
            val dst = RectF(padding, padding, padding + logoSize, padding + logoSize)
            val clip = Path().apply {
                addRoundRect(dst, logoSize * 0.28f, logoSize * 0.28f, Path.Direction.CW)
            }
            canvas.save()
            canvas.clipPath(clip)
            canvas.drawBitmap(
                logo,
                null,
                dst,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
            )
            canvas.restore()

            val centerY = padding + logoSize / 2f
            val baseline = centerY - (metrics.ascent + metrics.descent) / 2f
            canvas.drawText(BRAND_NAME, padding + logoSize + padding * 0.32f, baseline, wordmarkPaint)
        } else {
            val baseline = padding - (metrics.ascent + metrics.descent) / 2f
            canvas.drawText(BRAND_NAME, padding, baseline, wordmarkPaint)
        }
    }

    private fun buildTextLayout(
        text: String,
        paint: TextPaint,
        widthPx: Int,
        maxLines: Int
    ): StaticLayout {
        return StaticLayout.Builder
            .obtain(text, 0, text.length, paint, widthPx)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setMaxLines(maxLines)
            .setEllipsize(TextUtils.TruncateAt.END)
            .setLineSpacing(0f, 1.06f)
            .setIncludePad(false)
            .build()
    }

    private fun drawCenterCrop(canvas: Canvas, source: Bitmap, width: Int, height: Int, paint: Paint) {
        if (source.width <= 0 || source.height <= 0) return
        val scale = max(width.toFloat() / source.width, height.toFloat() / source.height)
        val dstWidth = source.width * scale
        val dstHeight = source.height * scale
        val left = (width - dstWidth) / 2f
        val top = (height - dstHeight) / 2f
        canvas.drawBitmap(source, null, RectF(left, top, left + dstWidth, top + dstHeight), paint)
    }

    private suspend fun loadArtwork(context: Context, model: String?): Bitmap? {
        if (model.isNullOrBlank()) return null
        return runCatching {
            val request = ImageRequest.Builder(context)
                .data(model)
                .allowHardware(false)
                .size(OUTPUT_WIDTH, OUTPUT_WIDTH)
                .build()
            context.imageLoader.execute(request).drawable?.let { drawable ->
                val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: OUTPUT_WIDTH
                val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: OUTPUT_WIDTH
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                drawable.setBounds(0, 0, width, height)
                drawable.draw(canvas)
                bitmap
            }
        }.getOrNull()
    }

    /**
     * Google Sans Rounded — the app's typeface — applied to a canvas [TextPaint]. The app
     * bundles Google Sans Flex and selects the rounded axis via a font variation, so we
     * reproduce the exact same instance here for the generated card.
     */
    @Suppress("DEPRECATION")
    private fun brandTypeface(context: Context, weight: Int): Typeface {
        return runCatching {
            Typeface.Builder(context.resources, R.font.gflex_variable)
                .setFontVariationSettings("'wght' $weight, 'ROND' $ROUNDED_AXIS")
                .build()
        }.getOrNull() ?: Typeface.create(
            "sans-serif",
            if (weight >= 600) Typeface.BOLD else Typeface.NORMAL
        )
    }

    private fun loadLogo(context: Context, sizePx: Int): Bitmap? {
        val drawable = runCatching {
            ContextCompat.getDrawable(context, R.mipmap.ic_launcher)
        }.getOrNull() ?: return null
        return runCatching {
            val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, sizePx, sizePx)
            drawable.draw(canvas)
            bitmap
        }.getOrNull()
    }
}
