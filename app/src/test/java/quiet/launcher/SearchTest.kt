package quiet.launcher

import android.content.Intent
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import android.provider.Settings

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-land-xhdpi")
class SearchTest {

    @After
    fun animationsBackOn() {
        Settings.Global.putFloat(
            org.robolectric.RuntimeEnvironment.getApplication().contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        )
    }

    private fun assertGoogleSearch(intent: Intent?, query: String?) {
        assertEquals(Intent.ACTION_ASSIST, intent?.action)
        assertEquals("com.google.android.katniss", intent?.`package`)
        assertEquals(query, intent?.getStringExtra("query"))
    }

    private fun HomeScreenRobot.overlayText(id: Int) = view<TextView>(id).text.toString()

    // top-row-and-search#AC-2
    @Test
    fun `AC-2 Search sends Google the text exactly as typed`() {
        val r = HomeScreenRobot()
        r.type("  the Office  ")

        r.pressSearch()

        assertGoogleSearch(r.nextStarted(), "  the Office  ")
    }

    // top-row-and-search#AC-2: a TV whose Google search app is older than 8.2 still has the trampoline.
    @Test
    fun `AC-2 without the current entry point Search uses the older GLOBAL_SEARCH one`() {
        val r = HomeScreenRobot(setUpSearch = { TvPackages.installGoogleSearch(it, assist = false, trampoline = true) })
        r.type("Severance")

        r.pressSearch()

        val started = r.nextStarted()
        assertEquals("android.search.action.GLOBAL_SEARCH", started?.action)
        assertEquals("com.google.android.katniss", started?.component?.packageName)
        assertEquals("Severance", started?.getStringExtra("query"))
    }

    // top-row-and-search#AC-2: with both, the current entry point comes first.
    @Test
    fun `AC-2 with both entry points Search uses the current one`() {
        val r = HomeScreenRobot(setUpSearch = { TvPackages.installGoogleSearch(it, assist = true, trampoline = true) })
        r.type("Severance")

        r.pressSearch()

        assertGoogleSearch(r.nextStarted(), "Severance")
    }

    // top-row-and-search#AC-2
    @Test
    fun `AC-2 Search with nothing typed sends nothing, and the box asks for a Search key`() {
        val r = HomeScreenRobot()
        r.type("")

        r.pressSearch()

        assertNull(r.nextStarted())
        assertEquals(View.GONE, r.overlay.visibility)
        assertEquals(EditorInfo.IME_ACTION_SEARCH, r.searchBox.imeOptions and EditorInfo.IME_MASK_ACTION)
    }

    // top-row-and-search#AC-3
    @Test
    fun `AC-3 while searching the overlay says so, the box stays selected, and only Back acts`() {
        val r = HomeScreenRobot()
        r.type("Severance")
        r.pressSearch()

        assertEquals(View.VISIBLE, r.overlay.visibility)
        assertEquals("Searching for “Severance”…", r.overlayText(R.id.search_overlay_title))
        assertEquals("Google's search will open in a moment. Back cancels.", r.overlayText(R.id.search_overlay_note))
        assertEquals(View.VISIBLE, r.view<View>(R.id.search_spinner).visibility)
        assertSame(r.searchBox, r.focused)

        r.key(KeyEvent.KEYCODE_DPAD_DOWN)
        r.key(KeyEvent.KEYCODE_DPAD_CENTER)
        assertSame(r.searchBox, r.focused)
        assertEquals(View.VISIBLE, r.overlay.visibility)

        r.key(KeyEvent.KEYCODE_BACK)
        assertEquals(View.GONE, r.overlay.visibility)
        assertEquals("Severance", r.searchBox.text.toString())
        assertSame(r.searchBox, r.focused)
    }

    // top-row-and-search#AC-3: with the TV's animations off ("remove animations" sets the animator
    // duration scale to 0) there is no spinner.
    @Test
    fun `AC-3 with animations off the overlay has no spinner`() {
        val r = HomeScreenRobot()
        Settings.Global.putFloat(r.context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        r.type("Severance")

        r.pressSearch()

        assertEquals(View.VISIBLE, r.overlay.visibility)
        assertEquals(View.GONE, r.view<View>(R.id.search_spinner).visibility)
    }

    // top-row-and-search#AC-4
    @Test
    fun `AC-4 when Google's search opens the overlay goes and the box is cleared, and it is selected on return`() {
        val r = HomeScreenRobot()
        r.type("Severance")
        r.pressSearch()

        r.leaveAndComeBack()

        assertEquals(View.GONE, r.overlay.visibility)
        assertEquals("", r.searchBox.text.toString())
        assertSame(r.searchBox, r.focused)
    }

    // top-row-and-search#AC-5
    @Test
    fun `AC-5 after 10 seconds without Google's search the text stays and the status line says so`() {
        val r = HomeScreenRobot()
        r.type("Severance")
        r.pressSearch()

        r.advance(9_500)
        assertEquals(View.VISIBLE, r.overlay.visibility)
        r.advance(600)

        assertEquals(View.GONE, r.overlay.visibility)
        assertEquals("Severance", r.searchBox.text.toString())
        assertSame(r.searchBox, r.focused)
        assertEquals("Google's search did not open. Press select to try again.", r.statusLine.text.toString())
    }

    // top-row-and-search#AC-5: with no entry point of Google's search on the TV, it says so at once.
    @Test
    fun `AC-5 without Google's search the text stays and the status line says so`() {
        val r = HomeScreenRobot(setUpSearch = { TvPackages.installGoogleSearch(it, assist = false, trampoline = false) })
        r.type("Severance")

        r.pressSearch()

        assertNull(r.nextStarted())
        assertEquals(View.GONE, r.overlay.visibility)
        assertEquals("Severance", r.searchBox.text.toString())
        assertSame(r.searchBox, r.focused)
        assertEquals("Google's search did not open. Press select to try again.", r.statusLine.text.toString())
    }

    // top-row-and-search#AC-6
    @Test
    fun `AC-6 Voice search sends Google's search with no text, and is selected on return`() {
        val r = HomeScreenRobot()
        r.voice.requestFocus()

        r.voice.performClick()
        assertGoogleSearch(r.nextStarted(), null)

        r.leaveAndComeBack()
        assertSame(r.voice, r.focused)
    }

    // top-row-and-search#AC-11
    @Test
    fun `AC-11 Back from the search box goes to the first app and never closes the screen`() {
        val r = HomeScreenRobot()
        r.searchBox.requestFocus()

        @Suppress("DEPRECATION")
        r.activity.onBackPressed()
        r.idle()

        assertEquals("app.n01", r.focused?.tag)
        assertFalse(r.activity.isFinishing)
    }

    // top-row-and-search#AC-11
    @Test
    fun `AC-11 a Home press clears the search box`() {
        val r = HomeScreenRobot()
        r.type("Sever")

        r.homePress()

        assertEquals("", r.searchBox.text.toString())
        assertEquals("app.n01", r.focused?.tag)
        assertTrue(r.activity.window.decorView.findFocus() !== r.searchBox)
    }

    /** Searches for [text] and lets Google's search screen open, as in AC-4. */
    private fun HomeScreenRobot.searchAndOpen(text: String) {
        type(text)
        pressSearch()
        nextStarted()
        leaveAndComeBack()
    }

    /** After a cover that is not retried, the screen acts as after a Home press. */
    private fun HomeScreenRobot.assertActsAsHome() {
        assertNull(nextStarted())
        assertEquals(View.GONE, overlay.visibility)
        assertEquals("", searchBox.text.toString())
        assertEquals("app.n01", focused?.tag)
    }

    // top-row-and-search#AC-14
    @Test
    fun `AC-14 Google TV's home screen covered soon after a search searches again once, with the overlay`() {
        val r = HomeScreenRobot()
        r.searchAndOpen("Severance")
        r.advance(8_000)

        r.covered()

        assertGoogleSearch(r.nextStarted(), "Severance")
        assertEquals(View.VISIBLE, r.overlay.visibility)
        assertEquals("Searching for “Severance”…", r.overlayText(R.id.search_overlay_title))
        assertEquals("Severance", r.searchBox.text.toString())
        assertSame(r.searchBox, r.focused)

        // Google's search screen opens again; Back comes back to an empty, selected box (AC-4).
        r.leaveAndComeBack()
        assertEquals(View.GONE, r.overlay.visibility)
        assertEquals("", r.searchBox.text.toString())
        assertSame(r.searchBox, r.focused)
    }

    // top-row-and-search#AC-14: never a second retry; Google TV's home screen reported again just
    // after the retry (seen on the TV) leaves the search box selected and empty for Back.
    @Test
    fun `AC-14 a cover just after the retry is not retried and does not reset the screen`() {
        val r = HomeScreenRobot()
        r.searchAndOpen("Severance")
        r.covered()
        r.nextStarted()
        r.leaveAndComeBack()

        r.covered()

        assertNull(r.nextStarted())
        assertEquals("", r.searchBox.text.toString())
        assertSame(r.searchBox, r.focused)
    }

    // top-row-and-search#AC-14: the time after the retry is bounded too.
    @Test
    fun `AC-14 a cover well after the retry acts as a Home press`() {
        val r = HomeScreenRobot()
        r.searchAndOpen("Severance")
        r.covered()
        r.nextStarted()
        r.leaveAndComeBack()
        r.advance(HomeActivity.RETRY_WINDOW_MS + 1_000)

        r.covered()

        r.assertActsAsHome()
    }

    // top-row-and-search#AC-14: only soon after the search.
    @Test
    fun `AC-14 a cover after the retry window acts as a Home press`() {
        val r = HomeScreenRobot()
        r.searchAndOpen("Severance")
        r.advance(HomeActivity.RETRY_WINDOW_MS + 1_000)

        r.covered()

        r.assertActsAsHome()
    }

    // top-row-and-search#AC-14 and home-takeover#AC-3: with no search, a cover is a Home press as before.
    @Test
    fun `AC-14 a cover with no search acts as a Home press`() {
        val r = HomeScreenRobot()
        r.type("Sever")

        r.covered()

        r.assertActsAsHome()
    }

    // top-row-and-search#AC-14: a Home press cancels the retry.
    @Test
    fun `AC-14 a Home press after the search cancels the retry`() {
        val r = HomeScreenRobot()
        r.searchAndOpen("Severance")
        r.homePress()

        r.covered()

        r.assertActsAsHome()
    }

    // top-row-and-search#AC-14: a key on the quiet home screen cancels the retry.
    @Test
    fun `AC-14 a key on the home screen after the search cancels the retry`() {
        val r = HomeScreenRobot()
        r.searchAndOpen("Severance")
        r.key(KeyEvent.KEYCODE_DPAD_DOWN)

        r.covered()

        r.assertActsAsHome()
    }

    // top-row-and-search#AC-14 and AC-5: a retry that does not open fails as a search does.
    @Test
    fun `AC-14 a retry that does not open says so after 10 seconds`() {
        val r = HomeScreenRobot()
        r.searchAndOpen("Severance")
        r.covered()
        r.nextStarted()

        r.advance(10_100)

        assertEquals(View.GONE, r.overlay.visibility)
        assertEquals("Severance", r.searchBox.text.toString())
        assertSame(r.searchBox, r.focused)
        assertEquals("Google's search did not open. Press select to try again.", r.statusLine.text.toString())
    }

    // top-row-and-search#AC-14: a voice search is not retried (there is no text to send again).
    @Test
    fun `AC-14 a cover after voice search acts as a Home press`() {
        val r = HomeScreenRobot()
        r.voice.requestFocus()
        r.voice.performClick()
        r.nextStarted()
        r.leaveAndComeBack()

        r.covered()

        r.assertActsAsHome()
    }
}
