package quiet.launcher

import android.view.View
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-land-xhdpi")
class MovementTest {

    /** Where the remote's direction pad would move the selection from [from]. */
    private fun HomeScreenRobot.move(from: View, direction: Int): View? {
        from.requestFocus()
        return from.focusSearch(direction)
    }

    private fun column(v: View?) = (v?.tag as String).removePrefix("app.n").toInt().minus(1) % 5

    // top-row-and-search#AC-10
    @Test
    fun `AC-10 left and right stop at the ends of each row of apps`() {
        val r = HomeScreenRobot(appCount = 7)

        assertSame(r.tile(0), r.move(r.tile(0), View.FOCUS_LEFT))
        assertSame(r.tile(4), r.move(r.tile(4), View.FOCUS_RIGHT))
        assertSame(r.tile(5), r.move(r.tile(5), View.FOCUS_LEFT))
        assertSame(r.tile(6), r.move(r.tile(6), View.FOCUS_RIGHT))
    }

    // top-row-and-search#AC-10
    @Test
    fun `AC-10 left and right stop at the ends of the top row`() {
        val r = HomeScreenRobot()

        assertSame(r.searchBox, r.move(r.searchBox, View.FOCUS_LEFT))
        assertSame(r.settings, r.move(r.settings, View.FOCUS_RIGHT))
        assertSame(r.voice, r.move(r.searchBox, View.FOCUS_RIGHT))
        assertSame(r.settings, r.move(r.voice, View.FOCUS_RIGHT))
    }

    // top-row-and-search#AC-10
    @Test
    fun `AC-10 down from the top row goes to the app nearest below`() {
        val r = HomeScreenRobot()

        assertEquals(0, column(r.move(r.searchBox, View.FOCUS_DOWN)).coerceAtMost(1))
        assertTrue(column(r.move(r.settings, View.FOCUS_DOWN)) >= 3)
    }

    // top-row-and-search#AC-10
    @Test
    fun `AC-10 up from the first row of apps goes to the top-row control nearest above`() {
        val r = HomeScreenRobot()

        assertSame(r.searchBox, r.move(r.tile(0), View.FOCUS_UP))
        assertSame(r.settings, r.move(r.tile(4), View.FOCUS_UP))
    }
}
