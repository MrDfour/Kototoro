package org.skepsun.kototoro.reader.ui.pager.widgets

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.widget.FrameLayout

class PageCurlLayout @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

	private var curlProgress = 0f // -1f to 1f
	private var isReversed = false
	private var isVertical = false

	private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
	private val clipPath = Path()

	fun setCurlProgress(progress: Float, reversed: Boolean, vertical: Boolean) {
		if (curlProgress != progress || isReversed != reversed || isVertical != vertical) {
			curlProgress = progress
			isReversed = reversed
			isVertical = vertical
			invalidate()
		}
	}

	override fun draw(canvas: Canvas) {
		val progress = curlProgress
		if (progress == 0f) {
			super.draw(canvas)
			return
		}

		val w = width.toFloat()
		val h = height.toFloat()
		if (w <= 0f || h <= 0f) {
			super.draw(canvas)
			return
		}

		canvas.save()

		if (isVertical) {
			// Vertical mode: position is negative for page being turned.
			val p = -progress.coerceIn(-1f, 0f) // goes 0 to 1
			if (p <= 0f || p >= 1f) {
				super.draw(canvas)
				canvas.restore()
				return
			}
			val yCurl = h * (1f - p)
			val rollWidth = (h * 0.08f).coerceAtMost(150f)
			val shadowWidth = (h * 0.05f).coerceAtMost(100f)

			// Clip path to draw flat page: y from 0 to yCurl
			clipPath.reset()
			clipPath.addRect(0f, 0f, w, yCurl, Path.Direction.CW)
			canvas.clipPath(clipPath)

			super.draw(canvas)
			canvas.restore() // Restore clipping to draw shadow overlays

			drawVerticalCurlShadows(canvas, yCurl, rollWidth, shadowWidth)
		} else if (isReversed) {
			// RTL horizontal mode: position is positive for page being turned.
			val p = progress.coerceIn(0f, 1f) // goes 0 to 1
			if (p <= 0f || p >= 1f) {
				super.draw(canvas)
				canvas.restore()
				return
			}
			val xCurl = w * p
			val rollWidth = (w * 0.08f).coerceAtMost(150f)
			val shadowWidth = (w * 0.05f).coerceAtMost(100f)

			// Clip path to draw flat page: x from xCurl to w
			clipPath.reset()
			clipPath.addRect(xCurl, 0f, w, h, Path.Direction.CW)
			canvas.clipPath(clipPath)

			super.draw(canvas)
			canvas.restore()

			drawReversedHorizontalCurlShadows(canvas, xCurl, rollWidth, shadowWidth)
		} else {
			// LTR horizontal mode: position is negative for page being turned.
			val p = -progress.coerceIn(-1f, 0f) // goes 0 to 1
			if (p <= 0f || p >= 1f) {
				super.draw(canvas)
				canvas.restore()
				return
			}
			val xCurl = w * (1f - p)
			val rollWidth = (w * 0.08f).coerceAtMost(150f)
			val shadowWidth = (w * 0.05f).coerceAtMost(100f)

			// Clip path to draw flat page: x from 0 to xCurl
			clipPath.reset()
			clipPath.addRect(0f, 0f, xCurl, h, Path.Direction.CW)
			canvas.clipPath(clipPath)

			super.draw(canvas)
			canvas.restore()

			drawHorizontalCurlShadows(canvas, xCurl, rollWidth, shadowWidth)
		}
	}

	private fun drawHorizontalCurlShadows(canvas: Canvas, xCurl: Float, rollWidth: Float, shadowWidth: Float) {
		val h = height.toFloat()

		// 1. Draw curl cylinder gradient (to the left of xCurl)
		// Colors from left to right ending at xCurl
		val curlColors = intArrayOf(
			Color.TRANSPARENT,
			0x1A000000.toInt(),
			0x40FFFFFF.toInt(),
			0x66000000.toInt()
		)
		val curlPositions = floatArrayOf(0.0f, 0.4f, 0.7f, 1.0f)
		paint.shader = LinearGradient(
			xCurl - rollWidth, 0f, xCurl, 0f,
			curlColors, curlPositions, Shader.TileMode.CLAMP
		)
		canvas.drawRect(xCurl - rollWidth, 0f, xCurl, h, paint)

		// 2. Draw drop shadow cast on underneath page (to the right of xCurl)
		val shadowColors = intArrayOf(
			0x4D000000.toInt(),
			Color.TRANSPARENT
		)
		paint.shader = LinearGradient(
			xCurl, 0f, xCurl + shadowWidth, 0f,
			shadowColors, null, Shader.TileMode.CLAMP
		)
		canvas.drawRect(xCurl, 0f, xCurl + shadowWidth, h, paint)

		paint.shader = null
	}

	private fun drawReversedHorizontalCurlShadows(canvas: Canvas, xCurl: Float, rollWidth: Float, shadowWidth: Float) {
		val h = height.toFloat()

		// 1. Draw curl cylinder gradient (to the right of xCurl)
		// Colors from left to right starting at xCurl
		val curlColors = intArrayOf(
			0x66000000.toInt(),
			0x40FFFFFF.toInt(),
			0x1A000000.toInt(),
			Color.TRANSPARENT
		)
		val curlPositions = floatArrayOf(0.0f, 0.3f, 0.6f, 1.0f)
		paint.shader = LinearGradient(
			xCurl, 0f, xCurl + rollWidth, 0f,
			curlColors, curlPositions, Shader.TileMode.CLAMP
		)
		canvas.drawRect(xCurl, 0f, xCurl + rollWidth, h, paint)

		// 2. Draw drop shadow cast on underneath page (to the left of xCurl)
		val shadowColors = intArrayOf(
			Color.TRANSPARENT,
			0x4D000000.toInt()
		)
		paint.shader = LinearGradient(
			xCurl - shadowWidth, 0f, xCurl, 0f,
			shadowColors, null, Shader.TileMode.CLAMP
		)
		canvas.drawRect(xCurl - shadowWidth, 0f, xCurl, h, paint)

		paint.shader = null
	}

	private fun drawVerticalCurlShadows(canvas: Canvas, yCurl: Float, rollWidth: Float, shadowWidth: Float) {
		val w = width.toFloat()

		// 1. Draw curl cylinder gradient (above yCurl)
		// Colors from top to bottom ending at yCurl
		val curlColors = intArrayOf(
			Color.TRANSPARENT,
			0x1A000000.toInt(),
			0x40FFFFFF.toInt(),
			0x66000000.toInt()
		)
		val curlPositions = floatArrayOf(0.0f, 0.4f, 0.7f, 1.0f)
		paint.shader = LinearGradient(
			0f, yCurl - rollWidth, 0f, yCurl,
			curlColors, curlPositions, Shader.TileMode.CLAMP
		)
		canvas.drawRect(0f, yCurl - rollWidth, w, yCurl, paint)

		// 2. Draw drop shadow cast on underneath page (below yCurl)
		val shadowColors = intArrayOf(
			0x4D000000.toInt(),
			Color.TRANSPARENT
		)
		paint.shader = LinearGradient(
			0f, yCurl, 0f, yCurl + shadowWidth,
			shadowColors, null, Shader.TileMode.CLAMP
		)
		canvas.drawRect(0f, yCurl, w, yCurl + shadowWidth, paint)

		paint.shader = null
	}
}
