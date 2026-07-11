package org.skepsun.kototoro.reader.ui.pager.vertical

import androidx.viewpager2.widget.ViewPager2
import dagger.hilt.android.AndroidEntryPoint
import org.skepsun.kototoro.reader.ui.pager.BasePagerReaderFragment
import org.skepsun.kototoro.reader.ui.pager.widgets.CurlPageTransformer

@AndroidEntryPoint
class VerticalReaderFragment : BasePagerReaderFragment() {

	override fun onInitPager(pager: ViewPager2) {
		super.onInitPager(pager)
		pager.orientation = ViewPager2.ORIENTATION_VERTICAL
	}

	override fun onCreateAdvancedTransformer(): ViewPager2.PageTransformer = VerticalPageAnimTransformer()

	override fun onCreateCurlTransformer(): ViewPager2.PageTransformer = CurlPageTransformer(isReversed = false, isVertical = true)
}
