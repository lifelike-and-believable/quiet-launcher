package quiet.launcher

import android.graphics.drawable.LayerDrawable
import android.provider.Settings
import android.widget.ImageView
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * The selected tile's look (docs/design/home-screen.md, Layout, Selection; the project lead's change
 * request of 2026-11-09): artwork with rounded corners, and an outline that pulses for about 5 seconds.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-land-xhdpi")
class TileLookTest {

    @After
    fun animationsBackOn() {
        Settings.Global.putFloat(RuntimeEnvironment.getApplication().contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    }

    private fun HomeScreenRobot.art(position: Int): ImageView = tile(position).findViewById(R.id.app_art)

    /** The white part of the selected tile's outline (the black edge stays as it is). */
    private fun HomeScreenRobot.outlineAlpha(position: Int): Int {
        val ring = art(position).foreground as? LayerDrawable
        assertNotNull("the selected tile has an outline", ring)
        return ring!!.getDrawable(ring.numberOfLayers - 1).alpha
    }

    private fun HomeScreenRobot.sampleAlpha(position: Int, fromMs: Long, toMs: Long, stepMs: Long = 100): Set<Int> {
        val seen = mutableSetOf<Int>()
        var t = fromMs
        while (t < toMs) {
            advance(stepMs)
            t += stepMs
            seen += outlineAlpha(position)
        }
        return seen
    }

    @Test
    fun `artwork is clipped to the outline's rounded corners`() {
        val r = HomeScreenRobot()

        val art = r.art(0)

        assertTrue(art.clipToOutline)
        assertNotNull(art.outlineProvider)
    }

    @Test
    fun `the selected app's outline pulses, then settles after about 5 seconds`() {
        val r = HomeScreenRobot()
        r.tile(1).requestFocus()

        val pulsing = r.sampleAlpha(1, 0, 2_000)
        assertTrue("the outline brightens and dims: saw $pulsing", pulsing.size > 2)

        r.advance(4_000)
        val settled = r.sampleAlpha(1, 0, 1_000)
        assertEquals("steady and fully bright after about 5 seconds", setOf(255), settled)
    }

    @Test
    fun `with animations off the outline does not pulse`() {
        val r = HomeScreenRobot()
        Settings.Global.putFloat(r.context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        r.tile(1).requestFocus()

        assertEquals(setOf(255), r.sampleAlpha(1, 0, 2_000))
    }
}
