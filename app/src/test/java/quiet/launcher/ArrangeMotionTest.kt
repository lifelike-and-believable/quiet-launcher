package quiet.launcher

import android.content.Context
import android.content.Intent
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

/**
 * docs/specs/arrange-mode.md, AC-6 and AC-7: apps slide only while one is being moved. The project
 * lead, 2026-11-11: "move animation should only play while you're actually moving the apps".
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-land-xhdpi")
class ArrangeMotionTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val store = ArrangementStore(context)
    private lateinit var controller: ActivityController<ArrangeActivity>
    private val activity get() = controller.get()
    private val grid get() = activity.findViewById<RecyclerView>(R.id.arrange_grid)

    @Before
    fun installApps() {
        for (i in 1..7) TvPackages.install(context.packageManager, "app.n%02d".format(i), "App %02d".format(i), installed = i.toLong())
    }

    private fun open(select: String) {
        controller = Robolectric.buildActivity(
            ArrangeActivity::class.java,
            Intent(context, ArrangeActivity::class.java).putExtra(ArrangeActivity.EXTRA_SELECT, select),
        ).setup()
        idle()
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun key(code: Int) {
        activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))
        activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
        idle()
    }

    /**
     * Presses [code] and runs the main thread only until the grid has been laid out: a slide has
     * then started and not finished (running everything would finish it at once).
     */
    private fun slides(code: Int): Boolean {
        activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))
        activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
        val looper = shadowOf(Looper.getMainLooper())
        var tasks = 0
        while (grid.isLayoutRequested && !looper.isIdle && tasks++ < 20) looper.runOneTask()
        val sliding = grid.isAnimating
        idle()
        return sliding
    }

    private fun moveApp() {
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        key(KeyEvent.KEYCODE_DPAD_CENTER) // Move
    }

    // arrange-mode#AC-6: each step of a move slides.
    @Test
    fun `AC-6 a step while moving slides the apps`() {
        open(select = "app.n02")
        moveApp()

        assertTrue(slides(KeyEvent.KEYCODE_DPAD_RIGHT))
    }

    // arrange-mode#AC-6: Back slides the app back to its place.
    @Test
    fun `AC-6 Back while moving slides the app back`() {
        open(select = "app.n02")
        moveApp()
        key(KeyEvent.KEYCODE_DPAD_RIGHT)
        key(KeyEvent.KEYCODE_DPAD_RIGHT)

        assertTrue(slides(KeyEvent.KEYCODE_BACK))
    }

    // arrange-mode#AC-6: putting it down changes no places, and nothing slides afterwards.
    @Test
    fun `AC-6 putting the app down does not slide`() {
        open(select = "app.n02")
        moveApp()
        key(KeyEvent.KEYCODE_DPAD_RIGHT)

        assertFalse(slides(KeyEvent.KEYCODE_DPAD_CENTER))
    }

    // arrange-mode#AC-7: Hide draws the new places at once.
    @Test
    fun `AC-7 Hide does not slide the apps`() {
        open(select = "app.n03")
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        activity.findViewById<View>(R.id.menu_hide).requestFocus()

        assertFalse(slides(KeyEvent.KEYCODE_DPAD_CENTER))
    }

    // arrange-mode#AC-7: Show draws the new places at once.
    @Test
    fun `AC-7 Show does not slide the apps`() {
        store.save(Arrangement(order = emptyList(), hidden = listOf("app.n01")))
        open(select = "app.n01")
        key(KeyEvent.KEYCODE_DPAD_CENTER)

        assertFalse(slides(KeyEvent.KEYCODE_DPAD_CENTER)) // Show
    }
}
