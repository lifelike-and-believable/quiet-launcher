package quiet.launcher

import android.content.Intent
import android.os.Looper
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
class HomeWatcherGoogleTvTest {

    private val app = RuntimeEnvironment.getApplication()
    private val service = Robolectric.setupService(HomeWatcher::class.java)

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun nextStarted(): Intent? = shadowOf(app).nextStartedActivity

    private fun drainStarted() {
        while (nextStarted() != null) Unit
    }

    private fun window(cls: String) = AccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED).apply {
        packageName = LAUNCHERX
        className = cls
    }

    private fun requestGoogleTv() {
        app.sendBroadcast(Intent(HomeWatcher.ACTION_OPEN_GOOGLE_TV).setPackage(app.packageName))
        idle()
    }

    private fun assertQuietHome(intent: Intent?) {
        assertEquals(HomeActivity::class.java.name, intent?.component?.className)
        assertTrue(intent!!.getBooleanExtra(HomeActivity.EXTRA_HOME, false))
    }

    // google-tv-button#AC-3
    @Test
    fun `AC-3 on request the service stops covering, then opens Google TV's home screen`() {
        requestGoogleTv()

        assertTrue(service.isAllowingGoogleTv)
        val opened = nextStarted()
        assertEquals(Intent.ACTION_MAIN, opened?.action)
        assertTrue(opened!!.hasCategory(Intent.CATEGORY_HOME))
        assertEquals(LAUNCHERX, opened.`package` ?: opened.component?.packageName)
        assertTrue(opened.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }

    // google-tv-button#AC-4
    @Test
    fun `AC-4 while allowed, Google TV's home screen and its pages are not covered`() {
        requestGoogleTv()
        drainStarted()

        service.onAccessibilityEvent(window("$LAUNCHERX.home.HomeActivity"))
        service.onAccessibilityEvent(window("$LAUNCHERX.entity.EntityActivity"))
        service.onAccessibilityEvent(window("$LAUNCHERX.home.HomeActivity"))

        assertNull(nextStarted())
        assertFalse(service.isCovering)
    }

    // google-tv-button#AC-5
    @Test
    fun `AC-5 the next Home press brings the quiet home screen back, and covering resumes`() {
        requestGoogleTv()
        drainStarted()

        assertTrue(service.handleKey(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_HOME)))
        assertQuietHome(nextStarted())
        assertFalse(service.isAllowingGoogleTv)

        service.onAccessibilityEvent(window("$LAUNCHERX.home.HomeActivity"))
        assertTrue(service.isCovering)
        assertQuietHome(nextStarted())
    }

    // google-tv-button#AC-6
    @Test
    fun `AC-6 when the screen turns off the service covers again and brings the quiet home screen forward`() {
        requestGoogleTv()
        drainStarted()

        app.sendBroadcast(Intent(Intent.ACTION_SCREEN_OFF))
        idle()

        assertFalse(service.isAllowingGoogleTv)
        assertQuietHome(nextStarted())
    }

    // google-tv-button#AC-6: a screen turning off at any other time changes nothing.
    @Test
    fun `AC-6 the screen turning off without Google TV open does nothing`() {
        app.sendBroadcast(Intent(Intent.ACTION_SCREEN_OFF))
        idle()

        assertNull(nextStarted())
    }

    // google-tv-button#AC-7
    @Test
    fun `AC-7 a service that starts afresh covers Google TV's home screen`() {
        requestGoogleTv()
        val fresh = Robolectric.setupService(HomeWatcher::class.java)

        assertFalse(fresh.isAllowingGoogleTv)
    }

    private companion object {
        const val LAUNCHERX = "com.google.android.apps.tv.launcherx"
    }
}
