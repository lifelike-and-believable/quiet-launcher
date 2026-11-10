package quiet.launcher

import android.content.Context
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Looper
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-land-xhdpi")
class ArtworkCacheTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val pm = context.packageManager

    // home-takeover#AC-12
    @Test
    fun `AC-12 artwork is loaded once per app and again only when the app is updated`() {
        TvPackages.install(pm, "com.netflix.ninja", "Netflix", installed = 1000)
        TvPackages.install(pm, "com.google.android.youtube.tv", "YouTube", installed = 500)
        val loaded = mutableListOf<String>()
        val source = PackageSource(pm) { activity -> loaded += activity.packageName; ColorDrawable(0) }

        source.read()
        val firstRead = loaded.toList()
        source.read()
        source.read()

        assertEquals("nothing loaded again while nothing changed", firstRead, loaded)

        TvPackages.install(pm, "com.netflix.ninja", "Netflix", installed = 1000, updated = 2000)
        source.read()

        assertEquals(firstRead + "com.netflix.ninja", loaded)
    }

    // home-takeover#AC-12: shown again, the screen draws what it has, with the Home selection, and
    // only then reads the apps again.
    @Test
    fun `AC-12 a Home press shows the list it has, selected, before reading the apps again`() {
        TvPackages.install(pm, "com.google.android.youtube.tv", "YouTube", installed = 100)
        TvPackages.install(pm, "com.netflix.ninja", "Netflix", installed = 200)
        val controller = Robolectric.buildActivity(HomeActivity::class.java).setup()
        shadowOf(Looper.getMainLooper()).idle()
        val a = controller.get()
        val grid = a.findViewById<RecyclerView>(R.id.app_grid)
        grid.getChildAt(1).requestFocus()

        TvPackages.install(pm, "tv.plex", "Plex", installed = 300)
        controller.newIntent(Intent(context, HomeActivity::class.java).putExtra(HomeActivity.EXTRA_HOME, true))

        // Before the main thread runs anything else: the old list, with the first app selected.
        val labels = (0 until grid.childCount).map { grid.getChildAt(it).findViewById<TextView>(R.id.app_label).text.toString() }
        assertEquals(listOf("YouTube", "Netflix"), labels)
        assertEquals("com.google.android.youtube.tv", a.window.decorView.findFocus()?.tag)

        // Then the apps are read again.
        shadowOf(Looper.getMainLooper()).idle()
        val after = (0 until grid.childCount).map { grid.getChildAt(it).findViewById<TextView>(R.id.app_label).text.toString() }
        assertEquals(listOf("YouTube", "Netflix", "Plex"), after)
    }
}
