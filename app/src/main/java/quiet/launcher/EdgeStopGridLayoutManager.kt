package quiet.launcher

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager

/**
 * A grid where the direction pad stops at the ends of each row and below the last row, instead of
 * wrapping to the next or previous row (top-row-and-search#AC-10). Up from the first row is left to
 * Android's own search, which finds the top-row control nearest above.
 */
class EdgeStopGridLayoutManager(context: Context, spanCount: Int) : GridLayoutManager(context, spanCount) {

    override fun onInterceptFocusSearch(focused: View, direction: Int): View? {
        val tile = findContainingItemView(focused) ?: return null
        val position = getPosition(tile)
        val last = itemCount - 1
        val stop = when (direction) {
            View.FOCUS_LEFT -> position % spanCount == 0
            View.FOCUS_RIGHT -> position % spanCount == spanCount - 1 || position == last
            View.FOCUS_DOWN -> position / spanCount == last / spanCount
            else -> false
        }
        return if (stop) focused else null
    }
}
