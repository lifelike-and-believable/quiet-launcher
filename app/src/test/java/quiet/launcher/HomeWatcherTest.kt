package quiet.launcher

import android.content.Intent
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HomeWatcherTest {

    private val service = Robolectric.setupService(HomeWatcher::class.java)

    private fun nextStarted(): Intent? = shadowOf(RuntimeEnvironment.getApplication()).nextStartedActivity

    private fun key(action: Int, code: Int, repeat: Int = 0) = KeyEvent(0L, 0L, action, code, repeat)

    private fun window(pkg: String, cls: String, type: Int = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) =
        AccessibilityEvent(type).apply {
            packageName = pkg
            className = cls
        }

    private fun assertQuietHomeStarted(started: Intent?) {
        assertEquals(HomeActivity::class.java.name, started?.component?.className)
        assertTrue(started!!.getBooleanExtra(HomeActivity.EXTRA_HOME, false))
        assertTrue(started.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }

    // home-takeover#AC-1
    @Test
    fun `AC-1 consumes the Home press, its repeats, and its release, and shows the quiet home screen once`() {
        assertTrue(service.handleKey(key(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_HOME)))
        assertQuietHomeStarted(nextStarted())

        assertTrue(service.handleKey(key(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_HOME, repeat = 1)))
        assertTrue(service.handleKey(key(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_HOME, repeat = 2)))
        assertTrue(service.handleKey(key(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_HOME)))
        assertNull(nextStarted())
    }

    // home-takeover#AC-2
    @Test
    fun `AC-2 lets every other key through`() {
        val others = listOf(
            KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_BUTTON_3, KeyEvent.KEYCODE_BUTTON_4,
            KeyEvent.KEYCODE_SEARCH, KeyEvent.KEYCODE_ASSIST, KeyEvent.KEYCODE_VOICE_ASSIST,
        )
        for (code in others) {
            assertFalse("key $code down", service.handleKey(key(KeyEvent.ACTION_DOWN, code)))
            assertFalse("key $code up", service.handleKey(key(KeyEvent.ACTION_UP, code)))
        }
        assertNull(nextStarted())
    }

    // home-takeover#AC-3
    @Test
    fun `AC-3 covers Google TV's home screen when it comes up`() {
        service.onAccessibilityEvent(window(LAUNCHERX, "$LAUNCHERX.home.HomeActivity"))
        assertQuietHomeStarted(nextStarted())

        service.onAccessibilityEvent(window(LAUNCHERX, "$LAUNCHERX.home.VanillaModeHomeActivity"))
        assertQuietHomeStarted(nextStarted())
    }

    // home-takeover#AC-3
    @Test
    fun `AC-3 leaves Google TV's other pages and other apps alone`() {
        service.onAccessibilityEvent(window(LAUNCHERX, "$LAUNCHERX.entity.EntityActivity"))
        service.onAccessibilityEvent(window(LAUNCHERX, "$LAUNCHERX.coreservices.bootmode.DispatchActivity"))
        service.onAccessibilityEvent(window(LAUNCHERX, "android.widget.FrameLayout"))
        service.onAccessibilityEvent(window("com.netflix.ninja", "com.netflix.ninja.MainActivity"))
        service.onAccessibilityEvent(window("com.example", "$LAUNCHERX.home.HomeActivity"))
        service.onAccessibilityEvent(
            window(LAUNCHERX, "$LAUNCHERX.home.HomeActivity", type = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED),
        )

        assertNull(nextStarted())
    }

    private companion object {
        const val LAUNCHERX = "com.google.android.apps.tv.launcherx"
    }
}
