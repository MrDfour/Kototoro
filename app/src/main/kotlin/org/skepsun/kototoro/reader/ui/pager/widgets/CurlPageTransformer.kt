package org.skepsun.kototoro.reader.ui.pager.widgets

import android.view.View
import androidx.viewpager2.widget.ViewPager2

class CurlPageTransformer(
	private val isReversed: Boolean,
	private val isVertical: Boolean
) : ViewPager2.PageTransformer {

	override fun transformPage(page: View, position: Float) {
		val pageCurlLayout = page as? PageCurlLayout ?: return

		page.cameraDistance = 20000f

		if (isVertical) {
			page.translationY = -position * page.height
			page.translationX = 0f
			when {
				position < -1f || position > 1f -> {
					page.alpha = 0f
					page.translationZ = -1f
					pageCurlLayout.setCurlProgress(0f, isReversed, isVertical)
				}
				position > 0f -> {
					// Page underneath
					page.alpha = 1f
					page.translationZ = 0f
					pageCurlLayout.setCurlProgress(0f, isReversed, isVertical)
				}
				position <= 0f -> {
					// Page being turned (progress is negative)
					page.alpha = 1f
					page.translationZ = 2f
					pageCurlLayout.setCurlProgress(position, isReversed, isVertical)
				}
			}
		} else if (isReversed) {
			page.translationX = -position * page.width
			page.translationY = 0f
			when {
				position < -1f || position > 1f -> {
					page.alpha = 0f
					page.translationZ = -1f
					pageCurlLayout.setCurlProgress(0f, isReversed, isVertical)
				}
				position <= 0f -> {
					// Page underneath
					page.alpha = 1f
					page.translationZ = 0f
					pageCurlLayout.setCurlProgress(0f, isReversed, isVertical)
				}
				position > 0f -> {
					// Page being turned (progress is positive)
					page.alpha = 1f
					page.translationZ = 2f
					pageCurlLayout.setCurlProgress(position, isReversed, isVertical)
				}
			}
		} else {
			// LTR standard
			page.translationX = -position * page.width
			page.translationY = 0f
			when {
				position < -1f || position > 1f -> {
					page.alpha = 0f
					page.translationZ = -1f
					pageCurlLayout.setCurlProgress(0f, isReversed, isVertical)
				}
				position > 0f -> {
					// Page underneath
					page.alpha = 1f
					page.translationZ = 0f
					pageCurlLayout.setCurlProgress(0f, isReversed, isVertical)
				}
				position <= 0f -> {
					// Page being turned (progress is negative)
					page.alpha = 1f
					page.translationZ = 2f
					pageCurlLayout.setCurlProgress(position, isReversed, isVertical)
				}
			}
		}
	}
}
