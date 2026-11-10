package quiet.launcher

import android.view.View
import android.widget.TextView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-land-xhdpi")
class GoogleTvButtonTest {

    private fun leftOf(v: View): Int = IntArray(2).also { v.getLocationOnScreen(it) }[0]

    // google-tv-button#AC-1
    @Test
    fun `AC-1 the top row has Google TV between Voice search and Settings`() {
        val r = HomeScreenRobot()
        val button = r.view<TextView>(R.id.google_tv_button)

        assertEquals("Google TV", button.text.toString())
        assertTrue(button.contentDescription == null || button.contentDescription == "Google TV")
        assertTrue(leftOf(r.voice) < leftOf(button))
        assertTrue(leftOf(button) < leftOf(r.settings))
        assertTrue(button.isFocusable)
    }

    // google-tv-button#AC-2
    @Test
    fun `AC-2 Google TV asks the service, inside the app only, to open Google TV's home screen`() {
        val r = HomeScreenRobot()

        r.view<View>(R.id.google_tv_button).performClick()
        r.idle()

        val sent = shadowOf(RuntimeEnvironment.getApplication()).broadcastIntents
            .filter { it.action == HomeWatcher.ACTION_OPEN_GOOGLE_TV }
        assertEquals(1, sent.size)
        assertEquals(r.context.packageName, sent.single().`package`)
    }
}
