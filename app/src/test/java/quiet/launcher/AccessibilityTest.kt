package quiet.launcher

import android.view.View
import android.widget.TextView
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-land-xhdpi")
class AccessibilityTest {

    private fun spoken(v: View): String = (v.contentDescription ?: (v as? TextView)?.let { it.text.ifEmpty { it.hint } }).toString()

    // top-row-and-search#AC-12
    @Test
    fun `AC-12 the controls read their names, and their icons are not read`() {
        val r = HomeScreenRobot()

        assertEquals("Search for a show", spoken(r.searchBox))
        assertEquals("Voice search", spoken(r.voice))
        assertEquals("Settings", spoken(r.settings))
        for (icon in listOf(R.id.search_icon, R.id.voice_icon, R.id.settings_icon)) {
            r.activity.findViewById<View>(icon)?.let {
                assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_NO, it.importantForAccessibility)
            }
        }
    }

    // top-row-and-search#AC-12
    @Test
    fun `AC-12 the overlay and the status line are announced, the clock is not`() {
        val r = HomeScreenRobot()

        assertEquals(View.ACCESSIBILITY_LIVE_REGION_POLITE, r.view<View>(R.id.search_overlay_title).accessibilityLiveRegion)
        assertEquals(View.ACCESSIBILITY_LIVE_REGION_POLITE, r.statusLine.accessibilityLiveRegion)
        assertEquals(View.ACCESSIBILITY_LIVE_REGION_NONE, r.view<View>(R.id.clock).accessibilityLiveRegion)

        r.type("Severance")
        r.pressSearch()
        r.advance(10_100)

        assertEquals(View.ACCESSIBILITY_LIVE_REGION_ASSERTIVE, r.statusLine.accessibilityLiveRegion)
    }
}
