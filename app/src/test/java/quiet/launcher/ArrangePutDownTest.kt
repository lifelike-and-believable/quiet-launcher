package quiet.launcher

import android.content.Context
import android.content.Intent
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
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
 * docs/specs/arrange-mode.md, AC-6: putting a moving app down, or back, ends its lifted look. The
 * project lead's report (2026-11-11): after select put the app down, nothing showed it was no longer
 * being moved, because the selected tile kept the dashed outline and the lifted size.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-land-xhdpi")
class ArrangePutDownTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var controller: ActivityController<ArrangeActivity>
    private val activity get() = controller.get()
    private val grid get() = activity.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.arrange_grid)

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

    private fun frame(pkg: String): FrameLayout {
        val tile = (0 until grid.childCount).map { grid.getChildAt(it) }.first { it.tag == pkg }
        return tile.findViewById<View>(R.id.app_art).parent as FrameLayout
    }

    /** The moving look: the dashed outline drawn over the tile, and the tile enlarged to 1.1. */
    private fun lifted(pkg: String): Boolean {
        val f = frame(pkg)
        return f.foreground?.javaClass?.simpleName == "DashedRing" || f.scaleX >= 1.1f
    }

    private fun startMove(pkg: String) {
        open(select = pkg)
        key(KeyEvent.KEYCODE_DPAD_CENTER) // the menu
        key(KeyEvent.KEYCODE_DPAD_CENTER) // Move
        assertTrue("lifted while moving", lifted(pkg))
    }

    // arrange-mode#AC-6
    @Test
    fun `AC-6 select puts the app down and it is no longer lifted, still selected`() {
        startMove("app.n02")
        key(KeyEvent.KEYCODE_DPAD_RIGHT)
        key(KeyEvent.KEYCODE_DPAD_CENTER)

        assertEquals("app.n02", activity.window.decorView.findFocus()?.tag)
        assertFalse("still lifted after it was put down", lifted("app.n02"))
    }

    // arrange-mode#AC-6
    @Test
    fun `AC-6 Back puts the app back and it is no longer lifted`() {
        startMove("app.n02")
        key(KeyEvent.KEYCODE_DPAD_RIGHT)
        key(KeyEvent.KEYCODE_BACK)

        assertFalse("still lifted after it was put back", lifted("app.n02"))
    }
}
