package quiet.launcher

import android.content.Context
import android.content.Intent
import android.os.Looper
import android.view.KeyEvent
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-land-xhdpi")
class HomeResetTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val pm = context.packageManager

    @Before
    fun installApps() {
        // 20 apps and the Play Store: more than fit on the screen, so the grid can scroll.
        for (i in 1..20) TvPackages.install(pm, "app.n%02d".format(i), "App %02d".format(i), installed = i.toLong())
        TvPackages.install(pm, AppCatalog.PLAY_STORE, "Google Play Store", installed = 0)
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun open(): ActivityController<HomeActivity> =
        Robolectric.buildActivity(HomeActivity::class.java).setup().also { idle() }

    private fun grid(a: HomeActivity) = a.findViewById<RecyclerView>(R.id.app_grid)

    private fun focused(a: HomeActivity) = a.window.decorView.findFocus()?.tag

    private fun homePress() = Intent(context, HomeActivity::class.java).putExtra(HomeActivity.EXTRA_HOME, true)

    // home-takeover#AC-4
    @Test
    fun `AC-4 a Home press goes back to the top with the first app selected`() {
        val controller = open()
        val a = controller.get()
        val last = grid(a).adapter!!.itemCount - 1
        grid(a).scrollToPosition(last)
        idle()
        grid(a).findViewHolderForAdapterPosition(last)!!.itemView.requestFocus()
        assertNotEquals(0, grid(a).computeVerticalScrollOffset())

        controller.newIntent(homePress())
        idle()

        assertEquals("app.n01", focused(a))
        assertEquals(0, grid(a).computeVerticalScrollOffset())
    }

    // home-takeover#AC-4: an intent that is not a Home press (for example a relaunch) keeps the selection.
    @Test
    fun `AC-4 an intent without the Home mark keeps the selection`() {
        val controller = open()
        val a = controller.get()
        grid(a).findViewHolderForAdapterPosition(2)!!.itemView.requestFocus()

        controller.newIntent(Intent(context, HomeActivity::class.java))
        idle()

        assertEquals("app.n03", focused(a))
    }

    // home-takeover#AC-5
    @Test
    fun `AC-5 Back does nothing on the quiet home screen`() {
        val a = open().get()

        a.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK))
        a.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK))
        @Suppress("DEPRECATION")
        a.onBackPressed()
        idle()

        assertFalse(a.isFinishing)
        assertFalse(a.isDestroyed)
    }
}
