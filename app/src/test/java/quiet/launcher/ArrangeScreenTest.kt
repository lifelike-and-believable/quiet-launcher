package quiet.launcher

import android.content.Context
import android.content.Intent
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import java.time.Duration

/** docs/specs/arrange-mode.md: arrange mode, driven as the remote would. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-land-xhdpi")
class ArrangeScreenTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val store = ArrangementStore(context)
    private lateinit var controller: ActivityController<ArrangeActivity>
    private val activity get() = controller.get()

    @Before
    fun installApps() {
        for (i in 1..7) TvPackages.install(context.packageManager, "app.n%02d".format(i), "App %02d".format(i), installed = i.toLong())
    }

    private fun open(select: String = "app.n01") {
        controller = Robolectric.buildActivity(
            ArrangeActivity::class.java,
            Intent(context, ArrangeActivity::class.java).putExtra(ArrangeActivity.EXTRA_SELECT, select),
        ).setup()
        idle()
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()
    private fun advance(ms: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))

    private fun key(code: Int) {
        activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))
        activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
        idle()
    }

    private fun text(id: Int) = activity.findViewById<TextView>(id).text.toString()
    private fun shown(id: Int) = activity.findViewById<View>(id)?.visibility == View.VISIBLE
    private val grid get() = activity.findViewById<RecyclerView>(R.id.arrange_grid)
    private val focused get() = activity.window.decorView.findFocus()

    /** The packages of the app tiles, in order, as drawn. */
    private fun tiles(): List<String> = (0 until grid.adapter!!.itemCount)
        .mapNotNull { grid.findViewHolderForAdapterPosition(it)?.itemView?.tag as? String }

    private fun tile(pkg: String): View = (0 until grid.childCount).map { grid.getChildAt(it) }.first { it.tag == pkg }

    /** Where the direction pad moves the selection; Robolectric does not move it on a dispatched key. */
    private fun move(direction: Int) {
        focused?.focusSearch(direction)?.requestFocus()
        idle()
    }

    private fun select(pkg: String) {
        tile(pkg).requestFocus()
        idle()
    }

    // arrange-mode#AC-4
    @Test
    fun `AC-4 arrange mode shows the heading, instructions, count, apps in order, and the hidden apps`() {
        store.save(Arrangement(order = listOf("app.n03"), hidden = listOf("app.n05", "app.n02")))

        open(select = "app.n04")

        assertEquals("Arranging apps", text(R.id.arrange_title))
        assertEquals("Select an app to move or hide it. Press Back when you are done; changes are saved as you go.", text(R.id.arrange_instructions))
        assertEquals("2 hidden apps, at the end", text(R.id.arrange_count))
        assertNull(activity.findViewById(R.id.search_box))
        assertEquals("Hidden apps", text(R.id.hidden_label))
        assertEquals(listOf("app.n03", "app.n01", "app.n04", "app.n06", "app.n07", "app.n05", "app.n02"), tiles())
        assertTrue(tile("app.n05").findViewById<View>(R.id.hidden_badge).visibility == View.VISIBLE)
        assertEquals("app.n04", focused?.tag)
    }

    // arrange-mode#AC-4
    @Test
    fun `AC-4 with no hidden apps the row says how to hide one`() {
        open()

        assertEquals("No hidden apps", text(R.id.arrange_count))
        assertEquals("No hidden apps. To hide one, select it and choose Hide.", text(R.id.no_hidden))
    }

    // arrange-mode#AC-5
    @Test
    fun `AC-5 select on an app opens Move, Hide, Cancel with Move selected, and Up and Down stay inside`() {
        open()

        key(KeyEvent.KEYCODE_DPAD_CENTER)

        assertTrue(shown(R.id.arrange_menu))
        assertTrue(shown(R.id.menu_move) && shown(R.id.menu_hide) && shown(R.id.menu_cancel))
        assertFalse(shown(R.id.menu_show))
        assertEquals(R.id.menu_move, focused?.id)
        move(View.FOCUS_UP)
        assertEquals(R.id.menu_move, focused?.id)
        move(View.FOCUS_DOWN)
        move(View.FOCUS_DOWN)
        move(View.FOCUS_DOWN)
        assertEquals(R.id.menu_cancel, focused?.id)
    }

    // arrange-mode#AC-5
    @Test
    fun `AC-5 Cancel and Back close the menu with nothing changed`() {
        open()
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        key(KeyEvent.KEYCODE_BACK)
        assertFalse(shown(R.id.arrange_menu))
        assertEquals("app.n01", focused?.tag)
        assertFalse(activity.isFinishing)

        key(KeyEvent.KEYCODE_DPAD_CENTER)
        activity.findViewById<View>(R.id.menu_cancel).requestFocus()
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        assertFalse(shown(R.id.arrange_menu))
        assertNull(store.load())
    }

    // arrange-mode#AC-5
    @Test
    fun `AC-5 a hidden app's menu has Show and Cancel`() {
        store.save(Arrangement(order = emptyList(), hidden = listOf("app.n07")))
        open()
        select("app.n07")

        key(KeyEvent.KEYCODE_DPAD_CENTER)

        assertTrue(shown(R.id.menu_show) && shown(R.id.menu_cancel))
        assertFalse(shown(R.id.menu_move) || shown(R.id.menu_hide))
    }

    // arrange-mode#AC-5
    @Test
    fun `AC-5 the last app on the home screen has no Hide, and the status line says why`() {
        store.save(Arrangement(order = emptyList(), hidden = (2..7).map { "app.n%02d".format(it) }))
        open()

        key(KeyEvent.KEYCODE_DPAD_CENTER)

        assertFalse(shown(R.id.menu_hide))
        assertTrue(shown(R.id.menu_move))
        assertEquals("Keep at least one app on the home screen.", text(R.id.status_line))
    }

    // arrange-mode#AC-6
    @Test
    fun `AC-6 Move lifts the app, the arrows move it, and select puts it down and saves`() {
        open(select = "app.n02")
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        key(KeyEvent.KEYCODE_DPAD_CENTER) // Move

        assertEquals("Moving App 02. Use the arrows, then press select to put it here. Back puts it back.", text(R.id.status_line))
        key(KeyEvent.KEYCODE_DPAD_RIGHT)
        key(KeyEvent.KEYCODE_DPAD_RIGHT)
        assertEquals("app.n02", tiles()[3])
        assertNull(store.load()) // nothing saved until it is put down
        key(KeyEvent.KEYCODE_DPAD_CENTER)

        assertEquals("app.n02", store.load()!!.order[3])
        assertEquals("app.n02", focused?.tag)
        assertEquals("", text(R.id.status_line))
    }

    // arrange-mode#AC-6
    @Test
    fun `AC-6 Back while moving puts the app back and saves nothing`() {
        open(select = "app.n02")
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        key(KeyEvent.KEYCODE_DPAD_DOWN)

        key(KeyEvent.KEYCODE_BACK)

        assertEquals("app.n02", tiles()[1])
        assertNull(store.load())
        assertFalse(activity.isFinishing)
    }

    // arrange-mode#AC-7
    @Test
    fun `AC-7 Hide moves the app to the hidden apps, says so, selects the next app, and saves`() {
        open(select = "app.n03")
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        activity.findViewById<View>(R.id.menu_hide).requestFocus()
        key(KeyEvent.KEYCODE_DPAD_CENTER)

        assertEquals("App 03 hidden. It is in the Hidden apps row.", text(R.id.status_line))
        assertEquals(listOf("app.n03"), store.load()!!.hidden)
        assertEquals("app.n04", focused?.tag)
        assertEquals("1 hidden app, at the end", text(R.id.arrange_count))
        assertEquals("app.n03", tiles().last())
    }

    // arrange-mode#AC-7
    @Test
    fun `AC-7 Show moves a hidden app to the end of the apps, says so, and saves`() {
        store.save(Arrangement(order = emptyList(), hidden = listOf("app.n01")))
        open(select = "app.n02")
        select("app.n01")
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        key(KeyEvent.KEYCODE_DPAD_CENTER) // Show

        assertEquals("App 01 shown, at the end of the apps.", text(R.id.status_line))
        assertEquals(emptyList<String>(), store.load()!!.hidden)
        assertEquals("app.n01", store.load()!!.order.last())
    }

    // arrange-mode#AC-8
    @Test
    fun `AC-8 Back with nothing open leaves arrange mode`() {
        open()

        key(KeyEvent.KEYCODE_BACK)

        assertTrue(activity.isFinishing)
    }

    // arrange-mode#AC-8
    @Test
    fun `AC-8 two minutes with no button pressed closes arrange mode and puts a moving app back`() {
        open(select = "app.n02")
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        key(KeyEvent.KEYCODE_DPAD_RIGHT)
        advance(119_000)
        assertFalse(activity.isFinishing)

        advance(2_000)

        assertTrue(activity.isFinishing)
        assertNull(store.load())
    }

    // arrange-mode#AC-9
    @Test
    fun `AC-9 TalkBack hears the instructions, hidden apps, messages, and each step of a move`() {
        store.save(Arrangement(order = emptyList(), hidden = listOf("app.n07")))
        open(select = "app.n02")

        assertEquals(
            "Arranging apps. Select an app to move or hide it. Press Back when you are done. 1 hidden app, at the end.",
            activity.lastAnnouncement,
        )
        assertEquals("App 07, hidden app", tile("app.n07").contentDescription)
        assertEquals(View.ACCESSIBILITY_LIVE_REGION_POLITE, activity.findViewById<View>(R.id.status_line).accessibilityLiveRegion)
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        key(KeyEvent.KEYCODE_DPAD_CENTER)
        key(KeyEvent.KEYCODE_DPAD_RIGHT)
        assertEquals("App 02, 3 of 6", activity.lastAnnouncement)
    }
}
