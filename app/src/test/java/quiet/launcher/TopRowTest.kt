package quiet.launcher

import android.provider.Settings
import android.view.View
import android.widget.TextClock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-land-xhdpi")
class TopRowTest {

    private fun leftOf(v: View): Int = IntArray(2).also { v.getLocationOnScreen(it) }[0]

    private fun topOf(v: View): Int = IntArray(2).also { v.getLocationOnScreen(it) }[1]

    // top-row-and-search#AC-1
    @Test
    fun `AC-1 the top row holds the search box, Voice search, Settings, and the clock, left to right`() {
        val r = HomeScreenRobot()
        val clock = r.view<View>(R.id.clock)

        assertEquals("Search for a show", r.searchBox.hint.toString())
        assertEquals("Voice search", r.voice.text.toString())
        assertEquals("Settings", r.settings.text.toString())
        assertTrue(leftOf(r.searchBox) < leftOf(r.voice))
        assertTrue(leftOf(r.voice) < leftOf(r.settings))
        assertTrue(leftOf(r.settings) < leftOf(clock))
        assertTrue("the top row is above the apps", topOf(r.settings) < topOf(r.grid))
    }

    // top-row-and-search#AC-1
    @Test
    fun `AC-1 the status line keeps its space when it is empty`() {
        val r = HomeScreenRobot()

        assertEquals("", r.statusLine.text.toString())
        assertEquals(View.VISIBLE, r.statusLine.visibility)
        assertTrue(r.statusLine.height > 0)
        assertTrue(topOf(r.statusLine) < topOf(r.grid))
    }

    // top-row-and-search#AC-7
    @Test
    fun `AC-7 Settings opens the TV's settings`() {
        val r = HomeScreenRobot()

        r.settings.performClick()

        assertEquals(Settings.ACTION_SETTINGS, r.nextStarted()?.action)
    }

    // top-row-and-search#AC-8
    @Test
    fun `AC-8 the clock follows the system clock, without AM or PM, and cannot be selected`() {
        val r = HomeScreenRobot()

        val clock = r.view<View>(R.id.clock)

        assertTrue(clock is TextClock)
        clock as TextClock
        assertEquals("h:mm", clock.format12Hour.toString())
        assertEquals("H:mm", clock.format24Hour.toString())
        assertFalse(clock.isFocusable)
    }
}
