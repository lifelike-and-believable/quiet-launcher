package quiet.launcher

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * docs/specs/arrange-mode.md, AC-8: back from arrange mode, the home screen shows the new arrangement
 * straight away. On the TV it drew the old order first and then slid the apps into their new places.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-land-xhdpi")
class ArrangeReturnTest {

    private val robot = HomeScreenRobot(appCount = 7)
    private val adapter get() = robot.grid.adapter as AppAdapter
    private val order get() = (0 until adapter.itemCount).map { robot.grid.findViewHolderForAdapterPosition(it)?.itemView?.tag }

    private fun arrange() {
        ArrangementStore(robot.context).save(Arrangement(order = listOf("app.n05", "app.n01"), hidden = listOf("app.n03")))
    }

    private fun assertNoSlide() {
        robot.idle()
        assertEquals(listOf("app.n05", "app.n01"), order.take(2))
        assertEquals(0, adapter.indexOf("app.n05"))
        assertTrue("no tile is on its way to a new place", (0 until adapter.itemCount).all {
            val tile = robot.grid.findViewHolderForAdapterPosition(it)?.itemView
            tile == null || (tile.translationX == 0f && tile.translationY == 0f)
        })
        assertEquals("app.n05", robot.focused?.tag)
    }

    // arrange-mode#AC-8: Back from arrange mode.
    @Test
    fun `AC-8 back from arrange mode the new order is there before the first frame and nothing slides`() {
        robot.controller.pause().stop()
        robot.idle()
        arrange()

        robot.controller.restart().start().resume()

        // Before anything posted runs, so before the screen is drawn: the old order is never shown.
        assertEquals(0, adapter.indexOf("app.n05"))
        assertEquals(-1, adapter.indexOf("app.n03"))
        assertNoSlide()
    }

    // arrange-mode#AC-8: Home from arrange mode.
    @Test
    fun `AC-8 Home from arrange mode the new order is there before the first frame and nothing slides`() {
        robot.controller.pause().stop()
        robot.idle()
        arrange()

        robot.controller.newIntent(Intent(robot.context, HomeActivity::class.java).putExtra(HomeActivity.EXTRA_HOME, true))
        robot.controller.restart().start().resume()

        assertEquals(0, adapter.indexOf("app.n05"))
        assertEquals(-1, adapter.indexOf("app.n03"))
        assertNoSlide()
    }
}
