package quiet.launcher

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Looper
import android.text.TextUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import android.view.View

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-land-xhdpi")
class HomeActivityTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val pm = context.packageManager

    @Before
    fun installApps() {
        TvPackages.install(pm, "com.google.android.youtube.tv", "YouTube", installed = 100)
        TvPackages.install(pm, "com.netflix.ninja", "Netflix", installed = 200)
        TvPackages.install(pm, AppCatalog.PLAY_STORE, "Google Play Store", installed = 0)
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun open(): ActivityController<HomeActivity> =
        Robolectric.buildActivity(HomeActivity::class.java).setup().also { idle() }

    private fun grid(a: HomeActivity) = a.findViewById<RecyclerView>(R.id.app_grid)

    private fun tiles(a: HomeActivity): List<View> = grid(a).let { g -> (0 until g.childCount).map { g.getChildAt(it) } }

    private fun labels(a: HomeActivity) = tiles(a).map { it.findViewById<TextView>(R.id.app_label).text.toString() }

    private fun broadcast(action: String, pkg: String) {
        context.sendBroadcast(Intent(action, Uri.parse("package:$pkg")))
        idle()
    }

    // app-list#AC-4
    @Test
    fun `AC-4 tiles show artwork and label in a five-column grid, labels on at most two lines`() {
        val a = open().get()

        assertEquals(5, (grid(a).layoutManager as GridLayoutManager).spanCount)
        assertEquals(listOf("YouTube", "Netflix", "Google Play Store"), labels(a))
        for (tile in tiles(a)) {
            val label = tile.findViewById<TextView>(R.id.app_label)
            assertEquals(2, label.maxLines)
            assertEquals(TextUtils.TruncateAt.END, label.ellipsize)
            assertNotNull(tile.findViewById<ImageView>(R.id.app_art).drawable)
        }
    }

    // app-list#AC-5
    @Test
    fun `AC-5 selecting a tile starts that app's TV launch activity in a new task`() {
        val a = open().get()

        tiles(a)[1].performClick()

        val started = shadowOf(a).nextStartedActivity
        assertEquals("com.netflix.ninja", started.component?.packageName)
        assertEquals("com.netflix.ninja.Main", started.component?.className)
        assertEquals(Intent.ACTION_MAIN, started.action)
        assertTrue(started.hasCategory(Intent.CATEGORY_LEANBACK_LAUNCHER))
        assertTrue(started.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }

    // app-list#AC-6
    @Test
    fun `AC-6 when the screen opens the first app is selected and the grid is at the top`() {
        val a = open().get()

        assertEquals("com.google.android.youtube.tv", a.currentFocus?.tag)
        assertEquals(0, grid(a).computeVerticalScrollOffset())
    }

    // app-list#AC-7
    @Test
    fun `AC-7 an installed app appears before the Play Store, and a removed one disappears`() {
        val a = open().get()

        TvPackages.install(pm, "tv.plex", "Plex", installed = 300)
        broadcast(Intent.ACTION_PACKAGE_ADDED, "tv.plex")
        assertEquals(listOf("YouTube", "Netflix", "Plex", "Google Play Store"), labels(a))

        TvPackages.remove(pm, "com.netflix.ninja")
        broadcast(Intent.ACTION_PACKAGE_REMOVED, "com.netflix.ninja")
        assertEquals(listOf("YouTube", "Plex", "Google Play Store"), labels(a))
    }

    // app-list#AC-7
    @Test
    fun `AC-7 the grid is rebuilt when the screen is shown again`() {
        val controller = open()

        controller.pause().stop()
        TvPackages.install(pm, "tv.plex", "Plex", installed = 300)
        controller.restart().start().resume()
        idle()

        assertEquals(listOf("YouTube", "Netflix", "Plex", "Google Play Store"), labels(controller.get()))
    }

    // app-list#AC-7: a rebuild keeps the selection, or moves it to the app now in its place.
    @Test
    fun `AC-7 a rebuild keeps the selection on the same app, or the next one if it was removed`() {
        val a = open().get()
        tiles(a)[1].requestFocus()
        assertEquals("com.netflix.ninja", a.currentFocus?.tag)

        TvPackages.install(pm, "tv.plex", "Plex", installed = 300)
        broadcast(Intent.ACTION_PACKAGE_ADDED, "tv.plex")
        assertEquals("com.netflix.ninja", a.currentFocus?.tag)

        TvPackages.remove(pm, "com.netflix.ninja")
        broadcast(Intent.ACTION_PACKAGE_REMOVED, "com.netflix.ninja")
        assertEquals("tv.plex", a.currentFocus?.tag)
    }

    // app-list#AC-8
    @Test
    fun `AC-8 with no apps the screen says so`() {
        TvPackages.remove(pm, "com.google.android.youtube.tv")
        TvPackages.remove(pm, "com.netflix.ninja")
        TvPackages.remove(pm, AppCatalog.PLAY_STORE)

        val a = open().get()

        val message = a.findViewById<TextView>(R.id.empty_message)
        assertEquals(View.VISIBLE, message.visibility)
        assertEquals("No apps to show. Apps installed on this TV appear here.", message.text.toString())
        assertEquals(0, grid(a).childCount)
    }

    // app-list#AC-8: the message is hidden while there are apps.
    @Test
    fun `AC-8 the message is hidden when there are apps`() {
        val a = open().get()

        assertEquals(View.GONE, a.findViewById<TextView>(R.id.empty_message).visibility)
    }
}
