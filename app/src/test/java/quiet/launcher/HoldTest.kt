package quiet.launcher

import android.view.KeyEvent
import android.view.View
import android.widget.TextView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** docs/specs/arrange-mode.md, AC-1 to AC-3 and AC-8: holding select on the home screen. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-land-xhdpi")
class HoldTest {

    private val robot = HomeScreenRobot(appCount = 4)
    private val question get() = robot.activity.findViewById<View>(R.id.arrange_question)

    private fun down() { robot.activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER)); robot.idle() }
    private fun repeat(n: Int) {
        robot.activity.dispatchKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER, n)); robot.idle()
    }
    private fun up() { robot.activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_CENTER)); robot.idle() }

    /** Holds select for [ms], with the key repeats a remote sends. */
    private fun hold(ms: Long) {
        down()
        var held = 0L
        var n = 1
        while (held < ms) { robot.advance(100); held += 100; repeat(n++) }
    }

    // arrange-mode#AC-1
    @Test
    fun `AC-1 holding select for less than 5 seconds opens the app and does not ask`() {
        hold(4_000)
        up()

        assertEquals("app.n01", robot.nextStarted()?.component?.packageName)
        assertFalse(question?.visibility == View.VISIBLE)
    }

    // arrange-mode#AC-2
    @Test
    fun `AC-2 holding select on an app for 5 seconds asks, with Cancel selected, and letting go opens nothing`() {
        hold(5_000)

        assertEquals(View.VISIBLE, question.visibility)
        assertEquals("Arrange apps?", robot.activity.findViewById<TextView>(R.id.question_title).text.toString())
        assertEquals(
            "You can change the order of the apps and hide the ones nobody uses. The changes are for everyone who uses this TV.",
            robot.activity.findViewById<TextView>(R.id.question_text).text.toString(),
        )
        assertEquals(R.id.question_cancel, robot.focused?.id)
        up()
        assertNull(robot.nextStarted())
    }

    // arrange-mode#AC-2
    @Test
    fun `AC-2 holding select on a top-row control does not ask`() {
        robot.settings.requestFocus()
        robot.idle()

        hold(6_000)

        assertFalse(question?.visibility == View.VISIBLE)
    }

    // arrange-mode#AC-3
    @Test
    fun `AC-3 Cancel and Back close the question with nothing changed`() {
        hold(5_000); up()
        robot.key(KeyEvent.KEYCODE_DPAD_CENTER) // Cancel

        assertEquals(View.GONE, question.visibility)
        assertEquals("app.n01", robot.focused?.tag)
        assertNull(robot.nextStarted())

        hold(5_000); up()
        robot.key(KeyEvent.KEYCODE_BACK)
        assertEquals(View.GONE, question.visibility)
        assertNull(ArrangementStore(robot.context).load())
    }

    // arrange-mode#AC-3
    @Test
    fun `AC-3 left and right stay on the question's two buttons, and Arrange apps opens arrange mode`() {
        hold(5_000); up()
        robot.key(KeyEvent.KEYCODE_DPAD_LEFT)
        assertEquals(R.id.question_cancel, robot.focused?.id)
        robot.key(KeyEvent.KEYCODE_DPAD_RIGHT)
        assertEquals(R.id.question_arrange, robot.focused?.id)
        robot.key(KeyEvent.KEYCODE_DPAD_RIGHT)
        assertEquals(R.id.question_arrange, robot.focused?.id)
        robot.key(KeyEvent.KEYCODE_DPAD_UP)
        assertEquals(R.id.question_arrange, robot.focused?.id)

        robot.key(KeyEvent.KEYCODE_DPAD_CENTER)

        val started = robot.nextStarted()
        assertNotNull(started)
        assertEquals(ArrangeActivity::class.java.name, started!!.component?.className)
        assertEquals("app.n01", started.getStringExtra(ArrangeActivity.EXTRA_SELECT))
        assertEquals(View.GONE, question.visibility)
    }

    // arrange-mode#AC-8
    @Test
    fun `AC-8 back from arrange mode the home screen shows the new arrangement with the first app selected`() {
        robot.tile(2).requestFocus()
        robot.idle()
        hold(5_000); up()
        robot.key(KeyEvent.KEYCODE_DPAD_RIGHT)
        robot.key(KeyEvent.KEYCODE_DPAD_CENTER)
        ArrangementStore(robot.context).save(Arrangement(order = listOf("app.n04"), hidden = listOf("app.n02")))

        robot.leaveAndComeBack()

        assertEquals("app.n04", robot.focused?.tag)
        assertTrue((0 until robot.grid.adapter!!.itemCount).none { robot.grid.findViewHolderForAdapterPosition(it)?.itemView?.tag == "app.n02" })
    }
}
