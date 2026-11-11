package quiet.launcher

import org.junit.Assert.assertEquals
import org.junit.Test

/** docs/specs/arranging-store.md: the home screen's list built from the arrangement. */
class ArrangedListTest {

    private fun entry(pkg: String, installed: Long) = AppEntry(pkg, "$pkg.Main", pkg.uppercase(), installed, false)

    /** The list AppCatalog.visible gives: install order, the Play Store last. */
    private val usual = listOf(
        entry("youtube", 10), entry("netflix", 20), entry("disney", 30), entry("prime", 40), entry("tubi", 50),
        entry(AppCatalog.PLAY_STORE, 0),
    )

    private fun shown(arrangement: Arrangement?, apps: List<AppEntry> = usual) =
        AppCatalog.arranged(apps, arrangement).shown.map { it.packageName }

    private fun hidden(arrangement: Arrangement?, apps: List<AppEntry> = usual) =
        AppCatalog.arranged(apps, arrangement).hidden.map { it.packageName }

    // arranging-store#AC-1
    @Test
    fun `AC-1 with no arrangement the list is the usual one and nothing is hidden`() {
        assertEquals(listOf("youtube", "netflix", "disney", "prime", "tubi", AppCatalog.PLAY_STORE), shown(null))
        assertEquals(emptyList<String>(), hidden(null))
    }

    // arranging-store#AC-2
    @Test
    fun `AC-2 listed apps come first in the arrangement's order, then the rest in their usual order`() {
        val a = Arrangement(order = listOf("prime", AppCatalog.PLAY_STORE, "youtube"), hidden = emptyList())

        assertEquals(listOf("prime", AppCatalog.PLAY_STORE, "youtube", "netflix", "disney", "tubi"), shown(a))
    }

    // arranging-store#AC-2: a new app goes at the end, after the Play Store once that is arranged.
    @Test
    fun `AC-2 a newly installed app goes at the end`() {
        val a = Arrangement(order = listOf("youtube", "netflix", "disney", "prime", "tubi", AppCatalog.PLAY_STORE), hidden = emptyList())

        assertEquals(
            listOf("youtube", "netflix", "disney", "prime", "tubi", AppCatalog.PLAY_STORE, "newapp"),
            shown(a, usual + entry("newapp", 99)),
        )
    }

    // arranging-store#AC-2
    @Test
    fun `AC-2 an uninstalled app is skipped and the others keep their order`() {
        val a = Arrangement(order = listOf("prime", "netflix", "youtube"), hidden = emptyList())
        val withoutNetflix = usual.filter { it.packageName != "netflix" }

        assertEquals(listOf("prime", "youtube", "disney", "tubi", AppCatalog.PLAY_STORE), shown(a, withoutNetflix))
    }

    // arranging-store#AC-3
    @Test
    fun `AC-3 hidden apps are left out and returned in the arrangement's order`() {
        val a = Arrangement(order = listOf("prime", "tubi", "youtube"), hidden = listOf("tubi", "disney"))

        assertEquals(listOf("prime", "youtube", "netflix", AppCatalog.PLAY_STORE), shown(a))
        assertEquals(listOf("tubi", "disney"), hidden(a))
    }

    // arranging-store#AC-3
    @Test
    fun `AC-3 a hidden app that is no longer installed is left out`() {
        val a = Arrangement(order = emptyList(), hidden = listOf("gone", "tubi"))

        assertEquals(listOf("tubi"), hidden(a))
        assertEquals(listOf("youtube", "netflix", "disney", "prime", AppCatalog.PLAY_STORE), shown(a))
    }

    // arranging-store#AC-4
    @Test
    fun `AC-4 with no shown app installed, the hidden ones are shown`() {
        val a = Arrangement(order = listOf("gone1", "gone2"), hidden = listOf("tubi", "disney"))
        val onlyHidden = usual.filter { it.packageName in setOf("tubi", "disney") }

        assertEquals(listOf("tubi", "disney"), shown(a, onlyHidden))
        assertEquals(emptyList<String>(), hidden(a, onlyHidden))
    }
}
