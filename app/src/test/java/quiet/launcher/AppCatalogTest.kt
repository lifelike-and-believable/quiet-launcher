package quiet.launcher

import org.junit.Assert.assertEquals
import org.junit.Test

class AppCatalogTest {

    private fun entry(pkg: String, label: String, installed: Long, home: Boolean = false) =
        AppEntry(pkg, "$pkg.Main", label, installed, home)

    // app-list#AC-1
    @Test
    fun `AC-1 leaves out this app, home screens, and three Google system apps, and keeps the Play Store`() {
        val entries = listOf(
            entry("com.netflix.ninja", "Netflix", 2),
            entry("quiet.launcher", "Quiet Launcher", 3),
            entry("com.spocky.projengmenu", "Projectivy Launcher", 4, home = true),
            entry("com.android.tv.settings", "Settings", 0),
            entry("com.google.android.play.games", "Play Games", 0),
            entry("com.google.android.videos", "Google TV", 0),
            entry("com.android.vending", "Google Play Store", 0),
        )

        val shown = AppCatalog.visible(entries, ownPackage = "quiet.launcher").map { it.packageName }

        assertEquals(listOf("com.netflix.ninja", "com.android.vending"), shown)
    }

    // app-list#AC-2
    @Test
    fun `AC-2 orders by install time, ties by label ignoring case, Play Store last`() {
        val entries = listOf(
            entry("com.android.vending", "Google Play Store", 0),
            entry("com.netflix.ninja", "Netflix", 20),
            entry("com.google.android.youtube.tv", "YouTube", 10),
            entry("b.zeta", "zeta", 5),
            entry("a.alpha", "Alpha", 5),
            entry("c.beta", "beta", 5),
        )

        val shown = AppCatalog.visible(entries, ownPackage = "quiet.launcher").map { it.label }

        assertEquals(listOf("Alpha", "beta", "zeta", "YouTube", "Netflix", "Google Play Store"), shown)
    }

    // app-list#AC-2: the Play Store is last even though its factory install time is the oldest.
    @Test
    fun `AC-2 a newly installed app goes before the Play Store`() {
        val entries = listOf(
            entry("com.android.vending", "Google Play Store", 0),
            entry("com.google.android.youtube.tv", "YouTube", 10),
            entry("new.app", "New App", 99),
        )

        val shown = AppCatalog.visible(entries, ownPackage = "quiet.launcher").map { it.label }

        assertEquals(listOf("YouTube", "New App", "Google Play Store"), shown)
    }
}
