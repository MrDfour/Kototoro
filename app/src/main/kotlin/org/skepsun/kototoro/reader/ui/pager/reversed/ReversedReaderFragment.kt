package org.skepsun.kototoro.reader.ui.pager.reversed

import android.util.Log
import androidx.viewpager2.widget.ViewPager2
import dagger.hilt.android.AndroidEntryPoint
import org.skepsun.kototoro.core.prefs.AppSettings
import org.skepsun.kototoro.reader.ui.ReaderState
import org.skepsun.kototoro.reader.ui.pager.BasePagerReaderFragment
import org.skepsun.kototoro.reader.ui.pager.ReaderPage
import org.skepsun.kototoro.reader.ui.pager.widgets.CurlPageTransformer
import javax.inject.Inject

@AndroidEntryPoint
class ReversedReaderFragment : BasePagerReaderFragment() {

	@Inject
	lateinit var settings: AppSettings

	override fun onCreateAdvancedTransformer(): ViewPager2.PageTransformer = ReversedPageAnimTransformer()

	override fun onCreateCurlTransformer(): ViewPager2.PageTransformer = CurlPageTransformer(isReversed = true, isVertical = false)

	override fun onCreateAdapter() = ReversedPagesAdapter(
		lifecycleOwner = viewLifecycleOwner,
		loader = pageLoader,
		enhancementController = enhancementController,
		readerSettingsProducer = viewModel.readerSettingsProducer,
		networkState = networkState,
		exceptionResolver = exceptionResolver,
	)

	override fun onWheelScroll(axisValue: Float) {
		val value = if (settings.isReaderControlAlwaysLTR) -axisValue else axisValue
		super.onWheelScroll(value)
	}

	override fun switchPageBy(delta: Int) {
		super.switchPageBy(-delta)
	}

	override fun switchPageTo(position: Int, smooth: Boolean) {
		super.switchPageTo(reversed(position), smooth)
	}

	override suspend fun onPagesChanged(pages: List<ReaderPage>, pendingState: ReaderState?) {
		super.onPagesChanged(pages.reversed(), pendingState)
	}

	override fun getCurrentState(): ReaderState? = viewBinding?.run {
		val itemCount = readerAdapter?.itemCount ?: return@run null
		if (itemCount <= 0) {
			return@run null
		}
		val adapterPosition = pager.currentItem.coerceIn(0, itemCount - 1)
		val contentPosition = reversed(adapterPosition)
		val page = viewModel.content.value.pages.getOrNull(contentPosition) ?: return@run null
		val state = ReaderState(
			chapterId = page.chapterId,
			page = page.index,
			scroll = 0,
		)
		Log.d(
			LOG_TAG,
			"reversedPager.getCurrentState: adapterPosition=$adapterPosition, " +
				"contentPosition=$contentPosition, page=${page.chapterId}:${page.index}, state=$state",
		)
		state
	}

	override fun notifyPageChanged(page: Int) {
		val pos = reversed(page)
		Log.d(LOG_TAG, "reversedPager.notifyPageChanged: adapterPosition=$page, contentPosition=$pos")
		viewModel.onCurrentPageChanged(pos, pos)
	}

	private fun reversed(position: Int): Int {
		return ((readerAdapter?.itemCount ?: 0) - position - 1).coerceAtLeast(0)
	}

	companion object {
		private const val LOG_TAG = "ReaderDebug"
	}
}
