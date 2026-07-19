package com.example.event_countdown

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient

/**
 * Draws the widget's glass/gradient card and its circular progress ring as
 * plain bitmaps. Bitmaps are stretched into place by the host ImageViews
 * (fitXY for the card, fitCenter for the always-square ring) so we never
 * need to know the widget's exact on-screen pixel size — this is what makes
 * the ring a true, undistorted circle at any widget width.
 */
object WidgetArtRenderer {

    private const val CARD_W = 480
    private const val CARD_H = 220
    private const val RING_SIZE = 220

    fun parseColor(hex: String?, fallback: Int): Int {
        return try {
            if (hex.isNullOrEmpty()) fallback else Color.parseColor(hex)
        } catch (e: Exception) {
            fallback
        }
    }

    /** Picks black or white text for best contrast against [background]. */
    fun bestTextColor(background: Int): Int {
        val r = Color.red(background) / 255.0
        val g = Color.green(background) / 255.0
        val b = Color.blue(background) / 255.0
        // Relative luminance (sRGB approximation is sufficient here).
        val luminance = 0.2126 * r + 0.7152 * g + 0.0722 * b
        return if (luminance > 0.55) Color.BLACK else Color.WHITE
    }

    fun renderCard(
        context: Context,
        colorStart: Int,
        colorEnd: Int,
        isAmoled: Boolean,
        isHighContrast: Boolean,
        bgImagePath: String?,
    ): Bitmap {
        val bmp = Bitmap.createBitmap(CARD_W, CARD_H, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val corner = CARD_H * 0.26f
        val rect = RectF(1f, 1f, CARD_W - 1f, CARD_H - 1f)
        val path = Path().apply { addRoundRect(rect, corner, corner, Path.Direction.CW) }
        canvas.clipPath(path)

        var paintedImage = false
        if (!bgImagePath.isNullOrEmpty()) {
            paintedImage = tryDrawImageBackground(canvas, bgImagePath)
        }

        if (!paintedImage) {
            when {
                isHighContrast -> {
                    canvas.drawColor(colorStart)
                }
                isAmoled -> {
                    canvas.drawColor(Color.BLACK)
                    // Faint diagonal tint for depth, never bright enough to
                    // hurt true-black OLED power savings.
                    val tint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        shader = LinearGradient(
                            0f, 0f, CARD_W.toFloat(), CARD_H.toFloat(),
                            colorWithAlpha(colorStart, 40), Color.TRANSPARENT,
                            Shader.TileMode.CLAMP
                        )
                    }
                    canvas.drawRect(rect, tint)
                }
                else -> {
                    val gradientPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        shader = LinearGradient(
                            0f, 0f, CARD_W.toFloat(), CARD_H.toFloat(),
                            colorStart, colorEnd, Shader.TileMode.CLAMP
                        )
                    }
                    canvas.drawRect(rect, gradientPaint)

                    // Soft glass sheen, top-left.
                    val sheen = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        shader = RadialGradient(
                            CARD_W * 0.22f, -CARD_H * 0.15f, CARD_W * 0.65f,
                            colorWithAlpha(Color.WHITE, 60), Color.TRANSPARENT,
                            Shader.TileMode.CLAMP
                        )
                    }
                    canvas.drawRect(rect, sheen)
                }
            }
        } else if (isHighContrast) {
            // Scrim so text stays legible even with high-contrast requested
            // over a custom photo background.
            val scrim = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colorWithAlpha(Color.BLACK, 140) }
            canvas.drawRect(rect, scrim)
        }

        // Border for definition against busy home-screen wallpapers.
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = if (isHighContrast) 3f else 1.5f
            color = if (isHighContrast) bestTextColor(colorStart).let { colorWithAlpha(it, 160) }
                    else colorWithAlpha(Color.WHITE, 70)
        }
        canvas.drawRoundRect(rect, corner, corner, borderPaint)

        return bmp
    }

    private fun tryDrawImageBackground(canvas: Canvas, path: String): Boolean {
        return try {
            val src = BitmapFactory.decodeFile(path) ?: return false
            val scale = maxOf(CARD_W.toFloat() / src.width, CARD_H.toFloat() / src.height)
            val dx = (CARD_W - src.width * scale) / 2f
            val dy = (CARD_H - src.height * scale) / 2f
            val matrix = Matrix().apply {
                setScale(scale, scale)
                postTranslate(dx, dy)
            }
            canvas.drawBitmap(src, matrix, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            // Scrim for text legibility over arbitrary photos.
            val scrim = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f, 0f, CARD_W.toFloat(), 0f,
                    colorWithAlpha(Color.BLACK, 150), colorWithAlpha(Color.BLACK, 60),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, CARD_W.toFloat(), CARD_H.toFloat(), scrim)
            if (!src.isRecycled) src.recycle()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun renderRing(
        colorStart: Int,
        colorEnd: Int,
        progressFraction: Float,
        isAmoled: Boolean,
        isHighContrast: Boolean,
        pulseEnabled: Boolean,
        isUrgent: Boolean,
    ): Bitmap {
        val bmp = Bitmap.createBitmap(RING_SIZE, RING_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val cx = RING_SIZE / 2f
        val cy = RING_SIZE / 2f
        val strokeWidth = RING_SIZE * 0.115f
        val radius = RING_SIZE / 2f - strokeWidth / 2f - 8f
        val ringRect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
        val progress = progressFraction.coerceIn(0f, 1f)

        // Gentle glow halo — the closest a static widget snapshot can get to
        // a "pulse": intensity varies with a coarse time phase so it looks
        // alive across successive refreshes without any background service.
        if (pulseEnabled && isUrgent && !isHighContrast) {
            val phase = ((System.currentTimeMillis() / 1200L) % 2L).toInt()
            val glowAlpha = if (phase == 0) 70 else 130
            val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = strokeWidth * 2.1f
                strokeCap = Paint.Cap.ROUND
                color = colorWithAlpha(colorEnd, glowAlpha)
                maskFilter = BlurMaskFilter(radius * 0.5f, BlurMaskFilter.Blur.NORMAL)
            }
            canvas.drawArc(ringRect, -90f, 360f * progress, false, glowPaint)
        }

        // Track.
        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = strokeWidth
            color = when {
                isHighContrast -> colorWithAlpha(bestTextColor(colorStart), 90)
                isAmoled -> colorWithAlpha(Color.WHITE, 25)
                else -> colorWithAlpha(Color.WHITE, 35)
            }
        }
        canvas.drawCircle(cx, cy, radius, trackPaint)

        // Progress arc.
        val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = strokeWidth
            strokeCap = Paint.Cap.ROUND
            if (isHighContrast) {
                color = colorStart
            } else {
                val sweep = SweepGradient(cx, cy, intArrayOf(colorStart, colorEnd, colorStart), floatArrayOf(0f, 0.5f, 1f))
                val matrix = Matrix().apply { postRotate(-90f, cx, cy) }
                sweep.setLocalMatrix(matrix)
                shader = sweep
            }
        }
        canvas.drawArc(ringRect, -90f, 360f * progress, false, progressPaint)

        return bmp
    }

    /** Fully transparent placeholder used when the progress ring is disabled. */
    fun emptyBitmap(): Bitmap = Bitmap.createBitmap(RING_SIZE, RING_SIZE, Bitmap.Config.ARGB_8888)

    private fun colorWithAlpha(color: Int, alpha: Int): Int {
        return Color.argb(alpha.coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))
    }
}
